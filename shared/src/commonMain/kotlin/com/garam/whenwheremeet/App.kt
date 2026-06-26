package com.garam.whenwheremeet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.garam.whenwheremeet.data.provider.MetropolitanMeetingAreaCandidateProvider
import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.di.appModule
import com.garam.whenwheremeet.platform.AuthSession
import com.garam.whenwheremeet.platform.rememberShareService
import com.garam.whenwheremeet.platform.rememberAuthPlatform
import com.garam.whenwheremeet.platform.rememberExternalUrlLauncher
import com.garam.whenwheremeet.platform.rememberMapLauncher
import com.garam.whenwheremeet.platform.PlatformBackHandler
import com.garam.whenwheremeet.presentation.screen.CreateMeetingRoomScreen
import com.garam.whenwheremeet.presentation.screen.CalendarScreen
import com.garam.whenwheremeet.presentation.screen.HomeScreen
import com.garam.whenwheremeet.presentation.screen.JoinRoomScreen
import com.garam.whenwheremeet.presentation.screen.MeetingRoomScreen
import com.garam.whenwheremeet.presentation.screen.MyPageScreen
import com.garam.whenwheremeet.presentation.screen.OnboardingScreen
import com.garam.whenwheremeet.presentation.component.WwmTheme
import com.garam.whenwheremeet.presentation.state.AppRoute
import com.garam.whenwheremeet.presentation.state.MeetingAppState
import com.garam.whenwheremeet.presentation.state.UiAction
import com.garam.whenwheremeet.presentation.state.UiEvent
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.koinInject

private const val TemporaryFeedbackUrl = "https://example.com"
private const val TemporaryTermsUrl = "https://example.com"
private const val TemporaryPrivacyUrl = "https://example.com"
private const val OnboardingCompletedKey = "onboarding_completed"
private const val MeetingSnapshotKey = "meeting_mvp_snapshot_v1"

