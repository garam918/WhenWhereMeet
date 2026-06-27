package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CalendarMonthUseCasesTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val today = LocalDate(2026, 6, 16)
    private val me = participant("me")

    @Test
    fun monthDatesGenerateEveryDateInMonth() {
        val dates = CalendarMonth(2026, 6).dates()

        assertEquals(LocalDate(2026, 6, 1), dates.first())
        assertEquals(LocalDate(2026, 6, 30), dates.last())
        assertEquals(30, dates.size)
    }

    @Test
    fun dayIndicatorsIncludeMeetingStatusAndMyActionRequired() {
        val result = BuildCalendarMonthUseCase()(
            meetings = listOf(overview(room("todo", MeetingStatus.COLLECTING_AVAILABILITY))),
            currentMonth = CalendarMonth(2026, 6),
            selectedDate = LocalDate(2026, 6, 17),
            today = today,
            filter = CalendarFilter.ALL,
        )

        val day = result.dayItems.first { it.date == LocalDate(2026, 6, 17) }
        assertTrue(CalendarDayIndicator.COLLECTING_AVAILABILITY in day.indicators)
        assertTrue(CalendarDayIndicator.MY_ACTION_REQUIRED in day.indicators)
    }

    @Test
    fun filterOnlyKeepsConfirmedMeetings() {
        val result = BuildCalendarMonthUseCase()(
            meetings = listOf(
                overview(room("confirmed", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 18))),
                overview(room("progress", MeetingStatus.COLLECTING_AVAILABILITY)),
            ),
            currentMonth = CalendarMonth(2026, 6),
            selectedDate = LocalDate(2026, 6, 18),
            today = today,
            filter = CalendarFilter.CONFIRMED,
        )

        assertEquals(listOf("confirmed"), result.selectedDateMeetings.map { it.roomId })
    }

    @Test
    fun selectedDateMeetingsIncludeCandidateDateForInProgressRoom() {
        val result = BuildCalendarMonthUseCase()(
            meetings = listOf(
                overview(
                    room("answered", MeetingStatus.COLLECTING_AVAILABILITY),
                    availabilities = listOf(availability("answered")),
                ),
            ),
            currentMonth = CalendarMonth(2026, 6),
            selectedDate = LocalDate(2026, 6, 19),
            today = today,
            filter = CalendarFilter.ALL,
        )

        assertEquals(listOf("answered"), result.selectedDateMeetings.map { it.roomId })
        assertEquals(HomeActionType.NONE, result.selectedDateMeetings.single().actionType)
    }

    @Test
    fun meetingsWithoutCurrentParticipantAreExcludedFromCalendar() {
        val result = BuildCalendarMonthUseCase()(
            meetings = listOf(
                overview(room("joined", MeetingStatus.COLLECTING_AVAILABILITY)),
                overview(room("left", MeetingStatus.COLLECTING_AVAILABILITY), currentParticipantId = null),
            ),
            currentMonth = CalendarMonth(2026, 6),
            selectedDate = LocalDate(2026, 6, 17),
            today = today,
            filter = CalendarFilter.ALL,
        )

        assertEquals(listOf("joined"), result.selectedDateMeetings.map { it.roomId })
        assertEquals(1, result.monthlySummary.inProgressCount)
    }

    private fun overview(
        room: MeetingRoom,
        availabilities: List<Availability> = emptyList(),
        currentParticipantId: String? = me.id,
    ) = MeetingOverview(
        room = room,
        participants = listOf(me),
        currentParticipantId = currentParticipantId,
        availabilities = availabilities,
        startLocations = emptyList(),
        areaRecommendations = emptyList(),
        placeCandidates = emptyList(),
        placeVotes = emptyList(),
    )

    private fun room(id: String, status: MeetingStatus, confirmedDate: LocalDate? = null) = MeetingRoom(
        id = id,
        title = "약속 $id",
        meetingType = MeetingType.CAFE,
        dateRangeStart = LocalDate(2026, 6, 17),
        dateRangeEnd = LocalDate(2026, 6, 19),
        minParticipants = 1,
        responseDeadline = LocalDate(2026, 6, 17),
        hostParticipantId = me.id,
        status = status,
        confirmedDate = confirmedDate,
        createdAt = now,
        updatedAt = now,
    )

    private fun participant(id: String) = Participant(
        id = id,
        roomId = "room",
        nickname = id,
        isHost = true,
        joinedAt = now,
    )

    private fun availability(roomId: String) = Availability(
        roomId = roomId,
        participantId = me.id,
        date = LocalDate(2026, 6, 17),
        status = AvailabilityStatus.AVAILABLE,
        updatedAt = now,
    )
}
