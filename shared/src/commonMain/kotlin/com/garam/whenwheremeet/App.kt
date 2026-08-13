package com.garam.whenwheremeet

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.di.appModule
import com.garam.whenwheremeet.domain.model.CalendarEventDraft
import com.garam.whenwheremeet.platform.AuthSession
import com.garam.whenwheremeet.platform.CalendarLaunchResult
import com.garam.whenwheremeet.platform.rememberCalendarService
import com.garam.whenwheremeet.platform.rememberShareService
import com.garam.whenwheremeet.platform.rememberAuthPlatform
import com.garam.whenwheremeet.platform.rememberExternalUrlLauncher
import com.garam.whenwheremeet.platform.rememberMapLauncher
import com.garam.whenwheremeet.platform.PlatformBackHandler
import com.garam.whenwheremeet.platform.PlatformDeepLinkEffect
import com.garam.whenwheremeet.presentation.screen.CreateMeetingRoomScreen
import com.garam.whenwheremeet.presentation.screen.CalendarScreen
import com.garam.whenwheremeet.presentation.screen.HomeScreen
import com.garam.whenwheremeet.presentation.screen.JoinRoomScreen
import com.garam.whenwheremeet.presentation.screen.MeetingRoomScreen
import com.garam.whenwheremeet.presentation.screen.MyPageScreen
import com.garam.whenwheremeet.presentation.screen.OnboardingScreen
import com.garam.whenwheremeet.presentation.component.CalendarAddBottomSheet
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
private const val TermsUrl = "https://whenwheremeet-legal.web.app/terms/"
private const val PrivacyUrl = "https://whenwheremeet-legal.web.app/privacy/"
private const val CachedAccountIdKey = "meeting_cache_account_uid"

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
    val restoredAuthSession = remember { currentAuthSession() }
    var authSession by remember {
        mutableStateOf(restoredAuthSession?.takeUnless(AuthSession::isAnonymous))
    }
    var appDataGeneration by remember { mutableStateOf(0) }
    val koin = getKoin()
    val appState = remember(appDataGeneration) {
        koin.get<MeetingAppState>()
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val calendarService = rememberCalendarService()
    val shareService = rememberShareService()
    val mapLauncher = rememberMapLauncher()
    val authPlatform = rememberAuthPlatform()
    val externalUrlLauncher = rememberExternalUrlLauncher()
    val event = appState.event
    val focusManager = LocalFocusManager.current
    var pendingCalendarEvent by remember { mutableStateOf<CalendarEventDraft?>(null) }
    var pendingInviteRoomCode by remember { mutableStateOf<String?>(null) }
    val isAuthenticated = authSession?.isAnonymous == false

    PlatformDeepLinkEffect { roomCode ->
        pendingInviteRoomCode = roomCode
    }

    fun acceptAuthenticatedSession(session: AuthSession) {
        if (session.isAnonymous) return
        if (storage.getString(CachedAccountIdKey) != session.uid) {
            appState.clearLocalCache()
        }
        storage.putString(CachedAccountIdKey, session.uid)
        authSession = session
        appDataGeneration += 1
    }

    fun resetLocalAppDataAndShowLogin() {
        appState.clearLocalCache()
        storage.remove(CachedAccountIdKey)
        authSession = null
        appDataGeneration += 1
    }

    LaunchedEffect(authSession?.uid, appState) {
        val session = authSession?.takeUnless(AuthSession::isAnonymous) ?: return@LaunchedEffect
        if (storage.getString(CachedAccountIdKey) != session.uid) {
            appState.clearLocalCache()
            storage.putString(CachedAccountIdKey, session.uid)
        }
        appState.restoreAccountMeetings()
    }

    LaunchedEffect(isAuthenticated, pendingInviteRoomCode, appState) {
        val roomCode = pendingInviteRoomCode ?: return@LaunchedEffect
        if (!isAuthenticated) return@LaunchedEffect
        pendingInviteRoomCode = null
        appState.openInviteRoom(roomCode)
    }

    LaunchedEffect(event) {
        when (event) {
            is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            is UiEvent.Share -> shareService.share(event.text)
            is UiEvent.OpenMap -> mapLauncher.openMap(event.place)
            is UiEvent.ShowCalendarPrompt -> pendingCalendarEvent = event.calendarEvent
            is UiEvent.OpenExternalUrl -> externalUrlLauncher.openUrl(event.url)
            null -> Unit
        }
        if (event != null) appState.consumeEvent()
    }

    PlatformBackHandler(enabled = isAuthenticated && appState.route != AppRoute.Home) {
        appState.dispatch(UiAction.NavigateBack)
    }

    WwmTheme {
        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            if (!isAuthenticated) {
                OnboardingScreen(
                    showAppleSignIn = authPlatform.showAppleSignIn,
                    onGoogleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithGoogle() }
                                .onSuccess {
                                    acceptAuthenticatedSession(it)
                                    snackbarHostState.showSnackbar("Google 로그인으로 시작했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Google 로그인에 실패했어요.")) }
                        }
                    },
                    onAppleSignIn = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signInWithApple() }
                                .onSuccess {
                                    acceptAuthenticatedSession(it)
                                    snackbarHostState.showSnackbar("Apple 로그인으로 시작했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("Apple 로그인에 실패했어요.")) }
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
                    onOpenFeedback = { externalUrlLauncher.openUrl(TemporaryFeedbackUrl) },
                    onOpenTerms = { externalUrlLauncher.openUrl(TermsUrl) },
                    onOpenPrivacy = { externalUrlLauncher.openUrl(PrivacyUrl) },
                    onSignOut = {
                        coroutineScope.launch {
                            runCatching { authPlatform.signOut() }
                                .onSuccess {
                                    resetLocalAppDataAndShowLogin()
                                    snackbarHostState.showSnackbar("로그아웃했어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("로그아웃에 실패했어요.")) }
                        }
                    },
                    onDeleteAccount = {
                        coroutineScope.launch {
                            runCatching { authPlatform.deleteAccount() }
                                .onSuccess {
                                    resetLocalAppDataAndShowLogin()
                                    snackbarHostState.showSnackbar("회원 탈퇴가 완료됐어요.")
                                }
                                .onFailure { snackbarHostState.showSnackbar(it.authErrorMessage("회원 탈퇴에 실패했어요. 다시 로그인한 뒤 시도해주세요.")) }
                        }
                    },
                    onOpenHome = { appState.dispatch(UiAction.OpenHome) },
                    onOpenCalendar = { appState.dispatch(UiAction.OpenCalendar) },
                    onCreateRoom = { appState.dispatch(UiAction.OpenCreateRoom) },
                    modifier = Modifier.padding(padding),
                )
                AppRoute.CreateRoom -> {
                    LaunchedEffect(Unit) { appState.refreshFriends() }
                    CreateMeetingRoomScreen(
                        friends = appState.friends(),
                        onBack = { appState.dispatch(UiAction.NavigateBack) },
                        onCreate = { coroutineScope.launch { appState.createRoom(it) } },
                        modifier = Modifier.padding(padding),
                    )
                }
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
                            onSaveStartLocation = { coroutineScope.launch { appState.saveStartLocation(route.roomId, it) } },
                            onSearchDestinationStations = { query ->
                                coroutineScope.launch { appState.searchDestinationStations(query) }
                            },
                            onProposeDestinationStation = {
                                coroutineScope.launch { appState.proposeDestinationStation(route.roomId, it) }
                            },
                            onVoteDestinationStation = {
                                coroutineScope.launch { appState.voteDestinationStation(route.roomId, it) }
                            },
                            onConfirmDestinationStation = {
                                coroutineScope.launch { appState.confirmDestinationStation(route.roomId, it) }
                            },
                            onConfirmWithoutPlace = {
                                coroutineScope.launch { appState.confirmMeetingWithoutPlace(route.roomId) }
                            },
                            onOpenDestinationRoute = { appState.openDestinationStationRoute(route.roomId, it) },
                            onOpenConfirmedRoute = { appState.openConfirmedDestinationRoute(route.roomId) },
                            onAddToCalendar = { appState.requestCalendarPrompt(route.roomId) },
                            onLeaveRoom = { coroutineScope.launch { appState.leaveRoom(route.roomId) } },
                            onDeleteRoom = { coroutineScope.launch { appState.deleteRoom(route.roomId) } },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
        pendingCalendarEvent?.let { calendarEvent ->
            CalendarAddBottomSheet(
                event = calendarEvent,
                onAddToCalendar = {
                    when (calendarService.openEventEditor(calendarEvent)) {
                        CalendarLaunchResult.OPENED -> pendingCalendarEvent = null
                        CalendarLaunchResult.UNAVAILABLE -> coroutineScope.launch {
                            snackbarHostState.showSnackbar("사용할 수 있는 캘린더 앱을 찾지 못했어요.")
                        }
                        CalendarLaunchResult.FAILED -> coroutineScope.launch {
                            snackbarHostState.showSnackbar("캘린더를 열지 못했어요. 다시 시도해주세요.")
                        }
                    }
                },
                onDismiss = { pendingCalendarEvent = null },
            )
        }
    }
}

private fun Throwable.authErrorMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
