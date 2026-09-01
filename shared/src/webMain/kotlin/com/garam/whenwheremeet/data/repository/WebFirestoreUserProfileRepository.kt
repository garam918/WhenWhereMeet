package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.UserProfile
import com.garam.whenwheremeet.domain.repository.UserProfileRepository
import com.garam.whenwheremeet.platform.toKoreaIsoDateTimeString
import com.garam.whenwheremeet.platform.toKoreaIsoDateTimeStringOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import web.http.RequestMethod
import web.http.fetch
import web.http.GET
import web.http.POST
import web.http.text

class WebFirestoreUserProfileRepository(
    private val config: WebFirebaseConfig,
    private val auth: WebFirebaseAuth,
) : UserProfileRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun upsert(profile: UserProfile) {
        val session = requireNotNull(auth.currentSession()) { "로그인 정보를 확인할 수 없습니다." }
        require(!session.isAnonymous && session.uid == profile.userId) { "본인의 회원 정보만 저장할 수 있습니다." }

        var lastFailure: Throwable? = null
        repeat(ProfileWriteAttemptCount) {
            val existing = getUserProfile(profile.userId)
            val fields = buildJsonObject {
                (profile.email ?: existing?.email)?.let { put("email", firestoreString(it)) }
                (profile.displayName ?: existing?.displayName)?.let { put("displayName", firestoreString(it)) }
                (profile.providerId ?: existing?.providerId)?.let { put("providerId", firestoreString(it)) }
                put(
                    "createdAt",
                    firestoreString(
                        existing?.createdAt?.toKoreaIsoDateTimeStringOrNull()
                            ?: profile.createdAt.toKoreaIsoDateTimeString(),
                    ),
                )
                put("lastLoginAt", firestoreString(profile.lastLoginAt.toKoreaIsoDateTimeString()))
                put("updatedAt", firestoreString(profile.updatedAt.toKoreaIsoDateTimeString()))
            }
            runCatching {
                commitUserProfile(profile.userId, fields, documentExists = existing != null)
            }.onSuccess {
                return
            }.onFailure {
                lastFailure = it
            }
        }
        throw requireNotNull(lastFailure)
    }

    private suspend fun getUserProfile(userId: String): StoredUserProfile? {
        val response = fetch(documentUrl(userId), jsonRequest(RequestMethod.GET, bearerToken = currentIdToken()))
        val responseText = response.text()
        if (response.status.toInt() == 404) return null
        if (!response.ok) throw IllegalStateException(profileErrorMessage(response.status.toInt(), responseText))
        val fields = json.parseToJsonElement(responseText).jsonObject["fields"]?.jsonObject ?: JsonObject(emptyMap())
        return StoredUserProfile(
            email = fields.optionalFirestoreString("email"),
            displayName = fields.optionalFirestoreString("displayName"),
            providerId = fields.optionalFirestoreString("providerId"),
            createdAt = fields.optionalFirestoreString("createdAt"),
        )
    }

    private suspend fun commitUserProfile(userId: String, fields: JsonObject, documentExists: Boolean) {
        val write = buildJsonObject {
            put("update", buildJsonObject {
                put("name", documentName(userId))
                put("fields", fields)
            })
            put("currentDocument", buildJsonObject { put("exists", documentExists) })
        }
        val body = buildJsonObject {
            put("writes", JsonArray(listOf(write)))
        }.toString()
        val response = fetch(
            url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents:commit",
            init = jsonRequest(RequestMethod.POST, body, currentIdToken()),
        )
        val responseText = response.text()
        if (!response.ok) throw IllegalStateException(profileErrorMessage(response.status.toInt(), responseText))
    }

    private suspend fun currentIdToken(): String = requireNotNull(auth.currentIdToken()) {
        "로그인이 필요해요. Google 또는 Apple 계정으로 다시 로그인해주세요."
    }

    private fun documentUrl(userId: String): String =
        "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents/users/${userId.encodeUserId()}"

    private fun documentName(userId: String): String =
        "projects/${config.projectId}/databases/${config.databaseId}/documents/users/$userId"

    private fun profileErrorMessage(status: Int, responseText: String): String {
        val parsed = runCatching { json.parseToJsonElement(responseText).jsonObject }.getOrNull()
        val message = parsed?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        return when {
            status == 401 -> "웹 로그인 정보가 만료됐어요. 다시 시작해주세요."
            status == 403 -> "회원 정보를 저장할 권한이 없어요. Firestore 보안 규칙을 확인해주세요."
            !message.isNullOrBlank() -> message
            else -> "회원 정보를 저장하지 못했어요. ($status)"
        }
    }
}

private data class StoredUserProfile(
    val email: String?,
    val displayName: String?,
    val providerId: String?,
    val createdAt: String?,
)

private fun JsonObject.optionalFirestoreString(key: String): String? =
    this[key]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)

private fun firestoreString(value: String): JsonObject = buildJsonObject { put("stringValue", value) }

private fun String.encodeUserId(): String =
    encodeToByteArray().joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xff
        when {
            value in 'A'.code..'Z'.code -> value.toChar().toString()
            value in 'a'.code..'z'.code -> value.toChar().toString()
            value in '0'.code..'9'.code -> value.toChar().toString()
            value == '-'.code || value == '_'.code || value == '.'.code || value == '~'.code -> value.toChar().toString()
            else -> "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
    }

private const val ProfileWriteAttemptCount = 2
