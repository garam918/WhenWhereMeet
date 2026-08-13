package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import com.garam.whenwheremeet.domain.model.CalendarEventDraft

enum class CalendarLaunchResult {
    OPENED,
    UNAVAILABLE,
    FAILED,
}

interface CalendarService {
    fun openEventEditor(event: CalendarEventDraft): CalendarLaunchResult
}

@Composable
expect fun rememberCalendarService(): CalendarService
