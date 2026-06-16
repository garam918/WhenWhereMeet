package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmEmptyState
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmNavBackground
import com.garam.whenwheremeet.presentation.component.WwmSectionHeader
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.component.WwmTopBar
import com.garam.whenwheremeet.presentation.state.HomeUiState
import com.garam.whenwheremeet.presentation.state.toKoreanDate

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(WwmBackground)) {
        Column(Modifier.fillMaxSize()) {
            WwmTopBar(trailingText = "나")
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    top = 28.dp,
                    end = 20.dp,
                    bottom = 108.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("날짜부터 장소까지,", color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        Text("약속을 쉽게 정해요", color = WwmIndigo, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                }
                homeSection(
                    title = "진행 중인 약속",
                    icon = "↻",
                    rooms = state.rooms.filter { it.status == MeetingStatus.COLLECTING_AVAILABILITY },
                    emptyText = "진행 중인 약속이 없어요.",
                    onOpenRoom = onOpenRoom,
                )
                homeSection(
                    title = "응답 대기 중",
                    icon = "⌛",
                    rooms = state.rooms.filter { it.status == MeetingStatus.DATE_CONFIRMED || it.status == MeetingStatus.PLACE_SELECTING },
                    emptyText = "응답이 필요한 약속이 없어요.",
                    onOpenRoom = onOpenRoom,
                )
                homeSection(
                    title = "확정된 약속",
                    icon = "●",
                    rooms = state.rooms.filter { it.status == MeetingStatus.PLACE_CONFIRMED },
                    emptyText = "확정된 약속이 아직 없어요.",
                    onOpenRoom = onOpenRoom,
                )
                item { Spacer(Modifier.height(12.dp)) }
            }
        }

        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 88.dp)
                .size(56.dp).shadow(6.dp, CircleShape).background(WwmIndigo, CircleShape)
                .clickable(onClick = onCreateRoom),
            contentAlignment = Alignment.Center,
        ) { Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light) }

        HomeBottomBar(
            onJoinRoom = onJoinRoom,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeSection(
    title: String,
    icon: String,
    rooms: List<MeetingRoom>,
    emptyText: String,
    onOpenRoom: (String) -> Unit,
) {
    item { WwmSectionHeader(icon, title) }
    if (rooms.isEmpty()) {
        item { WwmEmptyState(emptyText) }
    } else {
        items(rooms, key = { it.id }) { room -> MeetingHomeCard(room, onOpenRoom) }
    }
}

@Composable
private fun MeetingHomeCard(room: MeetingRoom, onOpenRoom: (String) -> Unit) {
    WwmCard(Modifier.fillMaxWidth(), onClick = { onOpenRoom(room.id) }) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusPill(
                    when (room.status) {
                        MeetingStatus.COLLECTING_AVAILABILITY -> "날짜 조율 중"
                        MeetingStatus.DATE_CONFIRMED -> "장소 조율 중"
                        MeetingStatus.PLACE_SELECTING -> "장소 투표 중"
                        MeetingStatus.PLACE_CONFIRMED -> "확정"
                        else -> "준비 중"
                    },
                )
                Text("방 코드 ${room.id}", color = WwmMuted, fontSize = 12.sp)
            }
            Text(room.title, color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                room.confirmedDate?.let { "▣ ${it.toKoreanDate()}" }
                    ?: "▣ 후보: ${room.dateRangeStart.toKoreanDate()}",
                color = WwmMuted,
                fontSize = 14.sp,
            )
            HorizontalDivider(color = WwmBorder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(room.meetingType.label, color = WwmMuted, fontSize = 13.sp)
                Text("자세히 보기", color = WwmIndigo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HomeBottomBar(onJoinRoom: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(80.dp).background(WwmNavBackground, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 36.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomItem("⌂", "홈", selected = true)
        BottomItem("▣", "약속", onClick = onJoinRoom)
        BottomItem("○", "내 정보")
    }
}

@Composable
private fun BottomItem(icon: String, label: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Column(
        Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
            .then(if (selected) Modifier.background(WwmIndigo, RoundedCornerShape(999.dp)).padding(horizontal = 20.dp, vertical = 5.dp) else Modifier.padding(horizontal = 20.dp, vertical = 5.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = if (selected) Color.White else WwmMuted, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(label, color = if (selected) Color.White else WwmMuted, fontSize = 11.sp)
    }
}
