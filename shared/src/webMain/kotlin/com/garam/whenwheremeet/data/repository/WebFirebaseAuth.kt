package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.platform.AuthSession
import js.objects.unsafeJso
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import web.http.BodyInit
import web.http.POST
import web.http.Headers
import web.http.RequestInit
import web.http.RequestMethod
import web.http.fetch
import web.http.text
import kotlin.js.ExperimentalWasmJsInterop

private const val WebAuthUidKey = "web_auth_uid"
private const val WebAuthIdTokenKey = "web_auth_id_token"
private const val WebAuthProviderKey = "web_auth_provider"

class WebFirebaseAuth(
    private val config: WebFirebaseConfig,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val storage = platformKeyValueStorage()

    fun currentSession(): AuthSession? {
        val uid = storage.getString(WebAuthUidKey) ?: return null
        return AuthSession(
            uid = uid,
            displayName = null,
            email = null,
            isAnonymous = true,
            providerId = storage.getString(WebAuthProviderKey) ?: "web-local",
        )
    }

    fun currentIdToken(): String? = storage.getString(WebAuthIdTokenKey)

    suspend fun signInAnonymously(): AuthSession {
        if (!config.isConfigured) {
            return signInLocalOnly()
        }
        currentSession()?.takeIf { currentIdToken() != null }?.let { return it }

        val response = fetch(
            url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${config.apiKey}",
            init = jsonRequest(
                method = RequestMethod.POST,
                body = buildJsonObject { put("returnSecureToken", true) }.toString(),
            ),
        )
        val responseText = response.text()
        if (!response.ok) {
            throw IllegalStateException("웹 익명 로그인에 실패했어요. Firebase Web API key 설정을 확인해주세요. (${response.status})")
        }
        val body = json.parseToJsonElement(responseText) as JsonObject
        val uid = body.string("localId")
        val idToken = body.string("idToken")
        storage.putString(WebAuthUidKey, uid)
        storage.putString(WebAuthIdTokenKey, idToken)
        storage.putString(WebAuthProviderKey, "firebase-anonymous")
        return requireNotNull(currentSession())
    }

    fun signOut() {
        storage.remove(WebAuthUidKey)
        storage.remove(WebAuthIdTokenKey)
        storage.remove(WebAuthProviderKey)
    }

    private fun signInLocalOnly(): AuthSession {
        val uid = storage.getString(WebAuthUidKey) ?: "web-${kotlin.random.Random.nextLong().toString(radix = 16)}".also {
            storage.putString(WebAuthUidKey, it)
            storage.putString(WebAuthProviderKey, "web-local")
        }
        return AuthSession(
            uid = uid,
            displayName = null,
            email = null,
            isAnonymous = true,
            providerId = "web-local",
        )
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
