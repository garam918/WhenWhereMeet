package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.platform.currentLocalDate
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
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
    val monthStart = remember { LocalDate(today.year, today.monthNumber, 1) }
    val monthEnd = remember { LocalDate.fromEpochDays(monthStart.plus(DatePeriod(months = 1)).toEpochDays() - 1) }
    var hostNickname by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var meetingType by remember { mutableStateOf(MeetingType.MEAL) }
    var monthText by remember { mutableStateOf("${today.year}-${today.monthNumber.toString().padStart(2, '0')}") }
    var startText by remember { mutableStateOf(monthStart.toString()) }
    var endText by remember { mutableStateOf(monthEnd.toString()) }
    var minParticipantsText by remember { mutableStateOf("3") }
    var deadlineText by remember { mutableStateOf("") }
    var hostIsRequired by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(WwmBackground)) {
        WwmTopBar(title = "새 약속 만들기", leadingText = "×", onLeadingClick = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FormCard("기본 정보", "어떤 모임을 계획하고 계신가요?") {
                OutlinedTextField(hostNickname, { hostNickname = it }, label = { Text("방장 닉네임") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(title, { title = it }, label = { Text("약속 이름") }, placeholder = { Text("예: 주말 한강 피크닉") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("설명 또는 메모") }, minLines = 2, modifier = Modifier.fillMaxWidth())
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
                    listOf("이번 달", "다음 달", "직접 선택").forEachIndexed { index, label ->
                        FilterChip(
                            selected = index == 0,
                            onClick = {
                                if (index == 0) {
                                    startText = monthStart.toString()
                                    endText = monthEnd.toString()
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(monthText, { monthText = it }, label = { Text("특정 월 (YYYY-MM)") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startText, { startText = it }, label = { Text("시작일") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(endText, { endText = it }, label = { Text("종료일") }, modifier = Modifier.weight(1f))
                }
                HorizontalDivider(color = WwmBorder)
                OutlinedTextField(deadlineText, { deadlineText = it }, label = { Text("응답 마감일 (선택)") }, modifier = Modifier.fillMaxWidth())
                Text("마감일이 지나면 투표가 자동으로 종료됩니다.", color = WwmMuted, fontSize = 12.sp)
            }

            FormCard("인원 및 옵션") {
                OutlinedTextField(
                    minParticipantsText,
                    { minParticipantsText = it.filter(Char::isDigit) },
                    label = { Text("최소 참석 인원") },
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
                val start = runCatching { LocalDate.parse(startText) }.getOrNull()
                val end = runCatching { LocalDate.parse(endText) }.getOrNull()
                if (start != null && end != null) {
                    onCreate(
                        CreateRoomInput(
                            hostNickname = hostNickname,
                            title = title,
                            description = description,
                            meetingType = meetingType,
                            startDate = start,
                            endDate = end,
                            minParticipants = minParticipantsText.toIntOrNull() ?: 1,
                            responseDeadline = deadlineText.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            hostIsRequired = hostIsRequired,
                        ),
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
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
