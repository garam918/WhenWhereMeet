package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.DateAvailabilitySummary
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.RecommendedDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class CycleAvailabilityStatusUseCase {
    operator fun invoke(current: AvailabilityStatus?): AvailabilityStatus? = when (current) {
        null -> AvailabilityStatus.AVAILABLE
        AvailabilityStatus.AVAILABLE -> AvailabilityStatus.MAYBE
        AvailabilityStatus.MAYBE -> AvailabilityStatus.UNAVAILABLE
        AvailabilityStatus.UNAVAILABLE -> null
    }
}

class AggregateAvailabilityUseCase {
    operator fun invoke(
        room: MeetingRoom,
        participants: List<Participant>,
        availabilities: List<Availability>,
    ): List<DateAvailabilitySummary> {
        val byParticipantAndDate = availabilities.associateBy { it.participantId to it.date }
        return datesBetween(room.dateRangeStart, room.dateRangeEnd).map { date ->
            val available = mutableListOf<Participant>()
            val maybe = mutableListOf<Participant>()
            val unavailable = mutableListOf<Participant>()
            val unanswered = mutableListOf<Participant>()

            participants.forEach { participant ->
                when (byParticipantAndDate[participant.id to date]?.status) {
                    AvailabilityStatus.AVAILABLE -> available += participant
                    AvailabilityStatus.MAYBE -> maybe += participant
                    AvailabilityStatus.UNAVAILABLE -> unavailable += participant
                    null -> unanswered += participant
                }
            }

            val required = participants.filter { it.isRequired }
            DateAvailabilitySummary(
                date = date,
                availableParticipants = available,
                maybeParticipants = maybe,
                unavailableParticipants = unavailable,
                unansweredParticipants = unanswered,
                score = available.size * 2 + maybe.size,
                allRequiredAvailable = required.all { it in available },
            )
        }
    }
}

class RecommendDatesUseCase(
    private val aggregate: AggregateAvailabilityUseCase = AggregateAvailabilityUseCase(),
) {
    operator fun invoke(
        room: MeetingRoom,
        participants: List<Participant>,
        availabilities: List<Availability>,
        limit: Int = 3,
    ): List<RecommendedDate> {
        val summaries = aggregate(room, participants, availabilities)
        return summaries.sortedWith(
            compareByDescending<DateAvailabilitySummary> { it.score }
                .thenByDescending { it.availableParticipants.size }
                .thenBy { it.maybeParticipants.size }
                .thenByDescending { it.allRequiredAvailable }
                .thenBy { it.unansweredParticipants.size }
                .thenBy { it.date },
        ).take(limit).mapIndexed { index, summary ->
            RecommendedDate(index + 1, summary, recommendationReason(summary))
        }
    }

    private fun recommendationReason(summary: DateAvailabilitySummary): String = when {
        summary.totalParticipants > 0 && summary.availableParticipants.size == summary.totalParticipants ->
            "${summary.totalParticipants}명 모두 가능해요"
        summary.allRequiredAvailable && summary.availableParticipants.isNotEmpty() ->
            "필수 참석자가 모두 가능한 날이에요"
        summary.availableParticipants.isNotEmpty() ->
            "가능 인원이 가장 많은 날 중 하나예요"
        else -> "아직 가능한 날짜 응답이 필요해요"
    }
}

fun datesBetween(start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
    if (endInclusive < start) return emptyList()
    val dates = mutableListOf<LocalDate>()
    var current = start
    while (current <= endInclusive) {
        dates += current
        current = current.plus(DatePeriod(days = 1))
    }
    return dates
}
