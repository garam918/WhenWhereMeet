package com.garam.whenwheremeet.data.provider

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AccountDeletionService {
    private const val EndpointUrl =
        "https://asia-northeast3-whenwheremeet.cloudfunctions.net/deleteAccountData"

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 120_000
        }
    }

    suspend fun deleteServerAccount(idToken: String) {
        require(idToken.isNotBlank()) { "로그인 인증 정보를 확인할 수 없어요. 다시 로그인해주세요." }
        val response = httpClient.post(EndpointUrl) {
            bearerAuth(idToken)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (response.status.isSuccess()) return

        val body = response.bodyAsText()
        val serverMessage = runCatching {
            Json.parseToJsonElement(body)
                .jsonObject["error"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()
        throw IllegalStateException(serverMessage ?: "서버 데이터를 삭제하지 못했어요. 잠시 후 다시 시도해주세요.")
    }
}
