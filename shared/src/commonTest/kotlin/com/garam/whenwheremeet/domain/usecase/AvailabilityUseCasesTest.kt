package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AvailabilityUseCasesTest {
    private val date1 = LocalDate(2026, 6, 20)
    private val date2 = LocalDate(2026, 6, 21)
    private val instant = Instant.parse("2026-06-01T00:00:00Z")
    private val host = participant("host", isHost = true)
    private val guest = participant("guest")
    private val room = MeetingRoom(
        id = "ABC123",
        title = "테스트 약속",
        meetingType = MeetingType.STUDY,
        dateRangeStart = date1,
        dateRangeEnd = date2,
        minParticipants = 2,
        hostParticipantId = host.id,
        status = MeetingStatus.COLLECTING_AVAILABILITY,
        createdAt = instant,
        updatedAt = instant,
    )

    @Test
    fun availabilityStatusCyclesThroughAllStates() {
        val cycle = CycleAvailabilityStatusUseCase()
        val available = cycle(null)
        val maybe = cycle(available)
        val unavailable = cycle(maybe)

        assertEquals(AvailabilityStatus.AVAILABLE, available)
        assertEquals(AvailabilityStatus.MAYBE, maybe)
        assertEquals(AvailabilityStatus.UNAVAILABLE, unavailable)
        assertNull(cycle(unavailable))
    }

    @Test
    fun aggregateCountsResponsesAndUnansweredParticipants() {
        val summary = AggregateAvailabilityUseCase()(
            room,
            listOf(host, guest),
            listOf(
                availability(host, date1, AvailabilityStatus.AVAILABLE),
                availability(guest, date1, AvailabilityStatus.MAYBE),
            ),
        ).first()

        assertEquals(1, summary.availableParticipants.size)
        assertEquals(1, summary.maybeParticipants.size)
        assertEquals(0, summary.unansweredParticipants.size)
        assertEquals(3, summary.score)
    }

    @Test
    fun recommendationUsesScoreThenAvailableCount() {
        val recommendations = RecommendDatesUseCase()(
            room,
            listOf(host, guest),
            listOf(
                availability(host, date1, AvailabilityStatus.AVAILABLE),
                availability(guest, date1, AvailabilityStatus.AVAILABLE),
                availability(host, date2, AvailabilityStatus.AVAILABLE),
                availability(guest, date2, AvailabilityStatus.MAYBE),
            ),
        )

        assertEquals(date1, recommendations.first().summary.date)
        assertEquals(4, recommendations.first().summary.score)
        assertEquals(1, recommendations.first().rank)
    }

    private fun participant(id: String, isHost: Boolean = false) = Participant(
        id = id,
        roomId = "ABC123",
        nickname = id,
        isHost = isHost,
        joinedAt = instant,
    )

    private fun availability(participant: Participant, date: LocalDate, status: AvailabilityStatus) = Availability(
        roomId = room.id,
        participantId = participant.id,
        date = date,
        status = status,
        updatedAt = instant,
    )
}
