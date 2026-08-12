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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.usecase.AggregatePlaceVotesUseCase
import com.garam.whenwheremeet.presentation.state.MeetingRoomUiState
import com.garam.whenwheremeet.presentation.state.toKoreanDate
import kotlin.math.sqrt

@Composable
fun PlaceRecommendationTab(
    state: MeetingRoomUiState,
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
    onShare: () -> Unit,
    isDesktop: Boolean = false,
) {
    val room = state.room
    if (room.confirmedDate == null) {
        WwmEmptyState(text = "날짜 확정 후 이용할 수 있어요.\n먼저 모두가 가능한 날짜를 확정해주세요.")
        return
    }
    room.confirmedPlace?.let { place ->
        FinalMeetingCard(
            dateText = room.confirmedDate.toKoreanDate(),
            place = place,
            participantCount = state.participants.size,
            onOpenMap = { onOpenMap(place) },
            onShare = onShare,
        )
        return
    }

    var showLocationDialog by remember { mutableStateOf(false) }
    var expandedAreaId by remember { mutableStateOf<String?>(null) }
    val allLocationsReady = state.participants.isNotEmpty() &&
        state.startLocations.map { it.participantId }.toSet().size >= state.participants.size
    val selectedArea = state.areaRecommendations.firstOrNull {
        it.candidate.id == room.selectedAreaCandidateId
    }

    if (isDesktop) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("내 출발 정보")
                StartLocationSection(
                    state = state,
                    onUseCurrentLocation = onUseCurrentLocation,
                    onSaveTransportMode = onSaveTransportMode,
                    onOpenLocationDialog = { showLocationDialog = true },
                )
                WwmInfoPanel(
                    title = "내 위치는 안전하게 보호돼요",
                    description = "정확한 위치는 추천 계산에만 사용하고 다른 참여자에게 노출하지 않아요.",
                    icon = "🔒",
                )
                SectionTitle("입력 현황")
                StartLocationStatus(state)
            }
            PlaceDecisionContent(
                state = state,
                allLocationsReady = allLocationsReady,
                selectedArea = selectedArea,
                expandedAreaId = expandedAreaId,
                onExpandedAreaChange = { expandedAreaId = it },
                onCalculateAreas = onCalculateAreas,
                onSelectArea = onSelectArea,
                onSearchPlaces = onSearchPlaces,
                onVotePlace = onVotePlace,
                onConfirmPlace = onConfirmPlace,
                onOpenMap = onOpenMap,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("내 출발 정보")
            StartLocationSection(
                state = state,
                onUseCurrentLocation = onUseCurrentLocation,
                onSaveTransportMode = onSaveTransportMode,
                onOpenLocationDialog = { showLocationDialog = true },
            )
            WwmInfoPanel(
                title = "내 위치는 안전하게 보호돼요",
                description = "정확한 좌표와 주소는 추천 계산에만 사용하고, 다른 참여자에게는 출발 지역 이름과 예상 시간만 보여줘요.",
                icon = "🔒",
            )
            SectionTitle("입력 현황")
            StartLocationStatus(state)
            PlaceDecisionContent(
                state = state,
                allLocationsReady = allLocationsReady,
                selectedArea = selectedArea,
                expandedAreaId = expandedAreaId,
                onExpandedAreaChange = { expandedAreaId = it },
                onCalculateAreas = onCalculateAreas,
                onSelectArea = onSelectArea,
                onSearchPlaces = onSearchPlaces,
                onVotePlace = onVotePlace,
                onConfirmPlace = onConfirmPlace,
                onOpenMap = onOpenMap,
            )
        }
    }

    if (showLocationDialog) {
        LocationSearchDialog(
            results = state.locationSearchResults,
            onSearch = onSearchLocations,
            onSelect = {
                onSaveStartLocation(it)
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false },
        )
    }
}

