package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.garam.whenwheremeet.domain.usecase.ConfirmationParticipation
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
    onEditAvailability: () -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit,
    onShare: () -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String, Boolean) -> Unit,
    onConfirmWithoutPlace: (Boolean) -> Unit,
    onOpenDestinationRoute: (String) -> Unit,
    onOpenConfirmedRoute: () -> Unit,
    onAddToCalendar: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = state.room
    var selectedTab by remember { mutableStateOf(MeetingRoomTab.DATE) }
    var showRoomInfo by remember { mutableStateOf(false) }
    val currentFlowStep = when {
        room.status.isMeetingConfirmed -> 3
        room.confirmedDate != null -> 2
        else -> 1
    }
    BoxWithConstraints(modifier.fillMaxSize().background(WwmBackground)) {
        val isDesktop = maxWidth >= WwmDesktopBreakpoint
        if (isDesktop) {
            Column(Modifier.fillMaxSize()) {
                WwmDesktopFlowTopBar(
                    onBack = onBack,
                    trailingContent = {
                        MeetingRoomMoreMenuButton(
                            onShowRoomInfo = { showRoomInfo = true },
                            onShare = onShare,
                        )
                    },
                )
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
                        onEditAvailability = onEditAvailability,
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
                    trailingContent = {
                        MeetingRoomMoreMenuButton(
                            onShowRoomInfo = { showRoomInfo = true },
                            onShare = onShare,
                        )
                    },
                )
                MeetingRoomContent(
                    state = state,
                    selectedTab = selectedTab,
                    currentFlowStep = currentFlowStep,
                    onSelectTab = { selectedTab = it },
                    onCycleDate = onCycleDate,
                    onSelectDate = onSelectDate,
                    onEditAvailability = onEditAvailability,
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
        if (showRoomInfo) {
            MeetingRoomInfoDialog(
                state = state,
                onDismiss = { showRoomInfo = false },
            )
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
    onEditAvailability: () -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String, Boolean) -> Unit,
    onConfirmWithoutPlace: (Boolean) -> Unit,
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
                    MeetingDateSpotlight(state = state, onSelectDate = onSelectDate)
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
                        onEditAvailability = onEditAvailability,
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
            MeetingDateSpotlight(state = state, onSelectDate = onSelectDate)
            WwmStepProgress(labels = listOf("날짜 정하기", "출발역·후보역", "역 투표·확정"), currentStep = currentFlowStep)
            if (room.confirmedDate != null) ConfirmedMeetingCard(room, state.participants.size)
            MeetingRoomTabSelector(selectedTab = selectedTab, onSelectTab = onSelectTab)
            MeetingRoomSelectedTab(
                state = state,
                selectedTab = selectedTab,
                onCycleDate = onCycleDate,
                onSelectDate = onSelectDate,
                onEditAvailability = onEditAvailability,
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
    onEditAvailability: () -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit,
    onSearchLocations: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSaveStartLocation: (LocationSearchResult) -> Unit,
    onSearchDestinationStations: (String) -> Unit,
    onProposeDestinationStation: (LocationSearchResult) -> Unit,
    onVoteDestinationStation: (String) -> Unit,
    onConfirmDestinationStation: (String, Boolean) -> Unit,
    onConfirmWithoutPlace: (Boolean) -> Unit,
    onOpenDestinationRoute: (String) -> Unit,
    onOpenConfirmedRoute: () -> Unit,
    onAddToCalendar: () -> Unit,
    onShare: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
    isDesktop: Boolean,
) {
    when (selectedTab) {
        MeetingRoomTab.DATE -> DateTab(
            state = state,
            onCycleDate = onCycleDate,
            onSelectDate = onSelectDate,
            onEditAvailability = onEditAvailability,
            onSave = onSave,
            onConfirm = onConfirm,
        )
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
private fun MeetingRoomMoreMenuButton(
    onShowRoomInfo: () -> Unit,
    onShare: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "더보기" },
        ) {
            Text(
                text = "⋮",
                color = WwmText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("약속방 정보") },
                leadingIcon = { Text("ⓘ", color = WwmIndigo) },
                onClick = {
                    expanded = false
                    onShowRoomInfo()
                },
            )
            DropdownMenuItem(
                text = { Text("공유하기") },
                leadingIcon = { Text("↗", color = WwmIndigo) },
                onClick = {
                    expanded = false
                    onShare()
                },
            )
        }
    }
}

@Composable
private fun MeetingRoomInfoDialog(
    state: MeetingRoomUiState,
    onDismiss: () -> Unit,
) {
    val room = state.room
    val statusText = when (room.status) {
        MeetingStatus.PLACE_SELECTING -> "장소 조율 중"
        MeetingStatus.PLACE_CONFIRMED -> "약속 확정"
        MeetingStatus.MEETING_CONFIRMED -> "약속 확정 · 장소 미정"
        MeetingStatus.DATE_CONFIRMED -> "날짜 확정"
        else -> "날짜 조율 중"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("약속방 정보") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Box(
                                Modifier.size(30.dp).background(WwmSoftIndigo, androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(participant.nickname.take(1), color = WwmIndigo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (state.participants.size > 4) {
                            Box(
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
    )
}

@Composable
private fun MeetingDateSpotlight(
    state: MeetingRoomUiState,
    onSelectDate: (LocalDate) -> Unit,
) {
    val highlightedDate = state.selectedSummary?.date ?: state.room.confirmedDate
    WwmCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (highlightedDate != null && state.room.confirmedDate == highlightedDate) {
                        Text(
                            "확정한 날짜",
                            color = WwmMuted,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(
                        highlightedDate?.toKoreanDate() ?: "아직 선택한 날짜가 없어요",
                        color = WwmText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                highlightedDate?.let { StatusPill(if (it == state.room.confirmedDate) "확정" else "선택") }
            }
            Text("추천 날짜 TOP 3", color = WwmIndigo, style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recommendations.take(3).forEach { recommendation ->
                    val summary = recommendation.summary
                    Column(
                        Modifier
                            .weight(1f)
                            .background(
                                if (highlightedDate == summary.date) WwmSoftIndigo else WwmSurfaceSubtle,
                                androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelectDate(summary.date) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("${recommendation.rank}순위", color = WwmIndigo, style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${summary.date.monthNumber}/${summary.date.dayOfMonth}",
                            color = WwmText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "가능 ${summary.availableParticipants.size}명",
                            color = WwmMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTab(
    state: MeetingRoomUiState,
    onCycleDate: (LocalDate) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onEditAvailability: () -> Unit,
    onSave: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit,
) {
    val room = state.room
    var pendingLowParticipationDate by remember { mutableStateOf<LocalDate?>(null) }
    val canEditAvailability = state.isAvailabilityEditing
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("참석 가능한 날짜를 선택해주세요")
        if (canEditAvailability) {
            WwmInfoPanel(
                title = "날짜를 눌러 응답하세요",
                description = "미선택 → 가능 → 애매 → 불가능 순서로 바뀌어요. 저장하면 응답이 잠겨요.",
                icon = "✓",
            )
        } else {
            WwmInfoPanel("응답을 저장했어요", "날짜를 바꾸려면 응답 변경 버튼을 먼저 눌러주세요.", icon = "✓")
            WwmOutlineButton("응답 변경", onClick = onEditAvailability)
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
        state.selectedSummary?.let { summary ->
            SectionTitle("${summary.date.toKoreanDate()} 상세")
            WwmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("추천 점수 ${summary.score}점", fontWeight = FontWeight.Bold)
                    Text("가능: ${summary.availableParticipants.namesOrNone()}")
                    Text("애매: ${summary.maybeParticipants.namesOrNone()}")
                    Text("불가능: ${summary.unavailableParticipants.namesOrNone()}")
                    Text("미응답: ${summary.unansweredParticipants.namesOrNone()}")
                    if (state.currentParticipant.isHost) {
                        WwmPrimaryButton(
                            if (room.confirmedDate == null) "이 날짜로 확정하기" else "이 날짜로 변경하기",
                            {
                                if (state.dateConfirmationParticipation?.meetsThreshold == true) {
                                    onConfirm(summary.date, false)
                                } else {
                                    pendingLowParticipationDate = summary.date
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    pendingLowParticipationDate?.let { date ->
        LowParticipationConfirmDialog(
            participation = state.dateConfirmationParticipation,
            subject = "날짜",
            onDismiss = { pendingLowParticipationDate = null },
            onConfirm = {
                pendingLowParticipationDate = null
                onConfirm(date, true)
            },
        )
    }
}

@Composable
private fun LowParticipationConfirmDialog(
    participation: ConfirmationParticipation?,
    subject: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val voterCount = participation?.voterCount ?: 0
    val participantCount = participation?.participantCount ?: 0
    val requiredVoterCount = participation?.requiredVoterCount ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("투표가 아직 충분하지 않아요") },
        text = {
            Text(
                "현재 $voterCount/${participantCount}명이 참여했어요. " +
                    "바로 확정하려면 ${requiredVoterCount}명(70%) 이상이 필요합니다. 그래도 ${subject}를 확정할까요?",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("확정", color = WwmIndigo) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
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
