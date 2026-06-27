package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.presentation.component.AvailabilityCalendar
import com.garam.whenwheremeet.presentation.component.AvailabilityLegend
import com.garam.whenwheremeet.presentation.component.ConfirmedMeetingCard
import com.garam.whenwheremeet.presentation.component.PlaceRecommendationTab
import com.garam.whenwheremeet.presentation.component.SectionTitle
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmOutlineButton
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.component.WwmTopBar
import com.garam.whenwheremeet.presentation.state.MeetingRoomUiState
import com.garam.whenwheremeet.presentation.state.toKoreanDate
import kotlinx.datetime.LocalDate

private enum class MeetingRoomTab(val label: String) {
    DATE("날짜"),
    PLACE("장소"),
    PARTICIPANTS("참여자"),
}

@Composable
fun MeetingRoomScreen(
    state: MeetingRoomUiState,
    onBack: () -> Unit,
    onCycleDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onShare: () -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSaveTransportMode: (TransportMode) -> Unit,
    onCalculateAreas: () -> Unit,
    onSelectArea: (String) -> Unit,
    onSearchPlaces: () -> Unit,
    onVotePlace: (String, PlaceVoteType) -> Unit,
    onConfirmPlace: (PlaceCandidate) -> Unit,
    onOpenMap: (PlaceCandidate) -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = state.room
    var selectedTab by remember { mutableStateOf(MeetingRoomTab.DATE) }
    Column(modifier.fillMaxSize().background(WwmBackground)) {
        WwmTopBar(
            title = "언제어디",
            leadingText = "‹",
            onLeadingClick = onBack,
            trailingText = "↗",
            onTrailingClick = onShare,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(room.title, color = WwmText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    room.description?.let { Text(it, color = WwmMuted) }
                }
                StatusPill(
                    when (room.status) {
                        MeetingStatus.PLACE_SELECTING -> "지역 선택 중"
                        MeetingStatus.PLACE_CONFIRMED -> "장소 확정"
                        MeetingStatus.DATE_CONFIRMED -> "날짜 확정"
                        else -> "날짜 조율 중"
                    },
                )
            }
            Text("${room.meetingType.label} · 참여자 ${state.participants.size}/${room.maxParticipants}명 · 방 코드 ${room.id}", color = WwmMuted)
            Text("후보 기간 ${room.dateRangeStart} ~ ${room.dateRangeEnd}", color = WwmMuted)
            room.responseDeadline?.let { Text("응답 마감 $it", color = WwmMuted) }

            if (room.confirmedDate != null) ConfirmedMeetingCard(room, state.participants.size)

            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(999.dp)).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MeetingRoomTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (selectedTab) {
                MeetingRoomTab.DATE -> DateTab(state, onCycleDate, onSelectDate, onSave, onConfirm)
                MeetingRoomTab.PLACE -> PlaceRecommendationTab(
                    state = state,
                    onSearchLocations = onSearchLocations,
                    onUseCurrentLocation = onUseCurrentLocation,
                    onSaveStartLocation = onSaveStartLocation,
                    onSaveTransportMode = onSaveTransportMode,
                    onCalculateAreas = onCalculateAreas,
                    onSelectArea = onSelectArea,
                    onSearchPlaces = onSearchPlaces,
                    onVotePlace = onVotePlace,
                    onConfirmPlace = onConfirmPlace,
                    onOpenMap = onOpenMap,
                    onShare = onShare,
                )
                MeetingRoomTab.PARTICIPANTS -> ParticipantsTab(state, onLeaveRoom, onDeleteRoom)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DateTab(
    state: MeetingRoomUiState,
    onCycleDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val room = state.room
    var availabilityEditEnabled by remember(room.id, room.confirmedDate) {
        mutableStateOf(room.confirmedDate == null)
    }
    val canEditAvailability = room.confirmedDate == null || availabilityEditEnabled
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("내 가능한 날짜")
        if (room.confirmedDate == null) {
            Text("날짜를 누르면 미선택 → 가능 → 애매 → 불가능 순서로 바뀝니다.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("약속 날짜가 확정되어 날짜 선택이 잠겨 있어요.", style = MaterialTheme.typography.bodySmall)
            if (!availabilityEditEnabled) {
                WwmOutlineButton("날짜 변경하기", onClick = { availabilityEditEnabled = true })
            } else {
                Text("날짜 변경 중입니다. 수정 후 응답을 저장해주세요.", color = WwmIndigo, style = MaterialTheme.typography.bodySmall)
            }
        }
        AvailabilityLegend()
        AvailabilityCalendar(
            startDate = room.dateRangeStart,
            endDate = room.dateRangeEnd,
            selectedValues = state.selectedAvailability,
            summaries = state.summaries,
            onDateClick = { onCycleDate(it); onSelectDate(it) },
            enabled = canEditAvailability,
        )
        if (canEditAvailability) {
            WwmPrimaryButton("응답 저장", onSave)
        }
        SectionTitle("추천 날짜 TOP 3")
        state.recommendations.forEach { recommendation ->
            val summary = recommendation.summary
            WwmCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (canEditAvailability) onSelectDate(summary.date) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (recommendation.rank == 1) StatusPill("☆ 1순위")
                    Text("${summary.date.toKoreanDate()}", color = WwmText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("가능 ${summary.availableParticipants.size}/${summary.totalParticipants} · 애매 ${summary.maybeParticipants.size} · 점수 ${summary.score}")
                    Text(recommendation.reason, color = WwmMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        state.selectedSummary?.let { summary ->
            SectionTitle("${summary.date.toKoreanDate()} 상세")
            WwmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("추천 점수 ${summary.score}점", fontWeight = FontWeight.Bold)
                    Text("가능: ${summary.availableParticipants.namesOrNone()}")
                    Text("애매: ${summary.maybeParticipants.namesOrNone()}")
                    Text("불가능: ${summary.unavailableParticipants.namesOrNone()}")
                    Text("미응답: ${summary.unansweredParticipants.namesOrNone()}")
                    if (state.currentParticipant.isHost && canEditAvailability) {
                        WwmPrimaryButton(
                            if (room.confirmedDate == null) "이 날짜로 확정하기" else "이 날짜로 변경하기",
                            { onConfirm(summary.date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantsTab(state: MeetingRoomUiState, onLeaveRoom: () -> Unit, onDeleteRoom: () -> Unit) {
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("참여자 ${state.participants.size}명")
        WwmCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                state.participants.forEachIndexed { index, participant ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(participant.nickname)
                        Text(
                            "방장".takeIf { participant.isHost }.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (index < state.participants.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
        if (state.currentParticipant.isHost) {
            WwmOutlineButton("약속 삭제", onClick = { showDeleteConfirm = true })
        } else {
            WwmOutlineButton("약속방 나가기", onClick = { showLeaveConfirm = true })
        }
    }
    if (showLeaveConfirm) {
        ConfirmDangerDialog(
            title = "약속방에서 나갈까요?",
            text = "내 응답과 투표 정보가 이 기기에서 제거됩니다.",
            confirmText = "나가기",
            onDismiss = { showLeaveConfirm = false },
            onConfirm = {
                showLeaveConfirm = false
                onLeaveRoom()
            },
        )
    }
    if (showDeleteConfirm) {
        ConfirmDangerDialog(
            title = "약속을 삭제할까요?",
            text = "참여자들이 더 이상 이 약속방에 접근할 수 없습니다.",
            confirmText = "삭제",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDeleteRoom()
            },
        )
    }
}

@Composable
private fun ConfirmDangerDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

private fun List<com.garam.whenwheremeet.domain.model.Participant>.namesOrNone(): String =
    joinToString { it.nickname }.ifBlank { "없음" }
