package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.platform.currentLocalDate
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
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
    var monthText by remember { mutableStateOf("${today.year}-${today.monthNumber.toString().padStart(2, '0')}") }
    var maxParticipantsText by remember { mutableStateOf("6") }
    var hostIsRequired by remember { mutableStateOf(false) }

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
                MeetingType.entries.chunked(3).forEach { types ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { type ->
                            FilterChip(selected = meetingType == type, onClick = { meetingType = type }, label = { Text(type.label) })
                        }
                    }
                }
            }

            FormCard("일정 및 기한", "참석자들이 투표할 기간을 설정해주세요.") {
                Text("후보 기간 선택", color = WwmMuted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("이번 달" to 0, "다음 달" to 1).forEach { (label, monthOffset) ->
                        FilterChip(
                            selected = monthText == today.plus(DatePeriod(months = monthOffset)).yearMonthText(),
                            onClick = {
                                monthText = today.plus(DatePeriod(months = monthOffset)).yearMonthText()
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it },
                    label = { Text("후보 월 (YYYY-MM)") },
                    singleLine = true,
                    keyboardOptions = doneKeyboardOptions,
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            FormCard("인원 및 옵션") {
                OutlinedTextField(
                    value = maxParticipantsText,
                    onValueChange = { maxParticipantsText = it.filter(Char::isDigit) },
                    label = { Text("최대 인원") },
                    supportingText = { Text("정원이 차면 새 참여자는 입장할 수 없어요.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = doneKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("필수 참석자 사용 여부", color = WwmText, fontWeight = FontWeight.Medium)
                        Text("주최자를 필수 참석자로 지정합니다.", color = WwmMuted, fontSize = 12.sp)
                    }
                    Checkbox(checked = hostIsRequired, onCheckedChange = { hostIsRequired = it })
                }
            }
        }
        WwmPrimaryButton(
            text = "⊕ 약속방 만들기",
            onClick = {
                monthText.toMonthDateRange()?.let { (start, end) ->
                    onCreate(
                        CreateRoomInput(
                            hostNickname = hostNickname,
                            title = title,
                            description = description,
                            meetingType = meetingType,
                            startDate = start,
                            endDate = end,
                            minParticipants = 1,
                            maxParticipants = maxParticipantsText.toIntOrNull() ?: 2,
                            responseDeadline = null,
                            hostIsRequired = hostIsRequired,
                        ),
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

private fun LocalDate.yearMonthText(): String = "$year-${monthNumber.toString().padStart(2, '0')}"

private fun String.toMonthDateRange(): Pair<LocalDate, LocalDate>? {
    val parts = split("-")
    if (parts.size != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val start = LocalDate(year, month, 1)
    val end = LocalDate.fromEpochDays(start.plus(DatePeriod(months = 1)).toEpochDays() - 1)
    return start to end
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
