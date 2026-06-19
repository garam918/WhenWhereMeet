package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.usecase.RecommendMeetingAreasUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class MetropolitanTransitProvidersTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun candidateProviderNarrowsCommercialAreasAroundStartLocationCenter() {
        val provider = MetropolitanMeetingAreaCandidateProvider()
        val candidates = provider.getCandidates(
            startLocations = listOf(
                location("p1", "수원역", 37.2657, 127.0001),
                location("p2", "잠실역", 37.5133, 127.1001),
                location("p3", "안양역", 37.4019, 126.9227),
            ),
            limit = 8,
        )

        assertEquals(8, candidates.size)
        assertTrue(candidates.any { it.id == "sadang" })
        assertTrue(candidates.any { it.id == "gangnam" })
    }

    @Test
    fun staticTransitTimeProviderUsesPrebuiltDurationMatrix() = runTest {
        val provider = StaticMetropolitanTransitTimeProvider()
        val origin = MetropolitanTransitData.stations.first { it.id == "suwon" }.point
        val destination = MetropolitanTransitData.areaCandidates.first { it.id == "sadang" }.point

        val result = provider.getTravelTime(origin, destination, TransportMode.PUBLIC_TRANSIT)

        assertEquals(42, result.minutes)
    }

    @Test
    fun recommendationUseCaseRanksCandidatesWithMatrixDurations() = runTest {
        val candidateProvider = MetropolitanMeetingAreaCandidateProvider()
        val recommend = RecommendMeetingAreasUseCase(StaticMetropolitanTransitTimeProvider())
        val locations = listOf(
            location("p1", "수원역", 37.2657, 127.0001),
            location("p2", "부평역", 37.4895, 126.7245),
            location("p3", "잠실역", 37.5133, 127.1001),
        )
        val participants = locations.mapIndexed { index, location ->
            participant(location.participantId, "참여자${index + 1}")
        }
        val candidates = candidateProvider.getCandidates(locations)

        val recommendations = recommend(
            meetingType = MeetingType.MEAL,
            participants = participants,
            locations = locations,
            preferences = participants.map { preference(it.id) },
            candidates = candidates,
        )

        assertEquals(3, recommendations.size)
        assertTrue(recommendations.all { it.participantTravelTimes.size == 3 })
        assertTrue(recommendations.first().fairnessScore >= recommendations.last().fairnessScore)
    }

    private fun location(participantId: String, label: String, latitude: Double, longitude: Double) = UserStartLocation(
        participantId = participantId,
        roomId = "room",
        label = label,
        latitude = latitude,
        longitude = longitude,
        privacyLevel = LocationPrivacyLevel.EXACT_PRIVATE,
        updatedAt = now,
    )

    private fun participant(id: String, nickname: String) = Participant(
        id = id,
        roomId = "room",
        nickname = nickname,
        isHost = id == "p1",
        isRequired = false,
        joinedAt = now,
    )

    private fun preference(participantId: String) = ParticipantTravelPreference(
        participantId = participantId,
        roomId = "room",
        transportMode = TransportMode.PUBLIC_TRANSIT,
        updatedAt = now,
    )
}
