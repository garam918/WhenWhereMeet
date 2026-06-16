package com.garam.whenwheremeet.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

sealed interface MeetingEvent {
    data class DateConfirmed(
        val roomId: String,
        val confirmedDate: LocalDate,
        val occurredAt: Instant,
    ) : MeetingEvent
}

fun interface MeetingEventPublisher {
    fun publish(event: MeetingEvent)
}

object NoOpMeetingEventPublisher : MeetingEventPublisher {
    override fun publish(event: MeetingEvent) = Unit
}
