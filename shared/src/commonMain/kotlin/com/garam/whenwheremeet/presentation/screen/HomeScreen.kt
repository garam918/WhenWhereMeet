package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.MainBottomBar
import com.garam.whenwheremeet.presentation.component.MainTab
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmDesktopBreakpoint
import com.garam.whenwheremeet.presentation.component.WwmDesktopContentMaxWidth
import com.garam.whenwheremeet.presentation.component.WwmDesktopMetricCard
import com.garam.whenwheremeet.presentation.component.WwmDesktopTopBar
import com.garam.whenwheremeet.presentation.component.WwmEmptyState
import com.garam.whenwheremeet.presentation.component.WwmErrorState
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmLoadingState
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmOutlineButton
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSectionHeader
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmSurface
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.state.HomeDashboardUiState
import com.garam.whenwheremeet.presentation.state.HomeActionItemUiModel
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
    BoxWithConstraints(modifier.fillMaxSize().background(WwmBackground)) {
        if (maxWidth >= WwmDesktopBreakpoint) {
            DesktopHomeContent(
                state = state,
                onCreateRoom = onCreateRoom,
                onJoinRoom = onJoinRoom,
                onOpenRoom = onOpenRoom,
                onOpenMap = onOpenMap,
                onOpenCalendar = onOpenCalendar,
                onOpenMyPage = onOpenMyPage,
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        item { HomeHeader(onCreateRoom = onCreateRoom, onJoinRoom = onJoinRoom) }
                        if (state.isLoading) item { WwmLoadingState("약속을 불러오고 있어요.") }
                        state.errorMessage?.let { message -> item { WwmErrorState(message) } }
                        state.actionItems.firstOrNull()?.let { actionItem -> item { HomeNextActionCard(actionItem, onOpenRoom) } }
                        item { HomeSummaryCard(state) }
                        item { WwmSectionHeader("●", "다가오는 약속") }
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
                        item { WwmSectionHeader("✓", "지난 약속") }
                        if (state.pastMeetings.isEmpty()) {
                            item { WwmEmptyState("지난 약속이 아직 없어요.") }
                        } else {
                            items(state.pastMeetings, key = { "past-${it.roomId}" }) { meeting ->
                                PastMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom)
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
    }
}

@Composable
private fun DesktopHomeContent(
    state: HomeDashboardUiState,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMyPage: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WwmDesktopTopBar(
            selectedTab = MainTab.HOME,
            onHomeClick = {},
            onCalendarClick = onOpenCalendar,
            onMyPageClick = onOpenMyPage,
            onCreateRoom = onCreateRoom,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = WwmDesktopContentMaxWidth)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(2.1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        DesktopHomeHeader(onCreateRoom = onCreateRoom, onJoinRoom = onJoinRoom)
                        state.actionItems.firstOrNull()?.let { HomeNextActionCard(it, onOpenRoom) }
                        if (state.isLoading) WwmLoadingState("약속을 불러오고 있어요.")
                        state.errorMessage?.let { WwmErrorState(it) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        WwmDesktopMetricCard(
                            label = "진행 중인 약속",
                            value = (state.summary.pendingResponseCount + state.summary.placeVoteRequiredCount).toString(),
                        )
                        WwmDesktopMetricCard("확정된 일정", state.summary.confirmedCount.toString(), accentColor = WwmText)
                        WwmDesktopMetricCard("내 응답 필요", state.summary.pendingResponseCount.toString(), accentColor = com.garam.whenwheremeet.presentation.component.WwmError)
                    }
                }
            }
            item { WwmSectionHeader("●", "다가오는 약속") }
            if (state.upcomingConfirmedMeetings.isEmpty()) {
                item { WwmEmptyState("다가오는 확정 약속이 아직 없어요.") }
            } else {
                items(state.upcomingConfirmedMeetings, key = { "desktop-upcoming-${it.roomId}" }) { meeting ->
                    UpcomingMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom, onOpenMap = onOpenMap)
                }
            }
            item { WwmSectionHeader("↻", "진행 중인 약속") }
            if (state.inProgressMeetings.isEmpty()) {
                item { WwmEmptyState("조율 중인 약속이 없어요.") }
            } else {
                items(state.inProgressMeetings, key = { "desktop-progress-${it.roomId}" }) { meeting ->
                    InProgressMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom)
                }
            }
            item { WwmSectionHeader("✓", "지난 약속") }
            if (state.pastMeetings.isEmpty()) {
                item { WwmEmptyState("지난 약속이 아직 없어요.") }
            } else {
                items(state.pastMeetings, key = { "desktop-past-${it.roomId}" }) { meeting ->
                    PastMeetingCard(meeting = meeting, onOpenRoom = onOpenRoom)
                }
            }
        }
    }
}

@Composable
private fun DesktopHomeHeader(onCreateRoom: () -> Unit, onJoinRoom: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("안녕하세요!", color = WwmText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("오늘도 완벽한 모임을 준비해 볼까요?", color = WwmMuted, fontSize = 16.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WwmOutlineButton("방 코드 참여", onClick = onJoinRoom, modifier = Modifier.height(52.dp))
            WwmPrimaryButton("약속 만들기", onClick = onCreateRoom, modifier = Modifier.width(150.dp).height(52.dp))
        }
    }
}

@Composable
private fun HomeHeader(onCreateRoom: () -> Unit, onJoinRoom: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("언제어디", color = WwmIndigo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("안녕하세요,\n약속을 정해볼까요?", color = WwmText, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
        Text("날짜부터 장소까지 한 번에 정리해드려요.", color = WwmMuted, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WwmPrimaryButton("＋ 약속 만들기", onClick = onCreateRoom, modifier = Modifier.weight(1f).height(52.dp))
            WwmOutlineButton("방 코드로 참여", onClick = onJoinRoom, modifier = Modifier.weight(1f).height(52.dp))
        }
    }
}

@Composable
private fun HomeNextActionCard(item: HomeActionItemUiModel, onOpenRoom: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(WwmSoftIndigo, RoundedCornerShape(16.dp))
            .border(1.dp, WwmIndigo.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("다음 할 일", color = WwmIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            item.dueText?.let { Text(it, color = WwmMuted, fontSize = 12.sp) }
        }
        Text(item.title, color = WwmText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(item.description, color = WwmMuted, fontSize = 14.sp, lineHeight = 20.sp)
        Box(
            Modifier.fillMaxWidth().background(WwmSurface, RoundedCornerShape(12.dp))
                .clickable { onOpenRoom(item.roomId) }.padding(13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.ctaText, color = WwmIndigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeSummaryCard(state: HomeDashboardUiState) {
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("내 약속 현황", color = WwmText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                HomeMetric("확정", state.summary.confirmedCount.toString())
                HomeMetric("날짜 응답", state.summary.pendingResponseCount.toString())
                HomeMetric("장소 투표", state.summary.placeVoteRequiredCount.toString())
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, color = WwmIndigo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = WwmMuted, fontSize = 12.sp)
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

@Composable
private fun PastMeetingCard(meeting: HomeMeetingCardUiModel, onOpenRoom: (String) -> Unit) {
    WwmCard(Modifier.fillMaxWidth(), onClick = { onOpenRoom(meeting.roomId) }) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                StatusPill(meeting.statusText)
                Text(meeting.title, color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(meeting.dateText, color = WwmMuted, fontSize = 14.sp)
            }
            Text("상세 보기", color = WwmIndigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