@Suppress("DEPRECATION")
@Composable
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val storage = koinInject<KeyValueStorage>()
    var onboardingCompleted by remember {
        mutableStateOf(storage.getString(OnboardingCompletedKey) == "true")
    }
    var authSession by remember { mutableStateOf(currentAuthSession()) }
    var appDataGeneration by remember { mutableStateOf(0) }
    val koin = getKoin()
    val appState = remember(appDataGeneration) {
        koin.get<MeetingAppState>()
    }
    val areaCandidateProvider = koinInject<MetropolitanMeetingAreaCandidateProvider>()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareService = rememberShareService()
    val mapLauncher = rememberMapLauncher()
    val authPlatform = rememberAuthPlatform()
    val externalUrlLauncher = rememberExternalUrlLauncher()
    val event = appState.event

    LaunchedEffect(Unit) {
        initialRoomCodeFromLaunch()?.let { roomCode ->
            appState.dispatch(UiAction.OpenJoinRoom(roomCode))
        }
    }

    fun completeOnboarding() {
        storage.putString(OnboardingCompletedKey, "true")
        onboardingCompleted = true
    }

    fun resetLocalAppDataAndShowOnboarding() {
        storage.remove(OnboardingCompletedKey)
        storage.remove(MeetingSnapshotKey)
        authSession = null
        appDataGeneration += 1
        onboardingCompleted = false
    }

    LaunchedEffect(event) {
        when (event) {
            is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            is UiEvent.Share -> shareService.share(event.text)
            is UiEvent.OpenMap -> mapLauncher.openMap(event.place)
            null -> Unit
        }
        if (event != null) appState.consumeEvent()
    }

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted && authSession == null) {
            runCatching { authPlatform.signInAnonymously() }
                .onSuccess { authSession = it }
                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("익명 로그인에 실패했어요.")) }
        }
    }

    PlatformBackHandler(enabled = onboardingCompleted && appState.route != AppRoute.Home) {
        appState.dispatch(UiAction.NavigateBack)
    }

    WwmTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (!onboardingCompleted) {
                OnboardingScreen(
                    showAppleSignIn = authPlatform.showAppleSignIn,
                    onGoogleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithGoogle() }
                                .onSuccess {
                                    authSession = it
                                    completeOnboarding()
                                    snackbarHostState.showSnackbar("Google 로그인으로 시작했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Google 로그인에 실패했어요.")) }
                        }
                    },
                    onAppleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithApple() }
                                .onSuccess {
                                    authSession = it
                                    completeOnboarding()
                                    snackbarHostState.showSnackbar("Apple 로그인으로 시작했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Apple 로그인에 실패했어요.")) }
                        }
                    },
                    onStartWithoutLogin = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInAnonymously() }
                                .onSuccess {
                                    authSession = it
                                    completeOnboarding()
                                    snackbarHostState.showSnackbar("익명 계정으로 시작했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("익명 로그인에 실패했어요.")) }
                        }
                    },
                    onOpenUrl = externalUrlLauncher::openUrl,
                    modifier = Modifier.padding(padding),
                )
            } else when (val route = appState.route) {
                AppRoute.Home -> HomeScreen(
                    state = appState.homeUiState(),
                    onCreateRoom = { appState.dispatch(UiAction.OpenCreateRoom) },
                    onJoinRoom = { appState.dispatch(UiAction.OpenJoinRoom()) },
                    onOpenRoom = { appState.dispatch(UiAction.OpenRoom(it)) },
                    onOpenMap = appState::openConfirmedPlaceMap,
                    onOpenCalendar = { appState.dispatch(UiAction.OpenCalendar) },
                    onOpenMyPage = { appState.dispatch(UiAction.OpenMyPage) },
                    onRefresh = { coroutineScope.launch { appState.refreshDashboard() } },
                    modifier = Modifier.padding(padding),
                )
                AppRoute.Calendar -> CalendarScreen(
                    state = appState.calendarUiState(),
                    onPreviousMonth = { appState.dispatch(UiAction.OpenPreviousCalendarMonth) },
                    onNextMonth = { appState.dispatch(UiAction.OpenNextCalendarMonth) },
                    onToday = { appState.dispatch(UiAction.OpenTodayCalendarMonth) },
                    onFilterChange = { appState.dispatch(UiAction.ChangeCalendarFilter(it)) },
                    onSelectDate = { appState.dispatch(UiAction.SelectCalendarDate(it)) },
                    onOpenRoom = { appState.dispatch(UiAction.OpenRoom(it)) },
                    onOpenMap = appState::openConfirmedPlaceMap,
                    onCreateRoom = { appState.dispatch(UiAction.OpenCreateRoom) },
                    onOpenHome = { appState.dispatch(UiAction.OpenHome) },
                    onOpenMyPage = { appState.dispatch(UiAction.OpenMyPage) },
                    onRefresh = { coroutineScope.launch { appState.refreshDashboard() } },
                    modifier = Modifier.padding(padding),
                )
                AppRoute.MyPage -> MyPageScreen(
                    authSession = authSession,
                    showAppleSignIn = authPlatform.showAppleSignIn,
                    onGoogleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithGoogle() }
                                .onSuccess {
                                    authSession = it
                                    snackbarHostState.showSnackbar("Google 로그인으로 전환했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Google 로그인에 실패했어요.")) }
                        }
                    },
                    onAppleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithApple() }
                                .onSuccess {
                                    authSession = it
                                    snackbarHostState.showSnackbar("Apple 로그인으로 전환했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Apple 로그인에 실패했어요.")) }
                        }
                    },
                    onOpenFeedback = { externalUrlLauncher.openUrl(TemporaryFeedbackUrl) },
                    onOpenTerms = { externalUrlLauncher.openUrl(TemporaryTermsUrl) },
                    onOpenPrivacy = { externalUrlLauncher.openUrl(TemporaryPrivacyUrl) },
                    onSignOut = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signOut() }
                                .onSuccess {
                                    resetLocalAppDataAndShowOnboarding()
                                    snackbarHostState.showSnackbar("로그아웃했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("로그아웃에 실패했어요.")) }
                        }
                    },
                    onDeleteAccount = {
                        coroutineScope.launch {
                            runCatching { authPlatform.deleteAccount() }
                                .onSuccess {
                                    resetLocalAppDataAndShowOnboarding()
                                    snackbarHostState.showSnackbar("회원 탈퇴가 완료됐어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("회원 탈퇴에 실패했어요. 다시 로그인한 뒤 시도해주세요.")) }
                        }
                    },
                    onOpenHome = { appState.dispatch(UiAction.OpenHome) },
                    onOpenCalendar = { appState.dispatch(UiAction.OpenCalendar) },
                    modifier = Modifier.padding(padding),
                )
                AppRoute.CreateRoom -> CreateMeetingRoomScreen(
                    onBack = { appState.dispatch(UiAction.NavigateBack) },
                    onCreate = { coroutineScope.launch { appState.createRoom(it) } },
                    modifier = Modifier.padding(padding),
                )
                is AppRoute.JoinRoom -> JoinRoomScreen(
                    initialRoomCode = route.roomCode,
                    onBack = { appState.dispatch(UiAction.NavigateBack) },
                    onJoin = { roomCode, nickname -> coroutineScope.launch { appState.joinRoom(roomCode, nickname) } },
                    modifier = Modifier.padding(padding),
                )
                is AppRoute.MeetingRoom -> {
                    LaunchedEffect(route.roomId) {
                        appState.refreshRoom(route.roomId)
                    }
                    LaunchedEffect("room-updates-${route.roomId}") {
                        appState.collectRoomUpdates(route.roomId)
                    }
                    val state = appState.roomUiState(route.roomId)
                    if (state == null) {
                        LaunchedEffect(route.roomId) { appState.handleCurrentRoomUnavailable(route.roomId) }
                    } else {
                        MeetingRoomScreen(
                            state = state,
                            onBack = { appState.dispatch(UiAction.NavigateBack) },
                            onCycleDate = appState::cycleAvailability,
                            onSelectDate = appState::selectDate,
                            onSave = { coroutineScope.launch { appState.saveAvailability(route.roomId) } },
                            onConfirm = { coroutineScope.launch { appState.confirmDate(route.roomId, it) } },
                            onShare = { appState.requestShare(route.roomId) },
                            onSearchLocations = { query -> coroutineScope.launch { appState.searchLocations(query) } },
                            onUseCurrentLocation = { coroutineScope.launch { appState.useCurrentLocation(route.roomId) } },
                            onSaveStartLocation = { appState.saveStartLocation(route.roomId, it) },
                            onSaveTransportMode = { appState.saveTransportMode(route.roomId, it) },
                            onCalculateAreas = {
                                coroutineScope.launch {
                                    appState.calculateAreaRecommendations(
                                        roomId = route.roomId,
                                        candidates = areaCandidateProvider.getCandidates(state.startLocations),
                                    )
                                }
                            },
                            onSelectArea = { appState.selectAreaCandidate(route.roomId, it) },
                            onSearchPlaces = { coroutineScope.launch { appState.searchPlaceCandidates(route.roomId) } },
                            onVotePlace = { placeId, vote -> appState.votePlace(route.roomId, placeId, vote) },
                            onConfirmPlace = { appState.confirmPlace(route.roomId, it) },
                            onOpenMap = appState::openMap,
                            onLeaveRoom = { coroutineScope.launch { appState.leaveRoom(route.roomId) } },
                            onDeleteRoom = { coroutineScope.launch { appState.deleteRoom(route.roomId) } },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}

private fun Throwable.authErrorMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
