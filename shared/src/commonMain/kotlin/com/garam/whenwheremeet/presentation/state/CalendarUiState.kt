package com.garam.whenwheremeet.presentation.state

import com.garam.whenwheremeet.domain.usecase.CalendarDayIndicator
import com.garam.whenwheremeet.domain.usecase.CalendarFilter
import com.garam.whenwheremeet.domain.usecase.CalendarMonth
import com.garam.whenwheremeet.domain.usecase.HomeActionType
import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val isLoading: Boolean = false,
    val currentMonth: CalendarMonth,
    val selectedDate: LocalDate,
    val filter: CalendarFilter = CalendarFilter.ALL,
    val monthlySummary: CalendarMonthlySummaryUiModel = CalendarMonthlySummaryUiModel(),
    val dayItems: List<CalendarDayUiModel> = emptyList(),
    val selectedDateMeetings: List<CalendarMeetingItemUiModel> = emptyList(),
    val errorMessage: String? = null,
)

data class CalendarMonthlySummaryUiModel(
    val confirmedCount: Int = 0,
    val inProgressCount: Int = 0,
    val myActionRequiredCount: Int = 0,
)

data class CalendarDayUiModel(
    val date: LocalDate,
    val isToday: Boolean,
    val isSelected: Boolean,
    val indicators: List<CalendarDayIndicator>,
)

data class CalendarMeetingItemUiModel(
    val roomId: String,
    val title: String,
    val statusText: String,
    val dateText: String,
    val timeText: String?,
    val placeText: String?,
    val participantText: String,
    val responseText: String?,
    val actionType: HomeActionType,
    val ctaText: String,
    val isConfirmed: Boolean,
)
