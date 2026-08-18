package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.DestinationStationOption
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.presentation.state.MeetingRoomUiState
import com.garam.whenwheremeet.presentation.state.toKoreanDate

@Composable
fun PlaceRecommendationTab(
    state: MeetingRoomUiState,
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
    isDesktop: Boolean = false,
) {
    val room = state.room
    if (room.confirmedDate == null) {
        WwmEmptyState(text = "날짜 확정 후 이용할 수 있어요.\n먼저 모두가 가능한 날짜를 확정해주세요.")
        return
    }
    if (room.status == MeetingStatus.MEETING_CONFIRMED) {
        FinalMeetingWithoutPlaceCard(
            dateText = room.confirmedDate.toKoreanDate(),
            participantCount = state.participants.size,
            onAddToCalendar = onAddToCalendar,
            onShare = onShare,
        )
        return
    }
    room.confirmedPlace?.let { place ->
        FinalStationCard(
            dateText = room.confirmedDate.toKoreanDate(),
            place = place,
            participantCount = state.participants.size,
            onAddToCalendar = onAddToCalendar,
            onOpenRoute = onOpenConfirmedRoute,
            onShare = onShare,
        )
        return
    }

    var showOriginSearch by remember { mutableStateOf(false) }
    var showDestinationSearch by remember { mutableStateOf(false) }
    var showConfirmWithoutPlace by remember { mutableStateOf(false) }
    val currentParticipantId = state.currentParticipant.id
    val currentProposal = state.destinationStationProposals.firstOrNull {
        it.participantId == currentParticipantId
    }
    val currentVote = state.destinationStationVotes.firstOrNull {
        it.participantId == currentParticipantId
    }
    val step = when {
        state.currentStartLocation == null -> 1
        currentProposal == null -> 2
        else -> 3
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WwmStepProgress(
            labels = listOf("출발역 입력", "후보역 제안", "투표·확정"),
            currentStep = step,
        )
        OriginStationCard(
            state = state,
            onOpenSearch = { showOriginSearch = true },
            onUseCurrentLocation = onUseCurrentLocation,
        )
        if (state.currentStartLocation != null) {
            ProposalStationCard(
                currentStationName = currentProposal?.station?.name,
                onOpenSearch = { showDestinationSearch = true },
            )
            StationVotingSection(
                state = state,
                selectedStationId = currentVote?.stationId,
                onVote = onVoteDestinationStation,
                onOpenRoute = onOpenDestinationRoute,
                onConfirm = onConfirmDestinationStation,
            )
        } else {
            WwmEmptyState(
                text = "내 출발역을 입력하면\n만나고 싶은 역을 제안할 수 있어요.",
                icon = "1",
            )
        }
        if (state.currentParticipant.isHost) {
            WwmCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("장소는 나중에 정해도 돼요", fontWeight = FontWeight.Bold)
                    Text(
                        "날짜만으로 약속을 확정하면 참여자에게 장소 미정으로 표시돼요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WwmMuted,
                    )
                    WwmOutlineButton(
                        text = "장소 없이 약속 확정",
                        onClick = { showConfirmWithoutPlace = true },
                    )
                }
            }
        }
    }

    if (showOriginSearch) {
        StationSearchDialog(
            title = "내 출발역 검색",
            helperText = "정확한 주소나 좌표는 다른 참여자에게 공개되지 않아요.",
            results = state.locationSearchResults,
            onSearch = onSearchLocations,
            onSelect = {
                onSaveStartLocation(it)
                showOriginSearch = false
            },
            onDismiss = { showOriginSearch = false },
        )
    }
    if (showDestinationSearch) {
        StationSearchDialog(
            title = if (currentProposal == null) "만나고 싶은 역 제안" else "후보역 변경",
            helperText = "각자 하나의 역을 제안할 수 있고 같은 역은 하나로 합쳐져요.",
            results = state.destinationStationSearchResults,
            onSearch = onSearchDestinationStations,
            onSelect = {
                onProposeDestinationStation(it)
                showDestinationSearch = false
            },
            onDismiss = { showDestinationSearch = false },
        )
    }
    if (showConfirmWithoutPlace) {
        AlertDialog(
            onDismissRequest = { showConfirmWithoutPlace = false },
            title = { Text("장소 없이 확정할까요?") },
            text = { Text("확정 후에도 날짜를 변경하면 다시 조율 상태로 돌아가요. 장소는 미정으로 공유됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmWithoutPlace = false
                        onConfirmWithoutPlace()
                    },
                ) { Text("약속 확정", color = WwmIndigo) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmWithoutPlace = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun OriginStationCard(
    state: MeetingRoomUiState,
    onOpenSearch: () -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    val locationByParticipant = state.startLocations.associateBy { it.participantId }
    WwmCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("내 출발역", style = MaterialTheme.typography.labelLarge, color = WwmMuted)
                    Text(
                        state.currentStartLocation?.label ?: "아직 입력하지 않았어요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                WwmBadge(
                    text = if (state.currentStartLocation == null) "1단계" else "입력 완료",
                    containerColor = if (state.currentStartLocation == null) WwmSoftIndigo else WwmMintSurface,
                    contentColor = if (state.currentStartLocation == null) WwmIndigo else WwmMintText,
                )
            }
            Text(
                "출발역 이름만 참여자에게 보이고 정확한 좌표는 경로를 열 때만 사용해요.",
                style = MaterialTheme.typography.bodySmall,
                color = WwmMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WwmOutlineButton(
                    text = if (state.currentStartLocation == null) "출발역 검색" else "출발역 수정",
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f),
                )
                WwmOutlineButton(
                    text = "현재 위치",
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.participants.forEach { participant ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(32.dp).background(WwmSoftIndigo, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(participant.nickname.take(1), color = WwmIndigo, fontWeight = FontWeight.Bold)
                        }
                        Text(participant.nickname, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        WwmBadge(
                            text = locationByParticipant[participant.id]?.label ?: "미입력",
                            containerColor = if (locationByParticipant[participant.id] == null) WwmSurfaceSubtle else WwmMintSurface,
                            contentColor = if (locationByParticipant[participant.id] == null) WwmMuted else WwmMintText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalStationCard(currentStationName: String?, onOpenSearch: () -> Unit) {
    WwmCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("내가 제안한 후보역", style = MaterialTheme.typography.labelLarge, color = WwmMuted)
                    Text(
                        currentStationName ?: "만나고 싶은 역을 골라주세요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                WwmBadge(if (currentStationName == null) "2단계" else "제안 완료")
            }
            Text(
                "역 목록을 둘러보거나 이름으로 검색할 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = WwmMuted,
            )
            WwmPrimaryButton(
                text = if (currentStationName == null) "후보역 선택하기" else "후보역 변경하기",
                onClick = onOpenSearch,
            )
        }
    }
}

@Composable
private fun StationVotingSection(
    state: MeetingRoomUiState,
    selectedStationId: String?,
    onVote: (String) -> Unit,
    onOpenRoute: (String) -> Unit,
    onConfirm: (String) -> Unit,
) {
    val options = state.destinationStationOptions
    val totalVoters = state.destinationStationVotes.map { it.participantId }.distinct().size
    val topVoteCount = options.maxOfOrNull { it.voteCount } ?: 0
    val leadingCount = options.count { topVoteCount > 0 && it.voteCount == topVoteCount }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("후보역 투표", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("한 곳만 선택하며 언제든 바꿀 수 있어요.", style = MaterialTheme.typography.bodySmall, color = WwmMuted)
            }
            WwmBadge("$totalVoters/${state.participants.size}명 투표")
        }
        if (options.isEmpty()) {
            WwmEmptyState(text = "아직 제안된 후보역이 없어요.\n첫 번째 후보역을 제안해주세요.", icon = "2")
        } else {
            options.forEach { option ->
                DestinationStationCard(
                    option = option,
                    proposerNames = option.proposerParticipantIds.mapNotNull { participantId ->
                        state.participants.firstOrNull { it.id == participantId }?.nickname
                    },
                    totalParticipants = state.participants.size,
                    selected = selectedStationId == option.station.id,
                    leading = topVoteCount > 0 && option.voteCount == topVoteCount,
                    tied = leadingCount > 1,
                    onVote = { onVote(option.station.id) },
                    onOpenRoute = { onOpenRoute(option.station.id) },
                )
            }
        }
        if (state.currentParticipant.isHost) {
            WwmPrimaryButton(
                text = "이 역으로 확정",
                onClick = { selectedStationId?.let(onConfirm) },
                enabled = selectedStationId != null,
            )
            if (selectedStationId == null) {
                Text(
                    "방장도 후보역을 선택한 뒤 확정할 수 있어요.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WwmMuted,
                )
            }
        }
    }
}

@Composable
private fun DestinationStationCard(
    option: DestinationStationOption,
    proposerNames: List<String>,
    totalParticipants: Int,
    selected: Boolean,
    leading: Boolean,
    tied: Boolean,
    onVote: () -> Unit,
    onOpenRoute: () -> Unit,
) {
    val borderColor = when {
        selected -> WwmIndigo
        leading -> WwmIndigo.copy(alpha = 0.7f)
        else -> WwmBorder
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) WwmSoftIndigo.copy(alpha = 0.55f) else WwmSurface, RoundedCornerShape(16.dp))
            .border(if (selected || leading) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onVote)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(option.station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (leading) WwmBadge(if (tied) "공동 최다 득표" else "최다 득표")
                }
                Text(
                    "${proposerNames.joinToString(", ").ifBlank { "참여자" }} 제안 · ${option.station.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WwmMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${option.voteCount}/${totalParticipants}명", color = WwmIndigo, fontWeight = FontWeight.Bold)
                RadioButton(selected = selected, onClick = onVote)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            option.station.lines.take(4).forEach { line ->
                WwmBadge(text = line, containerColor = WwmSurfaceSubtle, contentColor = WwmText)
            }
        }
        WwmOutlineButton(
            text = "카카오맵에서 경로·시간 보기",
            onClick = onOpenRoute,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StationSearchDialog(
    title: String,
    helperText: String,
    results: List<LocationSearchResult>,
    onSearch: (String) -> Unit,
    onSelect: (LocationSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onSearch("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(helperText, style = MaterialTheme.typography.bodySmall, color = WwmMuted)
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    label = { Text("역 이름 검색") },
                    placeholder = { Text("예: 강남역, 홍대입구역") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (query.isBlank()) "추천 역" else "검색 결과",
                    style = MaterialTheme.typography.labelLarge,
                    color = WwmMuted,
                )
                if (results.isEmpty()) {
                    Text("일치하는 역이 없어요.", style = MaterialTheme.typography.bodyMedium, color = WwmMuted)
                } else {
                    results.take(10).forEach { result ->
                        WwmCard(onClick = { onSelect(result) }) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(result.label, fontWeight = FontWeight.Bold)
                                result.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WwmMuted) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun FinalStationCard(
    dateText: String,
    place: PlaceCandidate,
    participantCount: Int,
    onAddToCalendar: () -> Unit,
    onOpenRoute: () -> Unit,
    onShare: () -> Unit,
) {
    WwmCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).background(WwmMintSurface, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = WwmMintText, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("약속 장소가 확정됐어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateText, style = MaterialTheme.typography.bodySmall, color = WwmMuted)
                }
            }
            Text(place.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                listOfNotNull(place.openingHoursSummary, place.address).joinToString(" · ").ifBlank { "역 정보" },
                style = MaterialTheme.typography.bodySmall,
                color = WwmMuted,
            )
            WwmBadge("참여자 ${participantCount}명")
            WwmPrimaryButton(text = "카카오맵에서 내 경로 보기", onClick = onOpenRoute)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WwmOutlineButton(text = "캘린더 추가", onClick = onAddToCalendar, modifier = Modifier.weight(1f))
                WwmOutlineButton(text = "공유", onClick = onShare, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FinalMeetingWithoutPlaceCard(
    dateText: String,
    participantCount: Int,
    onAddToCalendar: () -> Unit,
    onShare: () -> Unit,
) {
    WwmCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).background(WwmMintSurface, CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = WwmMintText, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("약속이 확정됐어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateText, style = MaterialTheme.typography.bodySmall, color = WwmMuted)
                }
            }
            Text("장소 미정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("장소를 정하지 않고 날짜와 참여자만으로 확정한 약속이에요.", style = MaterialTheme.typography.bodySmall, color = WwmMuted)
            WwmBadge("참여자 ${participantCount}명")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WwmOutlineButton(text = "캘린더 추가", onClick = onAddToCalendar, modifier = Modifier.weight(1f))
                WwmOutlineButton(text = "공유", onClick = onShare, modifier = Modifier.weight(1f))
            }
        }
    }
}
