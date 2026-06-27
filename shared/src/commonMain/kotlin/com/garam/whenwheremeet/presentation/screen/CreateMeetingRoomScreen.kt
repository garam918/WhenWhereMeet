package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.MAX_MEETING_PARTICIPANTS
import com.garam.whenwheremeet.domain.model.MIN_MEETING_PARTICIPANTS
import com.garam.whenwheremeet.platform.currentLocalDate
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.component.WwmTopBar
import com.garam.whenwheremeet.presentation.state.CreateRoomInput
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
fun CreateMeetingRoomScreen(
    onBack: () -> Unit,
    onCreate: (CreateRoomInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { currentLocalDate() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val doneKeyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val doneKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    var hostNickname by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var meetingType by remember { mutableStateOf(MeetingType.MEAL) }
    var visibleMonth by remember { mutableStateOf(today.firstDayOfMonth()) }
    var selectedStartDate by remember { mutableStateOf(today) }
    var selectedEndDate by remember { mutableStateOf(today.plus(DatePeriod(days = 6))) }
    var maxParticipantsText by remember { mutableStateOf("6") }
    val maxParticipants = maxParticipantsText.toIntOrNull()
    val canCreate = hostNickname.isNotBlank() &&
        title.isNotBlank() &&
        selectedStartDate >= today &&
        selectedEndDate >= selectedStartDate &&
        maxParticipants != null &&
        maxParticipants in MIN_MEETING_PARTICIPANTS..MAX_MEETING_PARTICIPANTS

    Column(modifier.fillMaxSize().background(WwmBackground)) {
        WwmTopBar(title = "새 약속 만들기", leadingText = "×", onLeadingClick = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FormCard("기본 정보", "어떤 모임을 계획하고 계신가요?") {
                OutlinedTextField(
                    value = hostNickname,
                    onValueChange = { hostNickname = it },
                    label = { Text("방장 닉네임") },
                    singleLine = true,
                    keyboardOptions = doneKeyboardOptions,
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("약속 이름") },
                    placeholder = { Text("예: 주말 한강 피크닉") },
                    singleLine = true,
                    keyboardOptions = doneKeyboardOptions,
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명 또는 메모") },
                    minLines = 2,
                    keyboardOptions = doneKeyboardOptions,
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("모임 성격", color = WwmMuted, fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MeetingType.entries.forEach { type ->
                        FilterChip(selected = meetingType == type, onClick = { meetingType = type }, label = { Text(type.label) })
                    }
                }
            }

            FormCard("일정 및 기한", "참석자들이 투표할 기간을 설정해주세요.") {
                Text("후보 날짜 범위", color = WwmMuted, fontSize = 13.sp)
                Text(
                    "${selectedStartDate.toDateLabel()} ~ ${selectedEndDate.toDateLabel()}",
                    color = WwmText,
                    fontWeight = FontWeight.SemiBold,
                )
                DateRangePickerCalendar(
                    visibleMonth = visibleMonth,
                    today = today,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onPreviousMonth = {
                        val previous = visibleMonth.plus(DatePeriod(months = -1)).firstDayOfMonth()
                        if (previous >= today.firstDayOfMonth()) visibleMonth = previous
                    },
                    onNextMonth = {
                        visibleMonth = visibleMonth.plus(DatePeriod(months = 1)).firstDayOfMonth()
                    },
                    onDateClick = { date ->
                        when {
                            date < today -> Unit
                            selectedStartDate != selectedEndDate -> {
                                selectedStartDate = date
                                selectedEndDate = date
                            }
                            date < selectedStartDate -> {
                                selectedStartDate = date
                                selectedEndDate = date
                            }
                            else -> selectedEndDate = date
                        }
                    },
                )
                Text("오늘 이후 날짜만 선택할 수 있어요.", color = WwmMuted, fontSize = 12.sp)
            }

            FormCard("인원 및 옵션") {
                OutlinedTextField(
                    value = maxParticipantsText,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit)
                        maxParticipantsText = when {
                            digits.isBlank() -> ""
                            digits.toIntOrNull() == null -> maxParticipantsText
                            else -> digits.toInt().coerceAtMost(MAX_MEETING_PARTICIPANTS).toString()
                        }
                    },
                    label = { Text("최대 인원") },
                    supportingText = { Text("2~8명까지 설정할 수 있어요.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        WwmPrimaryButton(
            text = "⊕ 약속방 만들기",
            onClick = {
                onCreate(
                    CreateRoomInput(
                        hostNickname = hostNickname,
                        title = title,
                        description = description,
                        meetingType = meetingType,
                        startDate = selectedStartDate,
                        endDate = selectedEndDate,
                        minParticipants = 1,
                        maxParticipants = maxParticipants ?: MIN_MEETING_PARTICIPANTS,
                        responseDeadline = null,
                    ),
                )
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = canCreate,
        )
    }
}

@Composable
private fun DateRangePickerCalendar(
    visibleMonth: LocalDate,
    today: LocalDate,
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousMonth, enabled = visibleMonth > today.firstDayOfMonth()) {
                Text("이전")
            }
            Text(
                "${visibleMonth.year}년 ${visibleMonth.monthNumber}월",
                color = WwmText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onNextMonth) {
                Text("다음")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(it, Modifier.weight(1f), color = WwmMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
        val dates = visibleMonth.monthDates()
        val leading = (visibleMonth.dayOfWeek.ordinal + 1) % 7
        (List<LocalDate?>(leading) { null } + dates).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DateRangeCell(
                            date = date,
                            enabled = date >= today,
                            isStart = date == selectedStartDate,
                            isEnd = date == selectedEndDate,
                            isInRange = date >= selectedStartDate && date <= selectedEndDate,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
            }
        }
    }
}

@Composable
private fun DateRangeCell(
    date: LocalDate,
    enabled: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEdge = isStart || isEnd
    val background = when {
        !enabled -> Color.Transparent
        isEdge -> WwmIndigo
        isInRange -> WwmSoftIndigo
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> WwmBorder
        isEdge -> Color.White
        else -> WwmText
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isEdge) WwmIndigo else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(date.dayOfMonth.toString(), color = textColor, fontWeight = if (isEdge) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, monthNumber, 1)

private fun LocalDate.monthDates(): List<LocalDate> {
    val nextMonth = firstDayOfMonth().plus(DatePeriod(months = 1))
    return generateSequence(firstDayOfMonth()) { date ->
        LocalDate.fromEpochDays(date.toEpochDays() + 1)
    }.takeWhile { it < nextMonth }.toList()
}

private fun LocalDate.toDateLabel(): String = "${year}.${monthNumber.toString().padStart(2, '0')}.${dayOfMonth.toString().padStart(2, '0')}"

@Composable
private fun FormCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = WwmMuted, fontSize = 13.sp) }
            content()
        }
    }
}
