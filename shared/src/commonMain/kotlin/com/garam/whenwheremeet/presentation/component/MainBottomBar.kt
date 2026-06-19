package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MainTab {
    HOME,
    CALENDAR,
    MY_PAGE,
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyPageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(80.dp)
            .background(WwmNavBackground, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainBottomItem("⌂", "홈", selectedTab == MainTab.HOME, onHomeClick)
        MainBottomItem("▣", "캘린더", selectedTab == MainTab.CALENDAR, onCalendarClick)
        MainBottomItem("○", "내 정보", selectedTab == MainTab.MY_PAGE, onMyPageClick)
    }
}

@Composable
private fun MainBottomItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.background(WwmIndigo, RoundedCornerShape(999.dp)).padding(horizontal = 18.dp, vertical = 5.dp)
                } else {
                    Modifier.padding(horizontal = 18.dp, vertical = 5.dp)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = if (selected) Color.White else WwmMuted, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(label, color = if (selected) Color.White else WwmMuted, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 11.sp)
    }
}
