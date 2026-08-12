package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WwmStepProgress(
    labels: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        labels.forEachIndexed { index, label ->
            val step = index + 1
            val completed = step < currentStep
            val active = step == currentStep
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        Box(
                            Modifier.weight(1f).height(2.dp)
                                .background(if (completed || active) WwmIndigo else WwmBorder),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Box(
                        Modifier.size(28.dp)
                            .background(
                                if (completed || active) WwmIndigo else WwmSurfaceSubtle,
                                CircleShape,
                            )
                            .border(1.dp, if (completed || active) WwmIndigo else WwmBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (completed) "✓" else step.toString(),
                            color = if (completed || active) Color.White else WwmMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (index < labels.lastIndex) {
                        Box(
                            Modifier.weight(1f).height(2.dp)
                                .background(if (completed) WwmIndigo else WwmBorder),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Text(
                    label,
                    color = if (active) WwmIndigo else WwmMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun WwmInfoPanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: String = "i",
    accentColor: Color = WwmIndigo,
) {
    Row(
        modifier.fillMaxWidth()
            .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(28.dp).background(accentColor.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = WwmText, style = MaterialTheme.typography.titleSmall)
            Text(description, color = WwmMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WwmBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = WwmSoftIndigo,
    contentColor: Color = WwmIndigo,
) {
    Box(
        modifier.background(containerColor, RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WwmLoadingState(text: String, modifier: Modifier = Modifier) {
    WwmEmptyState(text = text, modifier = modifier, icon = "…")
}

@Composable
fun WwmErrorState(text: String, modifier: Modifier = Modifier) {
    WwmInfoPanel(
        title = "다시 확인해주세요",
        description = text,
        modifier = modifier,
        icon = "!",
        accentColor = WwmError,
    )
}
