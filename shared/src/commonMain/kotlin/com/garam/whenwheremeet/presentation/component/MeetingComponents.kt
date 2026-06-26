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
            listOf("월", "화", "수", "목", "금", "토", "일").forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
        }
        val leading = startDate.dayOfWeek.ordinal
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
        AvailabilityStatus.AVAILABLE -> WwmMint
        AvailabilityStatus.MAYBE -> WwmOrange
        AvailabilityStatus.UNAVAILABLE -> Color(0xFFE2E8F8)
        null -> Color.Transparent
    }
    val borderColor = when (status) {
        AvailabilityStatus.AVAILABLE -> WwmMint
        AvailabilityStatus.MAYBE -> WwmOrange
        AvailabilityStatus.UNAVAILABLE -> Color(0xFFE2E8F8)
        null -> Color.Transparent
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
        Text(date.dayOfMonth.toString(), fontWeight = FontWeight.SemiBold)
        if (summary != null && summary.availableParticipants.isNotEmpty()) {
            Box(Modifier.size(5.dp).background(WwmIndigo, RoundedCornerShape(99.dp)))
        }
    }
}

@Composable
fun AvailabilityLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem("가능", WwmMint)
        LegendItem("조율 가능", WwmOrange)
        LegendItem("내 일정", WwmIndigo)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ConfirmedMeetingCard(room: MeetingRoom, participantCount: Int) {
    WwmCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusPill(if (room.confirmedPlace == null) "날짜 확정" else "장소 확정")
            Text(room.confirmedDate?.toKoreanDate().orEmpty(), color = WwmText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${room.title} · 참여자 ${participantCount}명", color = WwmMuted)
            Text("장소: ${room.confirmedPlace?.name ?: "미정"}", color = WwmMuted)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = WwmText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
fun StatusPill(text: String) {
    val confirmed = text.contains("확정")
    Surface(color = if (confirmed) WwmMint else WwmSoftIndigo, shape = RoundedCornerShape(100.dp)) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (confirmed) WwmMintText else WwmIndigo,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
