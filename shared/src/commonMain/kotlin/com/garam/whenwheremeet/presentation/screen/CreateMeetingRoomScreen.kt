package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.garam.whenwheremeet.presentation.component.WwmDesktopBreakpoint
import com.garam.whenwheremeet.presentation.component.WwmDesktopContentMaxWidth
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmInfoPanel
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmStepProgress
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
    var currentStep by remember { mutableIntStateOf(1) }
    val maxParticipants = maxParticipantsText.toIntOrNull()
    val isBasicInfoValid = hostNickname.isNotBlank() && title.isNotBlank()
    val isDateRangeValid = selectedStartDate >= today && selectedEndDate >= selectedStartDate
    val isParticipantCountValid = maxParticipants != null &&
        maxParticipants in MIN_MEETING_PARTICIPANTS..MAX_MEETING_PARTICIPANTS
    val canContinue = when (currentStep) {
        1 -> isBasicInfoValid
        2 -> isDateRangeValid
        else -> isParticipantCountValid
    }

    BoxWithConstraints(modifier.fillMaxSize().background(WwmBackground)) {
        val screenWidth = maxWidth
        val isDesktop = screenWidth >= WwmDesktopBreakpoint
        Column(Modifier.fillMaxSize()) {
            WwmTopBar(
                title = if (isDesktop) "언제어디" else "새 약속 만들기",
                leadingText = if (isDesktop) "‹" else "×",
                onLeadingClick = onBack,
                trailingText = if (isDesktop) "저장" else null,
                onTrailingClick = if (isDesktop) onBack else null,
            )
            WwmStepProgress(
                labels = listOf("기본 정보", "날짜", "참여자"),
                currentStep = currentStep,
                modifier = Modifier.fillMaxWidth().widthIn(max = if (isDesktop) 640.dp else screenWidth)
                    .align(Alignment.CenterHorizontally).padding(horizontal = 16.dp, vertical = if (isDesktop) 28.dp else 14.dp),
            )
            if (isDesktop && currentStep == 2) {
                DesktopCreateDateStep(
                    title = title,
                    meetingType = meetingType,
                    visibleMonth = visibleMonth,
                    today = today,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onPreviousMonth = {
                        val previous = visibleMonth.plus(DatePeriod(months = -1)).firstDayOfMonth()
                        if (previous >= today.firstDayOfMonth()) visibleMonth = previous
                    },
                    onNextMonth = { visibleMonth = visibleMonth.plus(DatePeriod(months = 1)).firstDayOfMonth() },
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
                    modifier = Modifier.weight(1f).align(Alignment.CenterHorizontally),
                )
            } else {
                Column(
                    Modifier.weight(1f).fillMaxWidth().widthIn(max = if (isDesktop) 760.dp else screenWidth)
                        .align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    when (currentStep) {
                1 -> {
                    StepHeading("새로운 약속 만들기", "어떤 모임인지 알려주세요.")
                    FormCard("기본 정보") {
                        OutlinedTextField(
                            value = hostNickname,
                            onValueChange = { hostNickname = it },
                            label = { Text("방장 닉네임") },
                            singleLine = true,
                            keyboardOptions = doneKeyboardOptions,
                            keyboardActions = doneKeyboardActions,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("약속 이름") },
                            placeholder = { Text("예: 동아리 회식") },
                            singleLine = true,
                            keyboardOptions = doneKeyboardOptions,
                            keyboardActions = doneKeyboardActions,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
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
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("메모 (선택)") },
                            placeholder = { Text("참여자에게 전할 내용을 적어주세요.") },
                            minLines = 3,
                            keyboardOptions = doneKeyboardOptions,
                            keyboardActions = doneKeyboardActions,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                2 -> {
                    StepHeading("언제 만날까요?", "참여자들이 응답할 후보 기간을 선택해주세요.")
                    FormCard("후보 날짜") {
                        Text(
                            "${selectedStartDate.toDateLabel()}  →  ${selectedEndDate.toDateLabel()}",
                            color = WwmIndigo,
                            fontWeight = FontWeight.Bold,
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
                            onNextMonth = { visibleMonth = visibleMonth.plus(DatePeriod(months = 1)).firstDayOfMonth() },
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
                    }
                    WwmInfoPanel("날짜 선택 안내", "첫 번째 탭은 시작일, 두 번째 탭은 종료일이 됩니다.", icon = "i")
                }

                else -> {
                    StepHeading("약속방을 완성해볼까요?", "참여 인원을 확인하고 약속방을 만들어주세요.")
                    FormCard("참여자 설정") {
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = doneKeyboardActions,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        WwmInfoPanel("방장은 필수 참여자예요", "약속을 만든 뒤 초대 링크나 방 코드로 참여자를 불러보세요.", icon = "✓")
                    }
                    FormCard("입력 정보 요약") {
                        SummaryRow("약속", title)
                        SummaryRow("모임 성격", meetingType.label)
                        SummaryRow("후보 기간", "${selectedStartDate.toDateLabel()} ~ ${selectedEndDate.toDateLabel()}")
                        SummaryRow("최대 인원", "${maxParticipants ?: "-"}명")
                    }
                }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    Modifier.fillMaxWidth().widthIn(max = if (isDesktop) 560.dp else screenWidth)
                        .align(Alignment.Center).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (currentStep > 1) {
                        com.garam.whenwheremeet.presentation.component.WwmOutlineButton(
                            text = "이전",
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    WwmPrimaryButton(
                        text = if (currentStep < 3) "다음으로" else "약속방 만들기",
                        onClick = {
                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                onCreate(
                                    CreateRoomInput(
                                        hostNickname = hostNickname.trim(),
                                        title = title.trim(),
                                        description = description.trim(),
                                        meetingType = meetingType,
                                        startDate = selectedStartDate,
                                        endDate = selectedEndDate,
                                        minParticipants = 1,
                                        maxParticipants = maxParticipants ?: MIN_MEETING_PARTICIPANTS,
                                        responseDeadline = null,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.weight(if (currentStep > 1) 1.7f else 1f),
                        enabled = canContinue,
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopCreateDateStep(
    title: String,
    meetingType: MeetingType,
    visibleMonth: LocalDate,
    today: LocalDate,
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().widthIn(max = WwmDesktopContentMaxWidth).padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StepHeading("언제 만날까요?", "모임이 가능한 날짜 범위를 선택해 주세요.")
            FormCard("후보 날짜") {
                DateRangePickerCalendar(
                    visibleMonth = visibleMonth,
                    today = today,
                    selectedStartDate = selectedStartDate,
                    selectedEndDate = selectedEndDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onDateClick = onDateClick,
                )
            }
        }
        WwmCard(Modifier.width(360.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("실시간 요약", color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                SummaryRow("약속명", title.ifBlank { "입력 전" })
                SummaryRow("장소 성격", meetingType.label)
                androidx.compose.material3.HorizontalDivider(color = WwmBorder)
                Column(
                    Modifier.fillMaxWidth().background(WwmSoftIndigo, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("선택 기간", color = WwmMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${selectedStartDate.toDateLabel()} ~ ${selectedEndDate.toDateLabel()}",
                        color = WwmIndigo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                WwmInfoPanel("날짜 선택 안내", "첫 번째 탭은 시작일, 두 번째 탭은 종료일이 됩니다.", icon = "i")
            }
        }
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
private fun StepHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = WwmText, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = WwmMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WwmMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = WwmText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

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
