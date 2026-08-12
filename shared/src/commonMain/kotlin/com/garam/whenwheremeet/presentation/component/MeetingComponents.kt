package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.DateAvailabilitySummary
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.presentation.state.toKoreanDate
import kotlinx.datetime.LocalDate

@Composable
fun AvailabilityCalendar(
    startDate: LocalDate,
    endDate: LocalDate,
    selectedValues: Map<LocalDate, AvailabilityStatus>,
    summaries: List<DateAvailabilitySummary>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val summaryByDate = summaries.associateBy { it.date }
    val dates = generateSequence(startDate) { date ->
        runCatching { LocalDate.fromEpochDays(date.toEpochDays() + 1) }.getOrNull()
    }.takeWhile { it <= endDate }.toList()

    WwmCard(modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
        }
        val leading = (startDate.dayOfWeek.ordinal + 1) % 7
        (List<LocalDate?>(leading) { null } + dates).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(0.8f))
                    } else {
                        DateCell(
                            date = date,
                            status = selectedValues[date],
                            summary = summaryByDate[date],
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).aspectRatio(0.8f)) }
            }
        }
    }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    status: AvailabilityStatus?,
    summary: DateAvailabilitySummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val background = when (status) {
        AvailabilityStatus.AVAILABLE -> WwmMint.copy(alpha = 0.16f)
        AvailabilityStatus.MAYBE -> WwmOrange.copy(alpha = 0.16f)
        AvailabilityStatus.UNAVAILABLE -> WwmError.copy(alpha = 0.12f)
        null -> Color.Transparent
    }
    val borderColor = when (status) {
        AvailabilityStatus.AVAILABLE -> WwmMint
        AvailabilityStatus.MAYBE -> WwmOrange
        AvailabilityStatus.UNAVAILABLE -> WwmError
        null -> WwmBorder
    }
    val textColor = when (status) {
        AvailabilityStatus.AVAILABLE -> WwmMintText
        AvailabilityStatus.MAYBE -> Color(0xFF9A6200)
        AvailabilityStatus.UNAVAILABLE -> WwmError
        null -> WwmText
    }
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(5.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(date.dayOfMonth.toString(), color = textColor, fontWeight = FontWeight.SemiBold)
        if (summary != null && summary.availableParticipants.isNotEmpty()) {
            Box(Modifier.size(5.dp).background(WwmIndigo, RoundedCornerShape(99.dp)))
        }
    }
}

@Composable
fun AvailabilityLegend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LegendItem("가능", WwmMint, Modifier.weight(1f))
        LegendItem("애매", WwmOrange, Modifier.weight(1f))
        LegendItem("불가", WwmError, Modifier.weight(1f))
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, color = WwmText, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ConfirmedMeetingCard(room: MeetingRoom, participantCount: Int) {
    WwmCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(if (room.confirmedPlace == null) "날짜 확정" else "장소 확정")
            Text("약속이 확정됐어요", color = WwmMintText, style = MaterialTheme.typography.labelLarge)
            Text(room.confirmedDate?.toKoreanDate().orEmpty(), color = WwmText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${room.title} · 참여자 ${participantCount}명", color = WwmMuted)
            Text("장소: ${room.confirmedPlace?.name ?: "미정"}", color = WwmMuted)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = WwmText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
fun StatusPill(text: String) {
    val confirmed = text.contains("확정")
    Surface(color = if (confirmed) WwmMint.copy(alpha = 0.14f) else WwmSoftIndigo, shape = RoundedCornerShape(100.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (confirmed) WwmMintText else WwmIndigo,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
