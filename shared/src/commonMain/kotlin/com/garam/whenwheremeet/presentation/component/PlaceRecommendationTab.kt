package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
) {
    val room = state.room
    if (room.confirmedDate == null) {
        InfoCard("날짜를 먼저 확정해주세요.")
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StartLocationSection(state, onUseCurrentLocation, onSaveTransportMode) { showLocationDialog = true }
        StartLocationStatus(state)

        if (state.areaRecommendations.isEmpty()) {
            Button(
                onClick = onCalculateAreas,
                enabled = !state.isCalculatingAreas && state.startLocations.size == state.participants.size,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isCalculatingAreas) "추천 계산 중..." else "추천 지역 계산") }
        }

        if (state.room.selectedAreaCandidateId == null) {
            if (state.areaRecommendations.isNotEmpty()) SectionTitle("추천 지역 TOP 3")
            state.areaRecommendations.forEachIndexed { index, recommendation ->
                AreaRecommendationCard(
                    rank = index + 1,
                    recommendation = recommendation,
                    expanded = expandedAreaId == recommendation.candidate.id,
                    isHost = state.currentParticipant.isHost,
                    onToggle = {
                        expandedAreaId = if (expandedAreaId == recommendation.candidate.id) null else recommendation.candidate.id
                    },
                    onSelect = { onSelectArea(recommendation.candidate.id) },
                )
            }
        } else {
            val area = state.areaRecommendations.firstOrNull { it.candidate.id == state.room.selectedAreaCandidateId }
            SectionTitle("선택 지역")
            InfoCard(area?.candidate?.displayName ?: "선택된 추천 지역")

            if (state.placeCandidates.isEmpty()) {
                Button(
                    onClick = onSearchPlaces,
                    enabled = !state.isSearchingPlaces,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.isSearchingPlaces) "장소 검색 중..." else "장소 후보 찾기") }
            } else {
                SectionTitle("장소 후보와 투표")
                state.placeCandidates.forEachIndexed { index, scoredPlace ->
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

    if (showLocationDialog) {
        LocationSearchDialog(
            results = state.locationSearchResults,
            onSearch = onSearchLocations,
            onSelect = { onSaveStartLocation(it); showLocationDialog = false },
            onDismiss = { showLocationDialog = false },
        )
    }
}

@Composable
private fun StartLocationSection(
    state: MeetingRoomUiState,
    onUseCurrentLocation: () -> Unit,
    onSaveTransportMode: (TransportMode) -> Unit,
    onOpenLocationDialog: () -> Unit,
) {
    SectionTitle("내 출발 정보")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.currentStartLocation?.label ?: "출발 위치 미입력", fontWeight = FontWeight.Bold)
            Text("정확한 좌표는 다른 참여자에게 공개되지 않습니다.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenLocationDialog) { Text(if (state.currentStartLocation == null) "출발 위치 입력" else "출발 위치 수정") }
                OutlinedButton(onClick = onUseCurrentLocation) { Text("현재 위치 사용") }
            }
            Text("이동수단", fontWeight = FontWeight.SemiBold)
            TransportMode.entries.filter { it != TransportMode.UNKNOWN }.chunked(2).forEach { modes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
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
}

@Composable
private fun StartLocationStatus(state: MeetingRoomUiState) {
    SectionTitle("출발 위치 입력 현황")
    val locationByParticipant = state.startLocations.associateBy { it.participantId }
    state.participants.forEach { participant ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(participant.nickname)
            Text(locationByParticipant[participant.id]?.label ?: "미입력", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
    val summary = remember(place.id, state.placeVotes) { AggregatePlaceVotesUseCase()(place.id, state.placeVotes) }
    val myVote = state.placeVotes.firstOrNull {
        it.placeId == place.id && it.participantId == state.currentParticipant.id
    }?.voteType
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${rank}순위 · ${place.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${place.category.label} · 평점 ${place.rating ?: "정보 없음"} · 리뷰 ${place.reviewCount ?: 0}개")
            Text(place.roadAddress ?: place.address ?: "주소 정보 없음")
            scoredPlace.reasons.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Text("좋아요 ${summary.likeCount} · 별로예요 ${summary.dislikeCount}", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlaceVoteType.entries.forEach { voteType ->
                    FilterChip(
                        selected = myVote == voteType,
                        onClick = { onVote(voteType) },
                        label = { Text(voteType.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenMap, enabled = place.mapUrl != null) { Text("지도 열기") }
                if (state.currentParticipant.isHost) {
                    Button(onClick = onConfirm) { Text("이 장소로 확정") }
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("약속이 확정됐어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(dateText, style = MaterialTheme.typography.titleMedium)
            Text("장소: ${place.name}", fontWeight = FontWeight.Bold)
            Text("주소: ${place.roadAddress ?: place.address ?: "정보 없음"}")
            Text("참여자: ${participantCount}명")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenMap, enabled = place.mapUrl != null) { Text("지도 보기") }
                Button(onClick = onShare) { Text("약속 공유하기") }
            }
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp))
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("출발 위치 검색") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("역 또는 지역명") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSearch(query) }, modifier = Modifier.fillMaxWidth()) { Text("검색") }
                results.take(6).forEach { result ->
                    Card(Modifier.fillMaxWidth().clickable { onSelect(result) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(result.label, fontWeight = FontWeight.Bold)
                            result.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        },
        confirmButton = { OutlinedButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun AreaRecommendationCard(
    rank: Int,
    recommendation: AreaRecommendation,
    expanded: Boolean,
    isHost: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val difference = recommendation.maxTravelMinutes - recommendation.minTravelMinutes
    val differenceLabel = when {
        sqrt(recommendation.travelTimeVariance) < 8 -> "낮음"
        sqrt(recommendation.travelTimeVariance) < 15 -> "보통"
        else -> "높음"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${rank}순위 · ${recommendation.candidate.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("평균 ${recommendation.averageTravelMinutes}분 · 최대 ${recommendation.maxTravelMinutes}분 · 차이 $differenceLabel ($difference 분)")
            Text(recommendation.recommendationReason, style = MaterialTheme.typography.bodySmall)
            if (expanded) {
                recommendation.participantTravelTimes.forEach { Text("${it.participantNickname}: 약 ${it.travelMinutes}분") }
                if (isHost) Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) { Text("이 지역을 후보로 선택") }
            }
        }
    }
}