@Composable
private fun PlaceDecisionContent(
    state: MeetingRoomUiState,
    allLocationsReady: Boolean,
    selectedArea: AreaRecommendation?,
    expandedAreaId: String?,
    onExpandedAreaChange: (String?) -> Unit,
    onCalculateAreas: () -> Unit,
    onSelectArea: (String) -> Unit,
    onSearchPlaces: () -> Unit,
    onVotePlace: (String, PlaceVoteType) -> Unit,
    onConfirmPlace: (PlaceCandidate) -> Unit,
    onOpenMap: (PlaceCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = state.room
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            !allLocationsReady -> WwmInfoPanel(
                title = "모든 참여자의 입력을 기다리는 중",
                description = "출발 정보가 모두 모이면 공정한 중간 지역을 추천해드려요.",
                icon = "⏳",
                accentColor = WwmOrange,
            )

            state.areaRecommendations.isEmpty() -> {
                WwmInfoPanel(
                    title = "이제 중간 지역을 찾을 수 있어요",
                    description = "평균 이동 시간과 가장 오래 걸리는 사람의 시간을 함께 비교해요.",
                    icon = "⚖️",
                )
                WwmPrimaryButton(
                    text = if (state.isCalculatingAreas) "공정한 지역을 계산하는 중..." else "공정한 중간 지역 찾기",
                    onClick = onCalculateAreas,
                    enabled = !state.isCalculatingAreas,
                )
            }

            else -> {
                SectionTitle("공정한 중간 지역 TOP 3")
                state.areaRecommendations.take(3).forEachIndexed { index, recommendation ->
                    AreaRecommendationCard(
                        rank = index + 1,
                        recommendation = recommendation,
                        expanded = expandedAreaId == recommendation.candidate.id,
                        selected = room.selectedAreaCandidateId == recommendation.candidate.id,
                        isHost = state.currentParticipant.isHost,
                        onToggle = {
                            onExpandedAreaChange(if (expandedAreaId == recommendation.candidate.id) null else recommendation.candidate.id)
                        },
                        onSelect = { onSelectArea(recommendation.candidate.id) },
                    )
                }
                if (selectedArea == null) {
                    WwmInfoPanel(
                        title = if (state.currentParticipant.isHost) "모임 지역을 선택해주세요" else "방장이 모임 지역을 고르는 중이에요",
                        description = if (state.currentParticipant.isHost) {
                            "후보를 펼쳐 참여자별 이동 시간을 확인한 뒤 하나를 선택해주세요."
                        } else {
                            "선택이 끝나면 정확한 장소 후보에 투표할 수 있어요."
                        },
                        icon = "📍",
                    )
                } else {
                    SelectedAreaPanel(selectedArea)
                    SectionTitle("장소 투표")
                    if (state.currentParticipant.isHost) WwmBadge("방장만 최종 장소를 확정할 수 있어요")
                    if (state.placeCandidates.isEmpty()) {
                        WwmPrimaryButton(
                            text = if (state.isSearchingPlaces) "장소 후보를 찾는 중..." else "장소 후보 추천받기",
                            onClick = onSearchPlaces,
                            enabled = !state.isSearchingPlaces,
                        )
                    } else {
                        state.placeCandidates.take(5).forEachIndexed { index, scoredPlace ->
                            PlaceCandidateCard(
                                rank = index + 1,
                                scoredPlace = scoredPlace,
                                state = state,
                                onVote = { onVotePlace(scoredPlace.place.id, it) },
                                onOpenMap = { onOpenMap(scoredPlace.place) },
                                onConfirm = { onConfirmPlace(scoredPlace.place) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartLocationSection(
    state: MeetingRoomUiState,
    onUseCurrentLocation: () -> Unit,
    onSaveTransportMode: (TransportMode) -> Unit,
    onOpenLocationDialog: () -> Unit,
) {
    WwmCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = state.currentStartLocation?.label ?: "출발 위치를 입력해주세요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "가까운 역이나 주요 장소를 기준으로 입력하면 충분해요.",
                style = MaterialTheme.typography.bodySmall,
                color = WwmMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WwmOutlineButton(
                    text = if (state.currentStartLocation == null) "출발지 검색" else "출발지 수정",
                    onClick = onOpenLocationDialog,
                    modifier = Modifier.weight(1f),
                )
                WwmOutlineButton(
                    text = "현재 위치",
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("이동 수단", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransportMode.entries.filter { it != TransportMode.UNKNOWN }.forEach { mode ->
                    FilterChip(
                        selected = state.currentTransportMode == mode,
                        onClick = { onSaveTransportMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StartLocationStatus(state: MeetingRoomUiState) {
    val locationByParticipant = state.startLocations.associateBy { it.participantId }
    WwmCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.participants.forEachIndexed { index, participant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).background(WwmSoftIndigo, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(participant.nickname.take(1), color = WwmIndigo, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(participant.nickname, fontWeight = FontWeight.SemiBold)
                        if (participant.isHost) Text("방장", style = MaterialTheme.typography.labelSmall, color = WwmMuted)
                    }
                    val location = locationByParticipant[participant.id]
                    WwmBadge(
                        text = location?.label ?: "미입력",
                        containerColor = if (location == null) WwmSurfaceSubtle else Color(0xFFE9F8F2),
                        contentColor = if (location == null) WwmMuted else WwmMintText,
                    )
                }
                if (index != state.participants.lastIndex) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AreaRecommendationCard(
    rank: Int,
    recommendation: AreaRecommendation,
    expanded: Boolean,
    selected: Boolean,
    isHost: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val difference = recommendation.maxTravelMinutes - recommendation.minTravelMinutes
    val deviation = sqrt(recommendation.travelTimeVariance)
    val differenceLabel = when {
        deviation < 8 -> "매우 공평"
        deviation < 15 -> "균형적"
        else -> "차이 있음"
    }
    val containerColor = when {
        selected -> Color(0xFFE9F8F2)
        rank == 1 -> WwmSoftIndigo
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .border(
                width = if (selected || rank == 1) 1.5.dp else 1.dp,
                color = if (selected) WwmMint else if (rank == 1) WwmIndigo else WwmBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(30.dp).background(if (rank == 1) WwmIndigo else WwmSurfaceSubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", color = if (rank == 1) Color.White else WwmText, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(recommendation.candidate.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(recommendation.candidate.tags.take(2).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = WwmMuted)
            }
            if (selected) WwmBadge("선택됨", containerColor = WwmMint, contentColor = Color.White)
            else if (rank == 1) WwmBadge("가장 공평")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AreaMetric("평균", "${recommendation.averageTravelMinutes}분", Modifier.weight(1f))
            AreaMetric("최대", "${recommendation.maxTravelMinutes}분", Modifier.weight(1f))
            AreaMetric("격차", "$differenceLabel · ${difference}분", Modifier.weight(1.35f))
        }
        Text(recommendation.recommendationReason, style = MaterialTheme.typography.bodySmall, color = WwmMuted)

        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recommendation.participantTravelTimes.forEach { travelTime ->
                    TravelTimeRow(
                        nickname = travelTime.participantNickname,
                        minutes = travelTime.travelMinutes,
                        maxMinutes = recommendation.maxTravelMinutes.coerceAtLeast(1),
                    )
                }
            }
            if (isHost && !selected) {
                WwmPrimaryButton(text = "이 지역으로 선택", onClick = onSelect)
            }
        } else {
            Text("참여자별 예상 시간 보기", style = MaterialTheme.typography.labelMedium, color = WwmIndigo)
        }
    }
}

@Composable
private fun AreaMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = WwmMuted)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TravelTimeRow(nickname: String, minutes: Int, maxMinutes: Int) {
    val fraction = (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.08f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(nickname, modifier = Modifier.weight(0.75f), style = MaterialTheme.typography.bodySmall)
        Box(
            modifier = Modifier.weight(1.5f).height(7.dp).background(WwmSurfaceSubtle, CircleShape),
        ) {
            Box(Modifier.fillMaxWidth(fraction).height(7.dp).background(WwmIndigo, CircleShape))
        }
        Text("약 ${minutes}분", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SelectedAreaPanel(recommendation: AreaRecommendation) {
    WwmInfoPanel(
        title = "모임 지역 · ${recommendation.candidate.displayName}",
        description = "평균 ${recommendation.averageTravelMinutes}분, 가장 오래 걸려도 ${recommendation.maxTravelMinutes}분이에요.",
        icon = "✓",
        accentColor = WwmMint,
    )
}

@Composable
private fun PlaceCandidateCard(
    rank: Int,
    scoredPlace: ScoredPlaceCandidate,
    state: MeetingRoomUiState,
    onVote: (PlaceVoteType) -> Unit,
    onOpenMap: () -> Unit,
    onConfirm: () -> Unit,
) {
    val place = scoredPlace.place
    val summary = remember(place.id, state.placeVotes) {
        AggregatePlaceVotesUseCase()(place.id, state.placeVotes)
    }
    val myVote = state.placeVotes.firstOrNull {
        it.placeId == place.id && it.participantId == state.currentParticipant.id
    }?.voteType

    WwmCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).background(if (rank == 1) WwmIndigo else WwmSurfaceSubtle, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$rank", color = if (rank == 1) Color.White else WwmText, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(place.category.label, style = MaterialTheme.typography.bodySmall, color = WwmMuted)
                }
                if (rank == 1) WwmBadge("추천")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                place.rating?.let { Text("★ $it", style = MaterialTheme.typography.labelMedium, color = WwmOrange) }
                place.reviewCount?.let { Text("리뷰 ${it}개", style = MaterialTheme.typography.labelMedium, color = WwmMuted) }
                place.openingHoursSummary?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = WwmMintText) }
            }
            Text(place.roadAddress ?: place.address ?: "주소 정보 없음", style = MaterialTheme.typography.bodySmall, color = WwmMuted)
            scoredPlace.reasons.take(2).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlaceVoteType.entries.forEach { voteType ->
                    FilterChip(
                        selected = myVote == voteType,
                        onClick = { onVote(voteType) },
                        label = { Text(voteType.label) },
                    )
                }
            }
            Text(
                text = "좋아요 ${summary.likeCount} · 괜찮아요 ${summary.neutralCount} · 별로예요 ${summary.dislikeCount}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WwmOutlineButton(
                    text = "지도 보기",
                    onClick = onOpenMap,
                    enabled = place.mapUrl != null,
                    modifier = Modifier.weight(1f),
                )
                if (state.currentParticipant.isHost) {
                    WwmPrimaryButton(
                        text = "이 장소로 확정",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalMeetingCard(
    dateText: String,
    place: PlaceCandidate,
    participantCount: Int,
    onOpenMap: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WwmSoftIndigo, RoundedCornerShape(20.dp))
            .border(1.dp, WwmIndigo.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).background(WwmIndigo, CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text("약속이 확정됐어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(dateText, style = MaterialTheme.typography.titleMedium)
        Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(place.roadAddress ?: place.address ?: "주소 정보 없음", style = MaterialTheme.typography.bodySmall, color = WwmMuted)
        WwmBadge("참여자 ${participantCount}명")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WwmOutlineButton(
                text = "지도 보기",
                onClick = onOpenMap,
                enabled = place.mapUrl != null,
                modifier = Modifier.weight(1f),
            )
            WwmPrimaryButton(text = "약속 공유", onClick = onShare, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LocationSearchDialog(
    results: List<LocationSearchResult>,
    onSearch: (String) -> Unit,
    onSelect: (LocationSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchKeyboardActions = KeyboardActions(
        onDone = {
            onSearch(query)
            keyboardController?.hide()
        },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("출발 위치 검색") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("역 또는 주요 장소") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = searchKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
                WwmPrimaryButton(text = "검색", onClick = { onSearch(query) })
                results.take(6).forEach { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WwmSurfaceSubtle, RoundedCornerShape(12.dp))
                            .clickable { onSelect(result) }
                            .padding(12.dp),
                    ) {
                        Text(result.label, fontWeight = FontWeight.Bold)
                        result.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WwmMuted) }
                    }
                }
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text("닫기") } },
    )
}
