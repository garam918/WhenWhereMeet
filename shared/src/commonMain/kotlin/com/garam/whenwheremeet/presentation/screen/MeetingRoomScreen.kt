package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.isMeetingConfirmed
import com.garam.whenwheremeet.presentation.component.AvailabilityCalendar
import com.garam.whenwheremeet.presentation.component.AvailabilityLegend
import com.garam.whenwheremeet.presentation.component.ConfirmedMeetingCard
import com.garam.whenwheremeet.presentation.component.PlaceRecommendationTab
import com.garam.whenwheremeet.presentation.component.SectionTitle
import com.garam.whenwheremeet.presentation.component.StatusPill
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBadge
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmDesktopBreakpoint
import com.garam.whenwheremeet.presentation.component.WwmDesktopContentMaxWidth
import com.garam.whenwheremeet.presentation.component.WwmDesktopFlowSidebar
import com.garam.whenwheremeet.presentation.component.WwmDesktopFlowTopBar
import com.garam.whenwheremeet.presentation.component.WwmFlowSection
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmInfoPanel
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmOutlineButton
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmStepProgress
import com.garam.whenwheremeet.presentation.component.WwmSurfaceSubtle
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
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String) -> Unit,
    onConfirmWithoutPlace: () -> Unit,
    onOpenDestinationRoute: (String) -> Unit,
    onOpenConfirmedRoute: () -> Unit,
    onAddToCalendar: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = state.room
    var selectedTab by remember { mutableStateOf(MeetingRoomTab.DATE) }
    val currentFlowStep = when {
        room.status.isMeetingConfirmed -> 3
        room.confirmedDate != null -> 2
        else -> 1
    }
    BoxWithConstraints(modifier.fillMaxSize().background(WwmBackground)) {
        val isDesktop = maxWidth >= WwmDesktopBreakpoint
        if (isDesktop) {
            Column(Modifier.fillMaxSize()) {
                WwmDesktopFlowTopBar(onBack = onBack, onShare = onShare)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    val selectedSection = when (selectedTab) {
                        MeetingRoomTab.DATE -> WwmFlowSection.EVENT_DETAILS
                        MeetingRoomTab.PARTICIPANTS -> WwmFlowSection.PARTICIPANTS
                        MeetingRoomTab.PLACE -> if (room.status.isMeetingConfirmed) {
                            WwmFlowSection.FINALIZE
                        } else {
                            WwmFlowSection.LOCATION_OPTIONS
                        }
                    }
                    WwmDesktopFlowSidebar(
                        selectedSection = selectedSection,
                        onSelectSection = { section ->
                            selectedTab = when (section) {
                                WwmFlowSection.EVENT_DETAILS -> MeetingRoomTab.DATE
                                WwmFlowSection.PARTICIPANTS -> MeetingRoomTab.PARTICIPANTS
                                WwmFlowSection.LOCATION_OPTIONS,
                                WwmFlowSection.FINALIZE -> MeetingRoomTab.PLACE
                            }
                        },
                    )
                    MeetingRoomContent(
                        state = state,
                        selectedTab = selectedTab,
                        currentFlowStep = currentFlowStep,
                        onSelectTab = { selectedTab = it },
                        onCycleDate = onCycleDate,
                        onSelectDate = onSelectDate,
                        onSave = onSave,
                        onConfirm = onConfirm,
                        onSearchLocations = onSearchLocations,
                        onUseCurrentLocation = onUseCurrentLocation,
                        onSaveStartLocation = onSaveStartLocation,
                        onSearchDestinationStations = onSearchDestinationStations,
                        onProposeDestinationStation = onProposeDestinationStation,
                        onVoteDestinationStation = onVoteDestinationStation,
                        onConfirmDestinationStation = onConfirmDestinationStation,
                        onConfirmWithoutPlace = onConfirmWithoutPlace,
                        onOpenDestinationRoute = onOpenDestinationRoute,
                        onOpenConfirmedRoute = onOpenConfirmedRoute,
                        onAddToCalendar = onAddToCalendar,
                        onShare = onShare,
                        onLeaveRoom = onLeaveRoom,
                        onDeleteRoom = onDeleteRoom,
                        isDesktop = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                WwmTopBar(
                    title = "언제어디",
                    leadingText = "‹",
                    onLeadingClick = onBack,
                    trailingText = "↗",
                    onTrailingClick = onShare,
                )
                MeetingRoomContent(
                    state = state,
                    selectedTab = selectedTab,
                    currentFlowStep = currentFlowStep,
                    onSelectTab = { selectedTab = it },
                    onCycleDate = onCycleDate,
                    onSelectDate = onSelectDate,
                    onSave = onSave,
                    onConfirm = onConfirm,
                    onSearchLocations = onSearchLocations,
                    onUseCurrentLocation = onUseCurrentLocation,
                    onSaveStartLocation = onSaveStartLocation,
                    onSearchDestinationStations = onSearchDestinationStations,
                    onProposeDestinationStation = onProposeDestinationStation,
                    onVoteDestinationStation = onVoteDestinationStation,
                    onConfirmDestinationStation = onConfirmDestinationStation,
                    onConfirmWithoutPlace = onConfirmWithoutPlace,
                    onOpenDestinationRoute = onOpenDestinationRoute,
                    onOpenConfirmedRoute = onOpenConfirmedRoute,
                    onAddToCalendar = onAddToCalendar,
                    onShare = onShare,
                    onLeaveRoom = onLeaveRoom,
                    onDeleteRoom = onDeleteRoom,
                    isDesktop = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MeetingRoomContent(
    state: MeetingRoomUiState,
    selectedTab: MeetingRoomTab,
    currentFlowStep: Int,
    onSelectTab: (MeetingRoomTab) -> Unit,
    onCycleDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String) -> Unit,
    onConfirmWithoutPlace: () -> Unit,
    onOpenDestinationRoute: (String) -> Unit,
    onOpenConfirmedRoute: () -> Unit,
    onAddToCalendar: () -> Unit,
    onShare: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    isDesktop: Boolean,
    modifier: Modifier = Modifier,
) {
    val room = state.room
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(if (isDesktop) 32.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isDesktop) {
            Row(
                Modifier.fillMaxWidth().widthIn(max = WwmDesktopContentMaxWidth).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MeetingRoomHeader(state)
                    WwmStepProgress(
                        labels = listOf("날짜 정하기", "출발역·후보역", "역 투표·확정"),
                        currentStep = currentFlowStep,
                    )
                    if (room.confirmedDate != null) ConfirmedMeetingCard(room, state.participants.size)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MeetingRoomTabSelector(selectedTab = selectedTab, onSelectTab = onSelectTab)
                    MeetingRoomSelectedTab(
                        state = state,
                        selectedTab = selectedTab,
                        onCycleDate = onCycleDate,
                        onSelectDate = onSelectDate,
                        onSave = onSave,
                        onConfirm = onConfirm,
                        onSearchLocations = onSearchLocations,
                        onUseCurrentLocation = onUseCurrentLocation,
                        onSaveStartLocation = onSaveStartLocation,
                        onSearchDestinationStations = onSearchDestinationStations,
                        onProposeDestinationStation = onProposeDestinationStation,
                        onVoteDestinationStation = onVoteDestinationStation,
                        onConfirmDestinationStation = onConfirmDestinationStation,
                        onConfirmWithoutPlace = onConfirmWithoutPlace,
                        onOpenDestinationRoute = onOpenDestinationRoute,
                        onOpenConfirmedRoute = onOpenConfirmedRoute,
                        onAddToCalendar = onAddToCalendar,
                        onShare = onShare,
                        onLeaveRoom = onLeaveRoom,
                        onDeleteRoom = onDeleteRoom,
                        isDesktop = true,
                    )
                }
            }
        } else {
            MeetingRoomHeader(state)
            WwmStepProgress(labels = listOf("날짜 정하기", "출발역·후보역", "역 투표·확정"), currentStep = currentFlowStep)
            if (room.confirmedDate != null) ConfirmedMeetingCard(room, state.participants.size)
            MeetingRoomTabSelector(selectedTab = selectedTab, onSelectTab = onSelectTab)
            MeetingRoomSelectedTab(
                state = state,
                selectedTab = selectedTab,
                onCycleDate = onCycleDate,
                onSelectDate = onSelectDate,
                onSave = onSave,
                onConfirm = onConfirm,
                onSearchLocations = onSearchLocations,
                onUseCurrentLocation = onUseCurrentLocation,
                onSaveStartLocation = onSaveStartLocation,
                onSearchDestinationStations = onSearchDestinationStations,
                onProposeDestinationStation = onProposeDestinationStation,
                onVoteDestinationStation = onVoteDestinationStation,
                onConfirmDestinationStation = onConfirmDestinationStation,
                onConfirmWithoutPlace = onConfirmWithoutPlace,
                onOpenDestinationRoute = onOpenDestinationRoute,
                onOpenConfirmedRoute = onOpenConfirmedRoute,
                onAddToCalendar = onAddToCalendar,
                onShare = onShare,
                onLeaveRoom = onLeaveRoom,
                onDeleteRoom = onDeleteRoom,
                isDesktop = false,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MeetingRoomTabSelector(selectedTab: MeetingRoomTab, onSelectTab: (MeetingRoomTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(WwmSurfaceSubtle, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MeetingRoomTab.entries.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                label = { Text(tab.label) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MeetingRoomSelectedTab(
    state: MeetingRoomUiState,
    selectedTab: MeetingRoomTab,
    onCycleDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String) -> Unit,
    onConfirmWithoutPlace: () -> Unit,
    onOpenDestinationRoute: (String) -> Unit,
    onOpenConfirmedRoute: () -> Unit,
    onAddToCalendar: () -> Unit,
    onShare: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    isDesktop: Boolean,
) {
    when (selectedTab) {
        MeetingRoomTab.DATE -> DateTab(state, onCycleDate, onSelectDate, onSave, onConfirm)
        MeetingRoomTab.PLACE -> PlaceRecommendationTab(
            state = state,
            onSearchLocations = onSearchLocations,
            onUseCurrentLocation = onUseCurrentLocation,
            onSaveStartLocation = onSaveStartLocation,
            onSearchDestinationStations = onSearchDestinationStations,
            onProposeDestinationStation = onProposeDestinationStation,
            onVoteDestinationStation = onVoteDestinationStation,
            onConfirmDestinationStation = onConfirmDestinationStation,
            onConfirmWithoutPlace = onConfirmWithoutPlace,
            onOpenDestinationRoute = onOpenDestinationRoute,
            onOpenConfirmedRoute = onOpenConfirmedRoute,
            onAddToCalendar = onAddToCalendar,
            onShare = onShare,
            isDesktop = isDesktop,
        )
        MeetingRoomTab.PARTICIPANTS -> ParticipantsTab(state, onLeaveRoom, onDeleteRoom)
    }
}

@Composable
private fun MeetingRoomHeader(state: MeetingRoomUiState) {
    val room = state.room
    val statusText = when (room.status) {
        MeetingStatus.PLACE_SELECTING -> "장소 조율 중"
        MeetingStatus.PLACE_CONFIRMED -> "약속 확정"
        MeetingStatus.MEETING_CONFIRMED -> "약속 확정 · 장소 미정"
        MeetingStatus.DATE_CONFIRMED -> "날짜 확정"
        else -> "날짜 조율 중"
    }
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(room.meetingType.label, color = WwmIndigo, style = MaterialTheme.typography.labelLarge)
                    Text(room.title, color = WwmText, style = MaterialTheme.typography.headlineSmall)
                }
                StatusPill(statusText)
            }
            room.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = WwmMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                Modifier.fillMaxWidth().background(WwmSoftIndigo, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("초대 코드", color = WwmMuted, style = MaterialTheme.typography.labelSmall)
                    Text(room.id, color = WwmText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("참여자 ${state.participants.size}/${room.maxParticipants}명", color = WwmIndigo, style = MaterialTheme.typography.labelLarge)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.participants.take(4).forEach { participant ->
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(30.dp).background(WwmSoftIndigo, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(participant.nickname.take(1), color = WwmIndigo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.participants.size > 4) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(30.dp).background(WwmSurfaceSubtle, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+${state.participants.size - 4}", color = WwmMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    "${room.dateRangeStart} ~ ${room.dateRangeEnd}",
                    color = WwmMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
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
        SectionTitle("참석 가능한 날짜를 선택해주세요")
        if (room.confirmedDate == null) {
            WwmInfoPanel(
                title = "날짜를 눌러 응답하세요",
                description = "미선택 → 가능 → 애매 → 불가능 순서로 바뀌어요.",
                icon = "✓",
            )
        } else {
            WwmInfoPanel("날짜가 확정됐어요", "변경이 필요하면 아래 버튼을 눌러 응답을 수정할 수 있어요.", icon = "✓")
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
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(
                                Modifier.size(34.dp).background(WwmSoftIndigo, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(participant.nickname.take(1), color = WwmIndigo, fontWeight = FontWeight.Bold)
                            }
                            Text(participant.nickname, color = WwmText)
                        }
                        when {
                            participant.isHost -> WwmBadge("방장")
                            participant.isInvited -> WwmBadge("초대됨")
                        }
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
