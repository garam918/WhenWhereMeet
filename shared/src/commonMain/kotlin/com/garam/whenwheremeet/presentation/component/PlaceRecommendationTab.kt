package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StartLocationSection(state, onUseCurrentLocation, onSaveTransportMode) { showLocationDialog = true }
        StartLocationStatus(state)

        if (state.placeCandidates.isEmpty()) {
            Button(
                onClick = onSearchPlaces,
                enabled = !state.isSearchingPlaces && state.startLocations.size == state.participants.size,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isSearchingPlaces) "Gemini 추천 중..." else "Gemini로 장소 후보 추천받기") }
        } else {
            SectionTitle("Gemini 추천 장소 후보")
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
            Text(state.currentStartLocation?.label ?: "출발역 미입력", fontWeight = FontWeight.Bold)
            Text("참여자에게는 출발역 이름만 보입니다.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenLocationDialog) { Text(if (state.currentStartLocation == null) "출발역 입력" else "출발역 수정") }
            }
        }
    }
}

@Composable
private fun StartLocationStatus(state: MeetingRoomUiState) {
    SectionTitle("출발역 입력 현황")
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
            Text("${place.category.label} · Gemini 점수 ${scoredPlace.score.toInt()}점")
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchKeyboardActions = KeyboardActions(
        onDone = {
            onSearch(query)
            keyboardController?.hide()
        },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("출발역 검색") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("역 이름") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = searchKeyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
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
