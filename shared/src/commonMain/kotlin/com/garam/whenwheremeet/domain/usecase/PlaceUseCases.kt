package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteSummary
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.presentation.state.toKoreanDate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

class MeetingTypePlaceCategoryMapper {
    operator fun invoke(meetingType: MeetingType): Set<PlaceCategory> = when (meetingType) {
        MeetingType.MEAL -> setOf(PlaceCategory.RESTAURANT)
        MeetingType.CAFE -> setOf(PlaceCategory.CAFE)
        MeetingType.DRINKS -> setOf(PlaceCategory.BAR, PlaceCategory.RESTAURANT)
        MeetingType.STUDY -> setOf(PlaceCategory.STUDY_ROOM, PlaceCategory.CAFE)
        MeetingType.EXERCISE -> setOf(PlaceCategory.ACTIVITY)
        MeetingType.OTHER -> PlaceCategory.entries.toSet()
    }
}

class ScorePlaceCandidatesUseCase(
    private val categoryMapper: MeetingTypePlaceCategoryMapper = MeetingTypePlaceCategoryMapper(),
) {
    operator fun invoke(
        places: List<PlaceCandidate>,
        center: GeoPoint,
        meetingType: MeetingType,
        selectedAreaRecommendation: AreaRecommendation?,
    ): List<ScoredPlaceCandidate> = places.map { place ->
        score(place, center, meetingType, selectedAreaRecommendation)
    }.sortedByDescending { it.score }

    fun score(
        place: PlaceCandidate,
        center: GeoPoint,
        meetingType: MeetingType,
        selectedAreaRecommendation: AreaRecommendation?,
    ): ScoredPlaceCandidate {
        val distanceMeters = haversineKilometers(center, place.point) * 1000
        val matchingCategories = categoryMapper(meetingType)
        val reasons = mutableListOf<String>()
        var score = 100.0

        score -= (distanceMeters / 100).coerceAtMost(20.0)
        if (distanceMeters <= 500) reasons += "선택된 지역 중심에서 가까워요"

        if (place.category in matchingCategories) {
            score += 20
            reasons += "약속 유형과 잘 맞는 ${place.category.label}예요"
        }

        place.rating?.let { score += (it - 3.0).coerceAtLeast(0.0) * 8 }
        place.reviewCount?.let {
            score += ln((it + 1).toDouble()) * 2
            if (it >= 100) reasons += "리뷰 수가 많은 장소예요"
        }
        if (place.openingHoursSummary != null) score += 4
        if (place.mapUrl != null) {
            score += 3
            reasons += "지도 링크를 바로 열 수 있어요"
        }

        selectedAreaRecommendation?.let {
            if (it.maxTravelMinutes > 60) score -= (it.maxTravelMinutes - 60) * 0.3
        }

        return ScoredPlaceCandidate(place, score, reasons.ifEmpty { listOf("모임 후보로 검토하기 좋은 장소예요") })
    }
}

class AggregatePlaceVotesUseCase {
    operator fun invoke(placeId: String, votes: List<PlaceVote>): PlaceVoteSummary {
        val placeVotes = votes.filter { it.placeId == placeId }
        return PlaceVoteSummary(
            placeId = placeId,
            likeCount = placeVotes.count { it.voteType == PlaceVoteType.LIKE },
            neutralCount = placeVotes.count { it.voteType == PlaceVoteType.NEUTRAL },
            dislikeCount = placeVotes.count { it.voteType == PlaceVoteType.DISLIKE },
        )
    }
}

class SortPlacesByVotesUseCase(
    private val aggregateVotes: AggregatePlaceVotesUseCase = AggregatePlaceVotesUseCase(),
) {
    operator fun invoke(
        places: List<ScoredPlaceCandidate>,
        votes: List<PlaceVote>,
    ): List<ScoredPlaceCandidate> = places.sortedWith(
        compareByDescending<ScoredPlaceCandidate> { aggregateVotes(it.place.id, votes).likeCount }
            .thenBy { aggregateVotes(it.place.id, votes).dislikeCount }
            .thenByDescending { it.score }
            .thenByDescending { it.place.reviewCount ?: 0 },
    )
}

class BuildConfirmedMeetingShareTextUseCase {
    operator fun invoke(room: MeetingRoom, participantCount: Int): String {
        val date = requireNotNull(room.confirmedDate)
        return buildString {
            appendLine("[약속 확정]")
            appendLine()
            appendLine("약속명: ${room.title}")
            appendLine("날짜: ${date.toKoreanDate()}")
            val place = room.confirmedPlace
            if (place == null) {
                appendLine("장소: 미정")
            } else {
                appendLine("장소: ${place.name}")
                place.roadAddress?.let { appendLine("주소: $it") }
                place.mapUrl?.let { appendLine("지도: $it") }
            }
            appendLine()
            append("참여자: ${participantCount}명")
        }
    }
}

private fun haversineKilometers(origin: GeoPoint, destination: GeoPoint): Double {
    val earthRadius = 6371.0
    val lat1 = origin.latitude * PI / 180
    val lat2 = destination.latitude * PI / 180
    val deltaLat = (destination.latitude - origin.latitude) * PI / 180
    val deltaLon = (destination.longitude - origin.longitude) * PI / 180
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}
