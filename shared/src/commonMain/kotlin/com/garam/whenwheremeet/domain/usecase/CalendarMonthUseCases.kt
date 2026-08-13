package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.isMeetingConfirmed
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class CalendarFilter {
    ALL,
    CONFIRMED,
    IN_PROGRESS,
    MY_ACTION_REQUIRED,
}

enum class CalendarDayIndicator {
    CONFIRMED,
    COLLECTING_AVAILABILITY,
    PLACE_SELECTING,
    MY_ACTION_REQUIRED,
    CANCELLED,
}

data class CalendarMonth(
    val year: Int,
    val month: Int,
) {
    init {
        require(month in 1..12) { "month must be 1..12" }
    }

    val firstDay: LocalDate = LocalDate(year, month, 1)

    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)

    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    fun displayText(): String = "${year}년 ${month}월"

    companion object {
        fun from(date: LocalDate): CalendarMonth = CalendarMonth(date.year, date.monthNumber)
    }
}

data class CalendarMonthResult(
    val currentMonth: CalendarMonth,
    val selectedDate: LocalDate,
    val filter: CalendarFilter,
    val monthlySummary: CalendarMonthlySummary,
    val dayItems: List<CalendarDay>,
    val selectedDateMeetings: List<CalendarMeetingItem>,
)

data class CalendarMonthlySummary(
    val confirmedCount: Int,
    val inProgressCount: Int,
    val myActionRequiredCount: Int,
)

data class CalendarDay(
    val date: LocalDate,
    val isToday: Boolean,
    val isSelected: Boolean,
    val indicators: List<CalendarDayIndicator>,
)

