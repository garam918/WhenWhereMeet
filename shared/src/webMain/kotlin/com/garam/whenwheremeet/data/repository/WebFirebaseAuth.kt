package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.platform.AuthSession
import js.objects.unsafeJso
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.HTMLButtonElement
import web.http.BodyInit
import web.http.Headers
import web.http.RequestInit
import web.http.RequestMethod
import kotlin.js.ExperimentalWasmJsInterop

private const val WebAuthUidKey = "web_auth_uid"
private const val WebAuthIdTokenKey = "web_auth_id_token"
private const val WebAuthProviderKey = "web_auth_provider"
private const val WebAuthDisplayNameKey = "web_auth_display_name"
private const val WebAuthEmailKey = "web_auth_email"
private const val WebAuthIsAnonymousKey = "web_auth_is_anonymous"
private const val WebAuthChangeIdKey = "web_auth_change_id"
private const val WebAuthErrorIdKey = "web_auth_error_id"
private const val WebAuthErrorMessageKey = "web_auth_error_message"
private const val WebAuthReadyAttribute = "data-wwm-auth-ready"
private const val WebAuthGoogleActionId = "wwm-auth-google"
private const val WebAuthAppleActionId = "wwm-auth-apple"
private const val WebAuthRefreshActionId = "wwm-auth-refresh"
private const val WebAuthSignOutActionId = "wwm-auth-sign-out"
private const val WebAuthDeleteAccountActionId = "wwm-auth-delete-account"
private const val WebAuthPollIntervalMillis = 100L
private const val WebAuthTimeoutPollCount = 3_000

class WebFirebaseAuth(
    private val config: WebFirebaseConfig,
) {
    private val storage = platformKeyValueStorage()

    fun currentSession(): AuthSession? {
        val uid = storage.getString(WebAuthUidKey) ?: return null
        val providerId = storage.getString(WebAuthProviderKey) ?: "web-local"
        return AuthSession(
            uid = uid,
            displayName = storage.getString(WebAuthDisplayNameKey),
            email = storage.getString(WebAuthEmailKey),
            isAnonymous = storage.getString(WebAuthIsAnonymousKey)?.toBooleanStrictOrNull()
                ?: (providerId == "web-local" || providerId == "firebase-anonymous"),
            providerId = providerId,
        )
    }

    suspend fun currentIdToken(): String? {
        val session = currentSession()?.takeUnless { it.isAnonymous } ?: return null
        if (!isWebAuthBridgeReady()) return storage.getString(WebAuthIdTokenKey)

        awaitWebAuthAction(WebAuthRefreshActionId)
        return storage.getString(WebAuthIdTokenKey)
    }

    suspend fun signInWithGoogle(): AuthSession = signInWithProvider(WebAuthGoogleActionId)

    suspend fun signInWithApple(): AuthSession = signInWithProvider(WebAuthAppleActionId)

    suspend fun signOut() {
        if (isWebAuthBridgeReady()) {
            awaitWebAuthAction(WebAuthSignOutActionId)
        }
        clearStoredSession()
    }

    suspend fun deleteAccount() {
        if (isWebAuthBridgeReady()) {
            awaitWebAuthAction(WebAuthDeleteAccountActionId)
        }
        clearStoredSession()
    }

    private suspend fun signInWithProvider(actionId: String): AuthSession {
        check(config.isConfigured) {
            "웹 Firebase 설정을 찾지 못했어요. Firebase Hosting에서 다시 시도해주세요."
        }
        check(isWebAuthBridgeReady()) {
            "웹 로그인 모듈을 불러오지 못했어요. 네트워크 연결을 확인해주세요."
        }
        awaitWebAuthAction(actionId)
        val session = requireNotNull(currentSession()) { "웹 로그인 사용자 정보를 가져오지 못했어요." }
        check(!session.isAnonymous) { "Google 또는 Apple 계정으로 로그인해주세요." }
        return session
    }

    private suspend fun awaitWebAuthAction(actionId: String) {
        val initialChangeId = storage.getString(WebAuthChangeIdKey)
        val initialErrorId = storage.getString(WebAuthErrorIdKey)
        val button = document.getElementById(actionId) as? HTMLButtonElement
            ?: error("웹 로그인 화면을 준비하지 못했어요. 페이지를 새로고침해주세요.")
        button.click()

        repeat(WebAuthTimeoutPollCount) {
            delay(WebAuthPollIntervalMillis)
            val errorId = storage.getString(WebAuthErrorIdKey)
            if (errorId != initialErrorId) {
                error(storage.getString(WebAuthErrorMessageKey) ?: "웹 로그인에 실패했어요.")
            }
            if (storage.getString(WebAuthChangeIdKey) != initialChangeId) return
        }
        error("로그인 응답 시간이 초과됐어요. 다시 시도해주세요.")
    }

    private fun isWebAuthBridgeReady(): Boolean =
        document.documentElement?.getAttribute(WebAuthReadyAttribute) == "true"

    private fun clearStoredSession() {
        storage.remove(WebAuthUidKey)
        storage.remove(WebAuthIdTokenKey)
        storage.remove(WebAuthProviderKey)
        storage.remove(WebAuthDisplayNameKey)
        storage.remove(WebAuthEmailKey)
        storage.remove(WebAuthIsAnonymousKey)
    }

}

@OptIn(ExperimentalWasmJsInterop::class)
fun jsonRequest(method: RequestMethod, body: String? = null, bearerToken: String? = null): RequestInit {
    val headers = Headers()
    headers.set("Content-Type", "application/json")
    bearerToken?.let { headers.set("Authorization", "Bearer $it") }
    return unsafeJso {
        this.method = method
        this.headers = headers
        if (body != null) this.body = BodyInit(body)
    }
}

internal fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content.orEmpty()

internal fun JsonObject.optionalString(key: String): String? =
    this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

internal fun JsonObject.booleanValue(key: String): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull ?: false
