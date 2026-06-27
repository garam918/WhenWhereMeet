package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.provider.PlaceRecommendationProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GeminiPlaceRecommendationProvider(
    private val endpointUrl: String = DefaultEndpointUrl,
    private val httpClient: HttpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 70_000
            socketTimeoutMillis = 70_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    },
) : PlaceRecommendationProvider {
    override suspend fun recommendPlaces(
        roomTitle: String,
        meetingType: MeetingType,
        startStations: List<String>,
        limit: Int,
    ): List<ScoredPlaceCandidate> {
        val response = httpClient.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(
                RecommendPlacesRequest(
                    roomTitle = roomTitle,
                    meetingType = meetingType.name,
                    startStations = startStations,
                    limit = limit,
                ),
            )
        }.body<RecommendPlacesResponse>()

        return response.candidates
            .sortedByDescending { it.score }
            .take(limit)
            .mapIndexed { index, candidate ->
                candidate.toDomain(meetingType, index)
            }
    }

    private fun RecommendPlaceCandidate.toDomain(meetingType: MeetingType, index: Int): ScoredPlaceCandidate {
        val category = meetingType.toPlaceCategory()
        val place = PlaceCandidate(
            id = stablePlaceId(name, index),
            name = name.trim(),
            category = category,
            address = area.trim().ifBlank { null },
            roadAddress = area.trim().ifBlank { null },
            latitude = 0.0,
            longitude = 0.0,
            rating = null,
            reviewCount = null,
            mapUrl = null,
            source = PlaceSource.CUSTOM,
        )
        return ScoredPlaceCandidate(
            place = place,
            score = score.coerceIn(0.0, 100.0),
            reasons = listOf(reason.trim()).filter { it.isNotBlank() },
        )
    }

    private fun MeetingType.toPlaceCategory(): PlaceCategory = when (this) {
        MeetingType.MEAL -> PlaceCategory.RESTAURANT
        MeetingType.CAFE -> PlaceCategory.CAFE
        MeetingType.DRINKS -> PlaceCategory.BAR
        MeetingType.STUDY -> PlaceCategory.STUDY_ROOM
        MeetingType.EXERCISE -> PlaceCategory.ACTIVITY
        MeetingType.OTHER -> PlaceCategory.ETC
    }

    private fun stablePlaceId(name: String, index: Int): String {
        val normalized = name.trim().lowercase().filter { it.isLetterOrDigit() }
        val suffix = if (normalized.isBlank()) "candidate-${index + 1}" else normalized
        return "gemini-$suffix-${scoreHash(name).toString(16)}"
    }

    private fun scoreHash(value: String): Int =
        value.fold(0) { acc, char -> (acc * 31 + char.code) and 0x7fffffff }

    private companion object {
        const val DefaultEndpointUrl = "https://asia-northeast3-whenwheremeet.cloudfunctions.net/recommendPlaceCandidates"
    }
}

@Serializable
private data class RecommendPlacesRequest(
    val roomTitle: String,
    val meetingType: String,
    val startStations: List<String>,
    val limit: Int = 5,
)

@Serializable
private data class RecommendPlacesResponse(
    val candidates: List<RecommendPlaceCandidate> = emptyList(),
)

@Serializable
private data class RecommendPlaceCandidate(
    val name: String,
    val area: String = "",
    val score: Double,
    val reason: String,
)