data class CalendarMeetingItem(
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

class BuildCalendarMonthUseCase(
    private val resolveActionType: ResolveHomeActionTypeUseCase = ResolveHomeActionTypeUseCase(),
) {
    operator fun invoke(
        meetings: List<MeetingOverview>,
        currentMonth: CalendarMonth,
        selectedDate: LocalDate,
        today: LocalDate,
        filter: CalendarFilter,
    ): CalendarMonthResult {
        val monthDates = currentMonth.dates()
        val meetingsWithActions = meetings
            .filter { it.currentParticipantId != null }
            .map { it to resolveActionType(it) }
        val monthMeetings = meetingsWithActions.filter { (meeting, _) ->
            meeting.relevantDates().any { it in monthDates }
        }
        val filtered = monthMeetings.filter { it.matches(filter) }
        val dayItems = monthDates.map { date ->
            CalendarDay(
                date = date,
                isToday = date == today,
                isSelected = date == selectedDate,
                indicators = filtered.indicatorsFor(date),
            )
        }
        val selectedDateMeetings = filtered
            .filter { (meeting, _) -> selectedDate in meeting.relevantDates() }
            .sortedWith(compareBy<Pair<MeetingOverview, HomeActionType>> { it.first.room.status.ordinal }
                .thenBy { it.first.room.title })
            .map { (meeting, actionType) -> meeting.toCalendarMeetingItem(actionType, selectedDate) }

        return CalendarMonthResult(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            filter = filter,
            monthlySummary = CalendarMonthlySummary(
                confirmedCount = monthMeetings.count { it.first.room.status.isMeetingConfirmed },
                inProgressCount = monthMeetings.count {
                    !it.first.room.status.isMeetingConfirmed &&
                        it.first.room.status != MeetingStatus.CANCELLED
                },
                myActionRequiredCount = monthMeetings.count { (_, actionType) ->
                    actionType != HomeActionType.NONE && actionType != HomeActionType.VIEW_CONFIRMED
                },
            ),
            dayItems = dayItems,
            selectedDateMeetings = selectedDateMeetings,
        )
    }

    private fun List<Pair<MeetingOverview, HomeActionType>>.indicatorsFor(date: LocalDate): List<CalendarDayIndicator> =
        filter { (meeting, _) -> date in meeting.relevantDates() }
            .flatMap { (meeting, actionType) ->
                buildList {
                    add(meeting.room.status.toIndicator())
                    if (actionType != HomeActionType.NONE && actionType != HomeActionType.VIEW_CONFIRMED) {
                        add(CalendarDayIndicator.MY_ACTION_REQUIRED)
                    }
                }
            }
            .distinct()
            .take(4)

    private fun Pair<MeetingOverview, HomeActionType>.matches(filter: CalendarFilter): Boolean {
        val actionType = second
        return when (filter) {
            CalendarFilter.ALL -> true
            CalendarFilter.CONFIRMED -> first.room.status.isMeetingConfirmed
            CalendarFilter.IN_PROGRESS -> !first.room.status.isMeetingConfirmed &&
                first.room.status != MeetingStatus.CANCELLED
            CalendarFilter.MY_ACTION_REQUIRED -> actionType != HomeActionType.NONE && actionType != HomeActionType.VIEW_CONFIRMED
        }
    }

    private fun MeetingOverview.toCalendarMeetingItem(actionType: HomeActionType, selectedDate: LocalDate): CalendarMeetingItem =
        CalendarMeetingItem(
            roomId = room.id,
            title = room.title,
            statusText = room.status.statusText(),
            dateText = if (room.confirmedDate != null) room.confirmedDate.shortDateText() else selectedDate.shortDateText(),
            timeText = null,
            placeText = room.confirmedPlace?.name ?: selectedAreaName() ?: "장소 미정",
            participantText = "참여자 ${participants.size}명",
            responseText = responseText(),
            actionType = actionType,
            ctaText = when {
                room.status == MeetingStatus.PLACE_CONFIRMED -> "지도 보기"
                actionType != HomeActionType.NONE -> actionType.ctaText()
                else -> "약속방 보기"
            },
            isConfirmed = room.status.isMeetingConfirmed,
        )

    private fun MeetingOverview.responseText(): String? {
        if (room.status != MeetingStatus.COLLECTING_AVAILABILITY) return null
        val responded = availabilities.map { it.participantId }.distinct().size
        return "응답 $responded/${participants.size}명"
    }

    private fun MeetingOverview.selectedAreaName(): String? =
        areaRecommendations.firstOrNull { it.candidate.id == room.selectedAreaCandidateId }?.candidate?.displayName
}

fun CalendarMonth.dates(): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = firstDay
    while (current.monthNumber == month) {
        dates += current
        current = current.plus(DatePeriod(days = 1))
    }
    return dates
}

fun MeetingOverview.relevantDates(): List<LocalDate> = when (room.status) {
    MeetingStatus.PLACE_CONFIRMED,
    MeetingStatus.MEETING_CONFIRMED,
    MeetingStatus.DATE_CONFIRMED,
    MeetingStatus.PLACE_SELECTING -> listOfNotNull(room.confirmedDate)
    MeetingStatus.COLLECTING_AVAILABILITY -> datesBetween(room.dateRangeStart, room.dateRangeEnd)
    MeetingStatus.DRAFT,
    MeetingStatus.CANCELLED -> emptyList()
}

fun MeetingStatus.toIndicator(): CalendarDayIndicator = when (this) {
    MeetingStatus.PLACE_CONFIRMED,
    MeetingStatus.MEETING_CONFIRMED -> CalendarDayIndicator.CONFIRMED
    MeetingStatus.COLLECTING_AVAILABILITY -> CalendarDayIndicator.COLLECTING_AVAILABILITY
    MeetingStatus.DATE_CONFIRMED,
    MeetingStatus.PLACE_SELECTING -> CalendarDayIndicator.PLACE_SELECTING
    MeetingStatus.DRAFT -> CalendarDayIndicator.COLLECTING_AVAILABILITY
    MeetingStatus.CANCELLED -> CalendarDayIndicator.CANCELLED
}
