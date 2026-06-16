package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.MeetingAreaCandidate
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.ParticipantTravelTime
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.provider.TravelTimeProvider
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RecommendMeetingAreasUseCase(
    private val travelTimeProvider: TravelTimeProvider,
    private val calculator: AreaRecommendationCalculator = AreaRecommendationCalculator(),
) {
    suspend operator fun invoke(
        meetingType: MeetingType,
        participants: List<Participant>,
        locations: List<UserStartLocation>,
        preferences: List<ParticipantTravelPreference>,
        candidates: List<MeetingAreaCandidate>,
        limit: Int = 3,
    ): List<AreaRecommendation> {
        val locationByParticipant = locations.associateBy { it.participantId }
        val preferenceByParticipant = preferences.associateBy { it.participantId }
        val participantsWithLocation = participants.filter { it.id in locationByParticipant }

        return candidates.map { candidate ->
            val travelTimes = participantsWithLocation.map { participant ->
                val location = checkNotNull(locationByParticipant[participant.id])
                val mode = preferenceByParticipant[participant.id]?.transportMode ?: TransportMode.UNKNOWN
                val result = travelTimeProvider.getTravelTime(location.point, candidate.point, mode)
                ParticipantTravelTime(
                    participantId = participant.id,
                    participantNickname = participant.nickname,
                    transportMode = mode,
                    travelMinutes = result.minutes,
                )
            }
            calculator.calculate(candidate, travelTimes, meetingType)
        }.sortedByDescending { it.fairnessScore }.take(limit)
    }
}

class AreaRecommendationCalculator {
    fun calculate(
        candidate: MeetingAreaCandidate,
        travelTimes: List<ParticipantTravelTime>,
        meetingType: MeetingType,
    ): AreaRecommendation {
        require(travelTimes.isNotEmpty()) { "이동시간이 하나 이상 필요합니다." }
        val minutes = travelTimes.map { it.travelMinutes }
        val average = minutes.average()
        val maximum = minutes.max()
        val minimum = minutes.min()
        val variance = minutes.sumOf { (it - average).pow(2) } / minutes.size
        val standardDeviation = sqrt(variance)
        val meetingTypeBonus = if (candidate.tags.any { it.equals(meetingType.label, ignoreCase = true) }) 8.0 else 0.0
        val excessiveTravelPenalty = excessiveTravelPenalty(average, maximum)
        val score = 100.0 -
            average * 0.8 -
            maximum * 0.5 -
            standardDeviation * 0.7 -
            excessiveTravelPenalty +
            meetingTypeBonus

        return AreaRecommendation(
            candidate = candidate,
            participantTravelTimes = travelTimes.sortedBy { it.travelMinutes },
            averageTravelMinutes = average.roundToInt(),
            maxTravelMinutes = maximum,
            minTravelMinutes = minimum,
            travelTimeVariance = variance,
            fairnessScore = score,
            recommendationReason = recommendationReason(travelTimes, maximum, meetingTypeBonus > 0),
        )
    }

    private fun excessiveTravelPenalty(average: Double, maximum: Int): Double {
        val threshold = maxOf(45.0, average * 1.6)
        return if (maximum > threshold) (maximum - threshold) * 1.5 else 0.0
    }

    private fun recommendationReason(
        travelTimes: List<ParticipantTravelTime>,
        maximum: Int,
        hasMeetingTypeBonus: Boolean,
    ): String {
        val within45 = travelTimes.count { it.travelMinutes <= 45 }
        val base = when {
            within45 == travelTimes.size -> "${travelTimes.size}명 모두 45분 이내 도착할 수 있어요."
            within45 > 0 -> "${travelTimes.size}명 중 ${within45}명이 45분 이내 도착할 수 있어요."
            else -> "참여자들의 이동시간 차이가 비교적 작은 지역이에요."
        }
        val maxReason = if (maximum <= 50) " 가장 오래 걸리는 사람도 50분 이내예요." else " 최장 이동시간은 약 ${maximum}분이에요."
        val typeReason = if (hasMeetingTypeBonus) " 약속 유형과도 잘 맞아요." else ""
        return base + maxReason + typeReason
    }
}
