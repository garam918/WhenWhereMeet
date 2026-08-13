package com.garam.whenwheremeet.domain.model

import kotlinx.datetime.LocalDate

data class CalendarEventDraft(
    val title: String,
    val date: LocalDate,
    val location: String?,
    val notes: String?,
)
