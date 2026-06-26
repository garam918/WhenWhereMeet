package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.MainBottomBar
import com.garam.whenwheremeet.presentation.component.MainTab
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmEmptyState
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmOutlineButton
import com.garam.whenwheremeet.presentation.component.WwmSectionHeader
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.state.HomeDashboardUiState
import com.garam.whenwheremeet.presentation.state.HomeMeetingCardUiModel

@Composable
fun HomeScreen(
    state: HomeDashboardUiState,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMyPage: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(WwmBackground)) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item { HomeHeader(onCreateRoom = onCreateRoom, onJoinRoom = onJoinRoom) }
                item { HomeSummaryCard(state) }
                item { WwmSectionHeader("●", "다가오는 확정 약속") }
                if (state.upcomingConfirmedMeetings.isEmpty()) {
                    item { WwmEmptyState("다가오는 확정 약속이 아직 없어요.") }
                } else {
                    items(state.upcomingConfirmedMeetings, key = { "upcoming-${it.roomId}" }) { meeting ->
                        UpcomingMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom, onOpenMap = onOpenMap)
                    }
                }
                item { WwmSectionHeader("↻", "진행 중인 약속") }
                if (state.inProgressMeetings.isEmpty()) {
                    item { WwmEmptyState("조율 중인 약속이 없어요.") }
                } else {
                    items(state.inProgressMeetings, key = { "in-progress-${it.roomId}" }) { meeting ->
                        InProgressMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom)
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        MainBottomBar(
            selectedTab = MainTab.HOME,
            onHomeClick = {},
            onCalendarClick = onOpenCalendar,
            onMyPageClick = onOpenMyPage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HomeHeader(onCreateRoom: () -> Unit, onJoinRoom: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("언제어디", color = WwmIndigo, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("날짜부터 장소까지, 약속을 쉽게 정해요", color = WwmMuted, fontSize = 15.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WwmOutlineButton("새 약속 만들기", onClick = onCreateRoom, modifier = Modifier.weight(1f))
            WwmOutlineButton("방 코드로 참여", onClick = onJoinRoom, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeSummaryCard(state: HomeDashboardUiState) {
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("이번 주 약속", color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("확정 ${state.summary.confirmedCount}개 · 진행 중 ${state.inProgressMeetings.size}개", color = WwmMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun UpcomingMeetingCard(
    meeting: HomeMeetingCardUiModel,
    onOpenRoom: (String) -> Unit,
    onOpenMap: (String) -> Unit,
) {
    WwmCard(Modifier.fillMaxWidth(), onClick = { onOpenRoom(meeting.roomId) }) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(meeting.statusText)
                Text(meeting.participantText, color = WwmMuted, fontSize = 12.sp)
            }
            Text(meeting.title, color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("${meeting.dateText} · ${meeting.timeText ?: "시간 미정"}", color = WwmMuted, fontSize = 14.sp)
            Text(meeting.placeText ?: "장소 미정", color = WwmText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            meeting.travelTimeText?.let { Text(it, color = WwmMuted, fontSize = 13.sp) }
            HorizontalDivider(color = WwmBorder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WwmOutlineButton("지도 보기", onClick = { onOpenMap(meeting.roomId) }, modifier = Modifier.weight(1f))
                WwmOutlineButton("상세 보기", onClick = { onOpenRoom(meeting.roomId) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InProgressMeetingCard(meeting: HomeMeetingCardUiModel, onOpenRoom: (String) -> Unit) {
    WwmCard(Modifier.fillMaxWidth(), onClick = { onOpenRoom(meeting.roomId) }) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(meeting.statusText)
                Text(meeting.participantText, color = WwmMuted, fontSize = 12.sp)
            }
            Text(meeting.title, color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            meeting.responseText?.let {
                Text(it, color = WwmMuted, fontSize = 14.sp)
            }
            Text("유력 날짜 ${meeting.dateText}", color = WwmMuted, fontSize = 14.sp)
            Row(
                Modifier.fillMaxWidth().background(WwmSoftIndigo, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(meeting.ctaText, color = WwmIndigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("약속방 보기", color = WwmIndigo, fontSize = 13.sp)
            }
        }
    }
}
