package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.CalendarEventDraft
import com.garam.whenwheremeet.presentation.state.toKoreanDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAddBottomSheet(
    event: CalendarEventDraft,
    onAddToCalendar: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WwmSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📅", style = MaterialTheme.typography.headlineLarge)
            Text(
                "약속이 확정됐어요",
                color = WwmText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "기본 캘린더 앱에서 일정을 확인한 뒤 저장해 주세요.",
                color = WwmMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(
                Modifier.fillMaxWidth().background(WwmSurfaceSubtle, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CalendarEventRow(label = "약속", value = event.title)
                CalendarEventRow(label = "날짜", value = "${event.date.toKoreanDate()} · 종일")
                event.location?.let { CalendarEventRow(label = "장소", value = it) }
            }
            WwmPrimaryButton(text = "캘린더에 추가", onClick = onAddToCalendar)
            WwmOutlineButton(
                text = "나중에",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CalendarEventRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.25f),
            color = WwmMuted,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            value,
            modifier = Modifier.weight(0.75f),
            color = WwmText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
