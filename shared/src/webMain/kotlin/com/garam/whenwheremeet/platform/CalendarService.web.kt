package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.domain.model.CalendarEventDraft

@Composable
actual fun rememberCalendarService(): CalendarService = remember {
    object : CalendarService {
        override fun openEventEditor(event: CalendarEventDraft): CalendarLaunchResult =
            CalendarLaunchResult.UNAVAILABLE
    }
}
