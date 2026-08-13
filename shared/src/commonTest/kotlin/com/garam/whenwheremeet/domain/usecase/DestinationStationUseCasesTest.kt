package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
import com.garam.whenwheremeet.domain.model.TransitStation
import com.garam.whenwheremeet.domain.model.UserStartLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class DestinationStationUseCasesTest {
    private val now = Instant.parse("2026-08-13T00:00:00Z")

    @Test
    fun duplicateStationProposalsAreMergedAndSortedByVotes() {
        val gangnam = station("gangnam", "강남역")
        val hongdae = station("hongdae", "홍대입구역")
        val proposals = listOf(
            proposal("p1", gangnam),
            proposal("p2", gangnam),
            proposal("p3", hongdae),
        )
        val votes = listOf(
            vote("p1", "hongdae"),
            vote("p2", "hongdae"),
            vote("p3", "gangnam"),
        )

        val options = BuildDestinationStationOptionsUseCase()(proposals, votes)

        assertEquals(listOf("hongdae", "gangnam"), options.map { it.station.id })
        assertEquals(listOf("p1", "p2"), options.first().voterParticipantIds)
        assertEquals(listOf("p1", "p2"), options.last().proposerParticipantIds)
    }

    @Test
    fun votesForRemovedCandidateAreIgnored() {
        val options = BuildDestinationStationOptionsUseCase()(
            proposals = listOf(proposal("p1", station("gangnam", "강남역"))),
            votes = listOf(vote("p2", "removed")),
        )

        assertEquals(0, options.single().voteCount)
    }

    @Test
    fun kakaoRouteUrlContainsEncodedStationNamesAndCoordinates() {
        val origin = UserStartLocation(
            participantId = "p1",
            roomId = "room",
            label = "서울역",
            latitude = 37.5547,
            longitude = 126.9706,
            privacyLevel = LocationPrivacyLevel.AREA_ONLY_VISIBLE,
            updatedAt = now,
        )

        val url = KakaoMapRouteUrlBuilder().routeUrl(origin, station("gangnam", "강남역"))

        assertTrue(url.startsWith("https://map.kakao.com/link/by/traffic/"))
        assertTrue("%EC%84%9C%EC%9A%B8%EC%97%AD" in url)
        assertTrue("37.5547,126.9706" in url)
        assertTrue("%EA%B0%95%EB%82%A8%EC%97%AD" in url)
    }

    private fun station(id: String, name: String) = TransitStation(
        id = id,
        name = name,
        latitude = if (id == "gangnam") 37.4979 else 37.5572,
        longitude = if (id == "gangnam") 127.0276 else 126.9254,
        lines = listOf("2호선"),
        region = "서울",
    )

    private fun proposal(participantId: String, station: TransitStation) = DestinationStationProposal(
        roomId = "room",
        participantId = participantId,
        station = station,
        updatedAt = now,
    )

    private fun vote(participantId: String, stationId: String) = DestinationStationVote(
        roomId = "room",
        participantId = participantId,
        stationId = stationId,
        updatedAt = now,
    )
}
