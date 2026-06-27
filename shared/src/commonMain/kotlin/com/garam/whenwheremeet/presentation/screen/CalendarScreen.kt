package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.domain.usecase.CalendarDayIndicator
import com.garam.whenwheremeet.domain.usecase.CalendarFilter
import com.garam.whenwheremeet.domain.usecase.CalendarMonth
import com.garam.whenwheremeet.presentation.component.MainBottomBar
import com.garam.whenwheremeet.presentation.component.MainTab
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmEmptyState
import com.garam.whenwheremeet.presentation.component.WwmError
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMint
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmOrange
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSectionHeader
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.state.CalendarDayUiModel
import com.garam.whenwheremeet.presentation.state.CalendarMeetingItemUiModel
import com.garam.whenwheremeet.presentation.state.CalendarUiState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay as KizitonwoseCalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onFilterChange: (CalendarFilter) -> Unit,
    onSelectDate: (kotlinx.datetime.LocalDate) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenMyPage: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(WwmBackground)) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    CalendarMonthHeader(
                        monthText = state.currentMonth.displayText(),
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onToday = onToday,
                    )
                }
                item { CalendarSummaryCard(state) }
                item { CalendarFilterChips(selected = state.filter, onFilterChange = onFilterChange) }
                item {
                    CalendarMonthGrid(
                        currentMonth = state.currentMonth,
                        days = state.dayItems,
                        onSelectDate = onSelectDate,
                    )
                }
                item { WwmSectionHeader("▣", "${state.selectedDate.monthNumber}월 ${state.selectedDate.dayOfMonth}일 약속") }
                if (state.selectedDateMeetings.isEmpty()) {
                    item { EmptyCalendarDay(onCreateRoom = onCreateRoom) }
                } else {
                    items(state.selectedDateMeetings, key = { it.roomId }) { meeting ->
                        CalendarMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom, onOpenMap = onOpenMap)
                    }
                }
            }
        }
        MainBottomBar(
            selectedTab = MainTab.CALENDAR,
            onHomeClick = onOpenHome,
            onCalendarClick = {},
            onMyPageClick = onOpenMyPage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CalendarMonthHeader(
    monthText: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderButton("‹", onPreviousMonth)
            HeaderButton("›", onNextMonth)
        }
        Text(monthText, color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        HeaderButton("오늘", onToday)
    }
}

