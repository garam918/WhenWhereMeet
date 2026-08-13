package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.domain.model.CalendarEventDraft
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKitUI.EKEventEditViewController
import platform.EventKitUI.EKEventEditViewDelegateProtocol
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.UIKit.UIApplication
import platform.darwin.NSObject

@Composable
actual fun rememberCalendarService(): CalendarService = remember {
    IosCalendarService()
}

private class IosCalendarService : CalendarService {
    private var eventStore: EKEventStore? = null
    private var editDelegate: CalendarEventEditDelegate? = null

    override fun openEventEditor(event: CalendarEventDraft): CalendarLaunchResult {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return CalendarLaunchResult.UNAVAILABLE
        return runCatching {
            val calendar = NSCalendar.currentCalendar
            val startComponents = NSDateComponents().apply {
                year = event.date.year.toLong()
                month = (event.date.month.ordinal + 1).toLong()
                day = event.date.day.toLong()
            }
            val startDate = calendar.dateFromComponents(startComponents)
                ?: return CalendarLaunchResult.FAILED
            val endDate = calendar.dateByAddingComponents(
                comps = NSDateComponents().apply { day = 1 },
                toDate = startDate,
                options = 0u,
            ) ?: return CalendarLaunchResult.FAILED

            val store = EKEventStore()
            val calendarEvent = EKEvent.eventWithEventStore(store).apply {
                title = event.title
                this.startDate = startDate
                this.endDate = endDate
                allDay = true
                location = event.location
                notes = event.notes
            }
            val controller = EKEventEditViewController().apply {
                eventStore = store
                this.event = calendarEvent
            }
            val delegate = CalendarEventEditDelegate {
                eventStore = null
                editDelegate = null
            }
            controller.editViewDelegate = delegate
            eventStore = store
            editDelegate = delegate
            rootViewController.presentViewController(controller, animated = true, completion = null)
            CalendarLaunchResult.OPENED
        }.getOrDefault(CalendarLaunchResult.FAILED)
    }
}

private class CalendarEventEditDelegate(
    private val onDismissed: () -> Unit,
) : NSObject(), EKEventEditViewDelegateProtocol {
    override fun eventEditViewController(
        controller: EKEventEditViewController,
        didCompleteWithAction: Long,
    ) {
        controller.dismissViewControllerAnimated(true) {
            onDismissed()
        }
    }
}
