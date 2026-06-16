package com.garam.whenwheremeet.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PlaceCandidate(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String? = null,
    val roadAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val openingHoursSummary: String? = null,
    val mapUrl: String? = null,
    val source: PlaceSource,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

@Serializable
enum class PlaceCategory(val label: String) {
    RESTAURANT("음식점"),
    CAFE("카페"),
    BAR("술집"),
    STUDY_ROOM("스터디룸"),
    ACTIVITY("체육·활동시설"),
    ETC("기타"),
}

@Serializable
enum class PlaceSource {
    FAKE,
    KAKAO,
    NAVER,
    GOOGLE,
    CUSTOM,
}

@Serializable
data class ScoredPlaceCandidate(
    val place: PlaceCandidate,
    val score: Double,
    val reasons: List<String>,
)

@Serializable
enum class PlaceVoteType(val label: String) {
    LIKE("좋아요"),
    NEUTRAL("괜찮아요"),
    DISLIKE("별로예요"),
}

@Serializable
data class PlaceVote(
    val roomId: String,
    val placeId: String,
    val participantId: String,
    val voteType: PlaceVoteType,
    val updatedAt: Instant,
)

data class PlaceVoteSummary(
    val placeId: String,
    val likeCount: Int,
    val neutralCount: Int,
    val dislikeCount: Int,
)
