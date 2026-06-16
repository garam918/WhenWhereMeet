package com.garam.whenwheremeet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.data.provider.FakeLocationSearchProvider
import com.garam.whenwheremeet.data.provider.FakePlaceSearchProvider
import com.garam.whenwheremeet.data.provider.FakeTravelTimeProvider
import com.garam.whenwheremeet.data.provider.SampleLocationData
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.platform.rememberShareService
import com.garam.whenwheremeet.platform.rememberMapLauncher
import com.garam.whenwheremeet.presentation.screen.CreateMeetingRoomScreen
import com.garam.whenwheremeet.presentation.screen.HomeScreen
import com.garam.whenwheremeet.presentation.screen.JoinRoomScreen
import com.garam.whenwheremeet.presentation.screen.MeetingRoomScreen
import com.garam.whenwheremeet.presentation.component.WwmTheme
import com.garam.whenwheremeet.presentation.state.AppRoute
import com.garam.whenwheremeet.presentation.state.MeetingAppState
import com.garam.whenwheremeet.presentation.state.UiAction
import com.garam.whenwheremeet.presentation.state.UiEvent
import com.garam.whenwheremeet.domain.usecase.RecommendMeetingAreasUseCase
import kotlinx.coroutines.launch

@Composable
fun App() {
    val appState = remember {
        MeetingAppState(
            repository = LocalMeetingRepository(platformKeyValueStorage()),
            locationSearchProvider = FakeLocationSearchProvider(),
            recommendMeetingAreas = RecommendMeetingAreasUseCase(FakeTravelTimeProvider()),
            placeSearchProvider = FakePlaceSearchProvider(),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareService = rememberShareService()
    val mapLauncher = rememberMapLauncher()
    val event = appState.event

    LaunchedEffect(event) {
        when (event) {
            is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            is UiEvent.Share -> shareService.share(event.text)
            is UiEvent.OpenMap -> mapLauncher.openMap(event.place)
            null -> Unit
        }
        if (event != null) appState.consumeEvent()
    }

    WwmTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when (val route = appState.route) {
                AppRoute.Home -> HomeScreen(
                    state = appState.homeUiState(),
                    onCreateRoom = { appState.dispatch(UiAction.OpenCreateRoom) },
                    onJoinRoom = { appState.dispatch(UiAction.OpenJoinRoom()) },
                    onOpenRoom = { appState.dispatch(UiAction.OpenRoom(it)) },
                    modifier = Modifier.padding(padding),
                )
                AppRoute.CreateRoom -> CreateMeetingRoomScreen(
                    onBack = { appState.dispatch(UiAction.NavigateBack) },
                    onCreate = appState::createRoom,
                    modifier = Modifier.padding(padding),
                )
                is AppRoute.JoinRoom -> JoinRoomScreen(
                    initialRoomCode = route.roomCode,
                    onBack = { appState.dispatch(UiAction.NavigateBack) },
                    onJoin = appState::joinRoom,
                    modifier = Modifier.padding(padding),
                )
                is AppRoute.MeetingRoom -> {
                    val state = appState.roomUiState(route.roomId)
                    if (state == null) {
                        LaunchedEffect(route.roomId) { appState.dispatch(UiAction.OpenJoinRoom(route.roomId)) }
                    } else {
                        MeetingRoomScreen(
                            state = state,
                            onBack = { appState.dispatch(UiAction.NavigateBack) },
                            onCycleDate = appState::cycleAvailability,
                            onSelectDate = appState::selectDate,
                            onSave = { appState.saveAvailability(route.roomId) },
                            onConfirm = { appState.confirmDate(route.roomId, it) },
                            onShare = { appState.requestShare(route.roomId) },
                            onSearchLocations = { query -> coroutineScope.launch { appState.searchLocations(query) } },
                            onUseCurrentLocation = { coroutineScope.launch { appState.useCurrentLocation(route.roomId) } },
                            onSaveStartLocation = { appState.saveStartLocation(route.roomId, it) },
                            onSaveTransportMode = { appState.saveTransportMode(route.roomId, it) },
                            onCalculateAreas = {
                                coroutineScope.launch {
                                    appState.calculateAreaRecommendations(route.roomId, SampleLocationData.areaCandidates)
                                }
                            },
                            onSelectArea = { appState.selectAreaCandidate(route.roomId, it) },
                            onSearchPlaces = { coroutineScope.launch { appState.searchPlaceCandidates(route.roomId) } },
                            onVotePlace = { placeId, vote -> appState.votePlace(route.roomId, placeId, vote) },
                            onConfirmPlace = { appState.confirmPlace(route.roomId, it) },
                            onOpenMap = appState::openMap,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