@Composable
private fun HeaderButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Color.White, RoundedCornerShape(999.dp)).border(1.dp, WwmBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = WwmIndigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalendarSummaryCard(state: CalendarUiState) {
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 달 약속", color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "확정 ${state.monthlySummary.confirmedCount}개 · 조율 중 ${state.monthlySummary.inProgressCount}개",
                color = WwmMuted,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun CalendarFilterChips(selected: CalendarFilter, onFilterChange: (CalendarFilter) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarFilter.entries.filterNot { it == CalendarFilter.MY_ACTION_REQUIRED }.forEach { filter ->
            val isSelected = filter == selected
            Box(
                Modifier.weight(1f)
                    .background(if (isSelected) WwmIndigo else Color.White, RoundedCornerShape(999.dp))
                    .border(1.dp, if (isSelected) WwmIndigo else WwmBorder, RoundedCornerShape(999.dp))
                    .clickable { onFilterChange(filter) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(filter.label(), color = if (isSelected) Color.White else WwmMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    currentMonth: CalendarMonth,
    days: List<CalendarDayUiModel>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val month = currentMonth.toYearMonth()
    val calendarState = rememberCalendarState(
        startMonth = month,
        endMonth = month,
        firstVisibleMonth = month,
        firstDayOfWeek = DayOfWeek.SUNDAY,
        outDateStyle = OutDateStyle.EndOfGrid,
    )
    val dayItemsByDate = remember(days) { days.associateBy { it.date } }
    val weekDays = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY) }

    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                weekDays.forEach {
                    Text(it.koreanLabel(), Modifier.weight(1f), color = WwmMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = false,
                dayContent = { calendarDay ->
                    CalendarDayCell(
                        calendarDay = calendarDay,
                        day = dayItemsByDate[calendarDay.date],
                        onSelectDate = onSelectDate,
                    )
                },
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    calendarDay: KizitonwoseCalendarDay,
    day: CalendarDayUiModel?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMonthDate = calendarDay.position == DayPosition.MonthDate
    val background = when {
        day?.isSelected == true -> WwmIndigo
        day?.isToday == true -> WwmSoftIndigo
        else -> Color.Transparent
    }
    val textColor = when {
        day?.isSelected == true -> Color.White
        isMonthDate -> WwmText
        else -> WwmMuted.copy(alpha = 0.35f)
    }
    val visibleIndicators = day?.indicators.orEmpty().filterNot { it == CalendarDayIndicator.MY_ACTION_REQUIRED }
    Column(
        modifier.aspectRatio(1f)
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, if (day?.isSelected == true) WwmIndigo else WwmBorder, RoundedCornerShape(10.dp))
            .then(
                if (isMonthDate && day != null) {
                    Modifier.clickable { onSelectDate(calendarDay.date) }
                } else {
                    Modifier
                },
            )
            .padding(5.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(calendarDay.date.dayOfMonth.toString(), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            visibleIndicators.take(3).forEach {
                Box(Modifier.size(5.dp).background(it.color(), CircleShape))
            }
            if (visibleIndicators.size > 3) {
                Text("+${visibleIndicators.size - 3}", color = textColor, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun CalendarMeetingCard(
    meeting: CalendarMeetingItemUiModel,
    onOpenRoom: (String) -> Unit,
    onOpenMap: (String) -> Unit,
) {
    val primaryAction = if (meeting.isConfirmed) onOpenMap else onOpenRoom
    WwmCard(Modifier.fillMaxWidth(), onClick = { onOpenRoom(meeting.roomId) }) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(meeting.statusText)
                Text(meeting.participantText, color = WwmMuted, fontSize = 12.sp)
            }
            Text(meeting.title, color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("${meeting.dateText} · ${meeting.timeText ?: "시간 미정"}", color = WwmMuted, fontSize = 14.sp)
            Text(meeting.placeText ?: "장소 미정", color = WwmText, fontSize = 14.sp)
            meeting.responseText?.let { Text(it, color = WwmMuted, fontSize = 13.sp) }
            Box(
                Modifier.fillMaxWidth().background(WwmSoftIndigo, RoundedCornerShape(10.dp)).clickable { primaryAction(meeting.roomId) }.padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(meeting.ctaText, color = WwmIndigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyCalendarDay(onCreateRoom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WwmEmptyState("이 날은 아직 약속이 없어요\n새 약속을 만들거나 다른 날짜를 선택해보세요.")
        WwmPrimaryButton(text = "새 약속 만들기", onClick = onCreateRoom)
    }
}

private fun CalendarFilter.label(): String = when (this) {
    CalendarFilter.ALL -> "전체"
    CalendarFilter.CONFIRMED -> "확정"
    CalendarFilter.IN_PROGRESS -> "조율 중"
    CalendarFilter.MY_ACTION_REQUIRED -> "내가 할 일"
}

private fun CalendarDayIndicator.color(): Color = when (this) {
    CalendarDayIndicator.CONFIRMED -> WwmMint
    CalendarDayIndicator.COLLECTING_AVAILABILITY -> WwmOrange
    CalendarDayIndicator.PLACE_SELECTING -> WwmIndigo
    CalendarDayIndicator.MY_ACTION_REQUIRED -> WwmError
    CalendarDayIndicator.CANCELLED -> WwmMuted
}

private fun CalendarMonth.toYearMonth(): YearMonth = YearMonth(year, month)

private fun DayOfWeek.koreanLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}
