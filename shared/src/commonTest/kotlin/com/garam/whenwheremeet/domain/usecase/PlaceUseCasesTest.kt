package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.AreaType
import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingAreaCandidate
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class PlaceUseCasesTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun mapsMeetingTypeToPlaceCategories() {
        val mapper = MeetingTypePlaceCategoryMapper()

        assertEquals(setOf(PlaceCategory.RESTAURANT), mapper(MeetingType.MEAL))
        assertTrue(PlaceCategory.STUDY_ROOM in mapper(MeetingType.STUDY))
        assertTrue(PlaceCategory.CAFE in mapper(MeetingType.STUDY))
    }

    @Test
    fun matchingNearbyHighlyReviewedPlaceGetsHigherScore() {
        val scorer = ScorePlaceCandidatesUseCase()
        val center = GeoPoint(37.5, 127.0)
        val matching = place("matching", PlaceCategory.CAFE, 37.5002, 127.0002, 4.8, 300)
        val unrelated = place("unrelated", PlaceCategory.ACTIVITY, 37.51, 127.01, 3.5, 10)

        assertTrue(scorer.score(matching, center, MeetingType.CAFE, null).score > scorer.score(unrelated, center, MeetingType.CAFE, null).score)
    }

    @Test
    fun aggregatesLikeAndDislikeVotes() {
        val votes = listOf(
            vote("place", "p1", PlaceVoteType.LIKE),
            vote("place", "p2", PlaceVoteType.LIKE),
            vote("place", "p3", PlaceVoteType.DISLIKE),
        )
        val summary = AggregatePlaceVotesUseCase()("place", votes)

        assertEquals(2, summary.likeCount)
        assertEquals(1, summary.dislikeCount)
    }

    @Test
    fun sortsByLikesThenDislikesThenScore() {
        val first = ScoredPlaceCandidate(place("first"), 80.0, emptyList())
        val second = ScoredPlaceCandidate(place("second"), 100.0, emptyList())
        val votes = listOf(
            vote("first", "p1", PlaceVoteType.LIKE),
            vote("first", "p2", PlaceVoteType.LIKE),
            vote("second", "p1", PlaceVoteType.LIKE),
        )

        assertEquals("first", SortPlacesByVotesUseCase()(listOf(second, first), votes).first().place.id)
    }

    @Test
    fun buildsConfirmedMeetingShareText() {
        val confirmedPlace = place("confirmed").copy(name = "확정 식당", roadAddress = "서울 구로구 중심로 1")
        val room = room().copy(status = MeetingStatus.PLACE_CONFIRMED, confirmedPlace = confirmedPlace)
        val text = BuildConfirmedMeetingShareTextUseCase()(room, 5)

        assertTrue(text.contains("확정 식당"))
        assertTrue(text.contains("서울 구로구 중심로 1"))
        assertTrue(text.contains("참여자: 5명"))
    }

    private fun place(
        id: String,
        category: PlaceCategory = PlaceCategory.RESTAURANT,
        latitude: Double = 37.5,
        longitude: Double = 127.0,
        rating: Double = 4.0,
        reviewCount: Int = 10,
    ) = PlaceCandidate(
        id = id,
        name = id,
        category = category,
        latitude = latitude,
        longitude = longitude,
        rating = rating,
        reviewCount = reviewCount,
        mapUrl = "https://maps.example/$id",
        source = PlaceSource.FAKE,
    )

    private fun vote(placeId: String, participantId: String, type: PlaceVoteType) =
        PlaceVote("room", placeId, participantId, type, now)

    private fun room() = MeetingRoom(
        id = "room",
        title = "테스트 약속",
        meetingType = MeetingType.MEAL,
        dateRangeStart = LocalDate(2026, 6, 20),
        dateRangeEnd = LocalDate(2026, 6, 21),
        minParticipants = 2,
        hostParticipantId = "host",
        status = MeetingStatus.PLACE_SELECTING,
        confirmedDate = LocalDate(2026, 6, 21),
        selectedAreaCandidateId = "area",
        createdAt = now,
        updatedAt = now,
    )
}
