package com.garam.whenwheremeet.presentation.component

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AvailabilityCalendarTest {
    @Test
    fun dateRangeIsSplitIntoLabeledCalendarMonths() {
        val months = buildAvailabilityCalendarMonths(
            startDate = LocalDate(2026, 8, 30),
            endDate = LocalDate(2026, 10, 2),
        )

        assertEquals(listOf(8, 9, 10), months.map { it.monthNumber })
        assertEquals(LocalDate(2026, 8, 30), months.first().dates.first())
        assertEquals(LocalDate(2026, 10, 2), months.last().dates.last())
    }
}
