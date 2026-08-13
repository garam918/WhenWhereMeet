package com.garam.whenwheremeet.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.garam.whenwheremeet.domain.model.CalendarEventDraft
import java.time.ZoneOffset

@Composable
actual fun rememberCalendarService(): CalendarService {
    val context = LocalContext.current
    return remember(context) {
        object : CalendarService {
            override fun openEventEditor(event: CalendarEventDraft): CalendarLaunchResult {
                val start = java.time.LocalDate.parse(event.date.toString())
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
                val end = java.time.LocalDate.parse(event.date.toString())
                    .plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, event.title)
                    event.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                    event.notes?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                    putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                }
                return try {
                    context.startActivity(intent)
                    CalendarLaunchResult.OPENED
                } catch (_: ActivityNotFoundException) {
                    CalendarLaunchResult.UNAVAILABLE
                } catch (_: Throwable) {
                    CalendarLaunchResult.FAILED
                }
            }
        }
    }
}
