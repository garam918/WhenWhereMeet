package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WwmDesktopBreakpoint: Dp = 900.dp
val WwmDesktopContentMaxWidth: Dp = 1200.dp

@Composable
fun WwmDesktopTopBar(
    selectedTab: MainTab,
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyPageClick: () -> Unit,
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).widthIn(max = WwmDesktopContentMaxWidth)
                .align(Alignment.CenterHorizontally).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text("언제어디", color = WwmIndigo, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    DesktopNavItem("Home", selectedTab == MainTab.HOME, onHomeClick)
                    DesktopNavItem("Calendar", selectedTab == MainTab.CALENDAR, onCalendarClick)
                    DesktopNavItem("Profile", selectedTab == MainTab.MY_PAGE, onMyPageClick)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.background(WwmIndigo, RoundedCornerShape(14.dp)).clickable(onClick = onCreateRoom)
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋ 약속 만들기", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Box(
                    Modifier.size(40.dp).background(WwmSoftIndigo, CircleShape).clickable(onClick = onMyPageClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("나", color = WwmIndigo, fontWeight = FontWeight.Bold)
                }
            }
        }
        HorizontalDivider(color = WwmBorder)
    }
}

@Composable
private fun DesktopNavItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().width(104.dp)
            .semantics { role = Role.Tab; selected = isSelected }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (isSelected) WwmIndigo else WwmText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
        )
        if (isSelected) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(WwmIndigo),
            )
        }
    }
}

enum class WwmFlowSection(val label: String, val icon: String) {
    EVENT_DETAILS("약속 정보", "ⓘ"),
    PARTICIPANTS("참여자", "♟"),
    LOCATION_OPTIONS("장소 정하기", "⌖"),
    FINALIZE("최종 확정", "✓"),
}

@Composable
fun WwmDesktopFlowTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("‹", modifier = Modifier.clickable(onClick = onBack).padding(8.dp), color = WwmText, fontSize = 28.sp)
                Text("언제어디", color = WwmIndigo, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier.border(1.dp, WwmIndigo, RoundedCornerShape(12.dp)).clickable(onClick = onShare)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text("일정 공유하기", color = WwmIndigo, style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(color = WwmBorder)
    }
}

@Composable
fun WwmDesktopFlowSidebar(
    selectedSection: WwmFlowSection,
    onSelectSection: (WwmFlowSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.width(256.dp).fillMaxHeight().background(WwmSoftIndigo.copy(alpha = 0.55f))
            .border(width = 0.dp, color = Color.Transparent).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Coordination", color = WwmIndigo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("약속 조율 단계", color = WwmMuted, style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.height(18.dp))
        WwmFlowSection.entries.forEach { section ->
            val selected = section == selectedSection
            Row(
                Modifier.fillMaxWidth().background(if (selected) WwmIndigo else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onSelectSection(section) }.padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(section.icon, color = if (selected) Color.White else WwmText, fontSize = 18.sp)
                Text(
                    section.label,
                    color = if (selected) Color.White else WwmText,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun WwmDesktopMetricCard(
    label: String,
    value: String,
    accentColor: Color = WwmIndigo,
    modifier: Modifier = Modifier,
) {
    WwmCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = WwmMuted, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, color = accentColor, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("건", color = WwmMuted, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
