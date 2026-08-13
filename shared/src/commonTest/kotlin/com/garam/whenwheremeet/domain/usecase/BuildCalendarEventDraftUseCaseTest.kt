package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildCalendarEventDraftUseCaseTest {
    private val useCase = BuildCalendarEventDraftUseCase()

    @Test
    fun buildsAllDayDraftFromConfirmedMeeting() {
        val room = meetingRoom(
            confirmedDate = LocalDate(2026, 9, 12),
            confirmedPlace = confirmedPlace(),
        )

        val result = useCase(room, "https://example.com/rooms/ABC123")

        requireNotNull(result)
        assertEquals("프로젝트 회고", result.title)
        assertEquals(LocalDate(2026, 9, 12), result.date)
        assertEquals("테스트 카페 · 서울시 중구 테스트로 1", result.location)
        assertTrue(result.notes.orEmpty().contains("분기 회고 모임"))
        assertTrue(result.notes.orEmpty().contains("https://example.com/rooms/ABC123"))
    }

    @Test
    fun returnsNullWhenDateIsNotConfirmed() {
        assertNull(useCase(meetingRoom(confirmedDate = null, confirmedPlace = null)))
    }

    @Test
    fun buildsDraftWithoutLocationForMeetingConfirmedWithoutPlace() {
        val room = meetingRoom(
            confirmedDate = LocalDate(2026, 9, 12),
            confirmedPlace = null,
        ).copy(status = MeetingStatus.MEETING_CONFIRMED)

        val result = requireNotNull(useCase(room))

        assertEquals(LocalDate(2026, 9, 12), result.date)
        assertNull(result.location)
    }

    private fun meetingRoom(
        confirmedDate: LocalDate?,
        confirmedPlace: PlaceCandidate?,
    ) = MeetingRoom(
        id = "ABC123",
        title = "프로젝트 회고",
        description = "분기 회고 모임",
        meetingType = MeetingType.CAFE,
        dateRangeStart = LocalDate(2026, 9, 1),
        dateRangeEnd = LocalDate(2026, 9, 30),
        minParticipants = 2,
        maxParticipants = 4,
        hostParticipantId = "host-1",
        status = if (confirmedPlace == null) MeetingStatus.COLLECTING_AVAILABILITY else MeetingStatus.PLACE_CONFIRMED,
        confirmedDate = confirmedDate,
        confirmedPlace = confirmedPlace,
        createdAt = Instant.parse("2026-08-12T00:00:00Z"),
        updatedAt = Instant.parse("2026-08-12T00:00:00Z"),
    )

    private fun confirmedPlace() = PlaceCandidate(
        id = "place-1",
        name = "테스트 카페",
        category = PlaceCategory.CAFE,
        roadAddress = "서울시 중구 테스트로 1",
        latitude = 37.5,
        longitude = 127.0,
        source = PlaceSource.FAKE,
    )
}
