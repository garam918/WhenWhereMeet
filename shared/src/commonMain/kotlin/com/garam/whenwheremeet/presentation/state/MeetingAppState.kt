package com.garam.whenwheremeet.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.garam.whenwheremeet.buildRoomJoinLink
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.DateAvailabilitySummary
import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingEvent
import com.garam.whenwheremeet.domain.model.MeetingEventPublisher
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.NoOpMeetingEventPublisher
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.RecommendedDate
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.provider.LocationSearchProvider
import com.garam.whenwheremeet.domain.provider.PlaceSearchProvider
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.domain.usecase.AggregateAvailabilityUseCase
import com.garam.whenwheremeet.domain.usecase.BuildCalendarMonthUseCase
import com.garam.whenwheremeet.domain.usecase.BuildHomeDashboardUseCase
import com.garam.whenwheremeet.domain.usecase.CycleAvailabilityStatusUseCase
import com.garam.whenwheremeet.domain.usecase.CalendarFilter
import com.garam.whenwheremeet.domain.usecase.CalendarMonth
import com.garam.whenwheremeet.domain.usecase.MeetingOverview
import com.garam.whenwheremeet.domain.usecase.RecommendDatesUseCase
import com.garam.whenwheremeet.domain.usecase.RecommendMeetingAreasUseCase
import com.garam.whenwheremeet.domain.usecase.BuildConfirmedMeetingShareTextUseCase
import com.garam.whenwheremeet.domain.usecase.ScorePlaceCandidatesUseCase
import com.garam.whenwheremeet.domain.usecase.SortPlacesByVotesUseCase
import com.garam.whenwheremeet.platform.currentLocalDate
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

sealed interface AppRoute {
    data object Home : AppRoute
    data object Calendar : AppRoute
    data object MyPage : AppRoute
    data object CreateRoom : AppRoute
    data class JoinRoom(val roomCode: String = "") : AppRoute
    data class MeetingRoom(val roomId: String) : AppRoute
}

sealed interface UiAction {
    data object OpenHome : UiAction
    data object OpenCalendar : UiAction
    data object OpenMyPage : UiAction
    data object OpenCreateRoom : UiAction
    data class OpenJoinRoom(val roomCode: String = "") : UiAction
    data class OpenRoom(val roomId: String) : UiAction
    data class SelectCalendarDate(val date: LocalDate) : UiAction
    data object OpenPreviousCalendarMonth : UiAction
    data object OpenNextCalendarMonth : UiAction
    data object OpenTodayCalendarMonth : UiAction
    data class ChangeCalendarFilter(val filter: CalendarFilter) : UiAction
    data object NavigateBack : UiAction
}

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
    data class Share(val text: String) : UiEvent
    data class OpenMap(val place: PlaceCandidate) : UiEvent
}

data class MeetingRoomUiState(
    val room: MeetingRoom,
    val participants: List<Participant>,
    val currentParticipant: Participant,
    val selectedAvailability: Map<LocalDate, AvailabilityStatus>,
    val summaries: List<DateAvailabilitySummary>,
    val recommendations: List<RecommendedDate>,
    val selectedSummary: DateAvailabilitySummary?,
    val startLocations: List<UserStartLocation>,
    val currentStartLocation: UserStartLocation?,
    val currentTransportMode: TransportMode,
    val areaRecommendations: List<AreaRecommendation>,
    val locationSearchResults: List<LocationSearchResult>,
    val isCalculatingAreas: Boolean,
    val placeCandidates: List<ScoredPlaceCandidate>,
    val placeVotes: List<PlaceVote>,
    val isSearchingPlaces: Boolean,
)

data class CreateRoomInput(
    val hostNickname: String,
    val title: String,
    val description: String,
    val meetingType: MeetingType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val minParticipants: Int,
    val maxParticipants: Int,
    val responseDeadline: LocalDate?,
    val hostIsRequired: Boolean,
)

class MeetingAppState(
    private val repository: MeetingRepository,
    private val aggregateAvailability: AggregateAvailabilityUseCase = AggregateAvailabilityUseCase(),
    private val recommendDates: RecommendDatesUseCase = RecommendDatesUseCase(),
    private val cycleStatus: CycleAvailabilityStatusUseCase = CycleAvailabilityStatusUseCase(),
    private val eventPublisher: MeetingEventPublisher = NoOpMeetingEventPublisher,
    private val locationSearchProvider: LocationSearchProvider,
    private val recommendMeetingAreas: RecommendMeetingAreasUseCase,
    private val placeSearchProvider: PlaceSearchProvider,
    private val scorePlaces: ScorePlaceCandidatesUseCase = ScorePlaceCandidatesUseCase(),
    private val sortPlacesByVotes: SortPlacesByVotesUseCase = SortPlacesByVotesUseCase(),
    private val buildConfirmedShareText: BuildConfirmedMeetingShareTextUseCase = BuildConfirmedMeetingShareTextUseCase(),
    private val buildHomeDashboard: BuildHomeDashboardUseCase = BuildHomeDashboardUseCase(),
    private val buildCalendarMonth: BuildCalendarMonthUseCase = BuildCalendarMonthUseCase(),
) {
    var route: AppRoute by mutableStateOf(AppRoute.Home)
        private set
    private var backStack by mutableStateOf<List<AppRoute>>(emptyList())
    var event: UiEvent? by mutableStateOf(null)
        private set
    private var revision by mutableStateOf(0)
    private var availabilityDraft by mutableStateOf<Map<LocalDate, AvailabilityStatus>>(emptyMap())
    private var selectedDate by mutableStateOf<LocalDate?>(null)
    private var calendarMonth by mutableStateOf(CalendarMonth.from(currentLocalDate()))
    private var calendarSelectedDate by mutableStateOf(currentLocalDate())
    private var calendarFilter by mutableStateOf(CalendarFilter.ALL)
    private var locationSearchResults by mutableStateOf<List<LocationSearchResult>>(emptyList())
    private var isCalculatingAreas by mutableStateOf(false)
    private var isSearchingPlaces by mutableStateOf(false)
    private var lastManualRefreshAt: Instant? = null

    fun homeUiState(): HomeDashboardUiState {
        revision
        val dashboard = buildHomeDashboard(meetingOverviews(), currentLocalDate())
        return HomeDashboardUiState(
            summary = HomeSummaryUiModel(
                confirmedCount = dashboard.summary.confirmedCount,
                pendingResponseCount = dashboard.summary.pendingResponseCount,
                placeVoteRequiredCount = dashboard.summary.placeVoteRequiredCount,
            ),
            actionItems = dashboard.actionItems.map {
                HomeActionItemUiModel(
                    roomId = it.roomId,
                    title = it.title,
                    actionType = it.actionType,
                    statusText = it.statusText,
                    description = it.description,
                    dueText = it.dueText,
                    ctaText = it.ctaText,
                )
            },
            upcomingConfirmedMeetings = dashboard.upcomingConfirmedMeetings.map { it.toUiModel() },
            inProgressMeetings = dashboard.inProgressMeetings.map { it.toUiModel() },
        )
    }

    fun calendarUiState(): CalendarUiState {
        revision
        val calendar = buildCalendarMonth(
            meetings = meetingOverviews(),
            currentMonth = calendarMonth,
            selectedDate = calendarSelectedDate,
            today = currentLocalDate(),
            filter = calendarFilter,
        )
        return CalendarUiState(
            currentMonth = calendar.currentMonth,
            selectedDate = calendar.selectedDate,
            filter = calendar.filter,
            monthlySummary = CalendarMonthlySummaryUiModel(
                confirmedCount = calendar.monthlySummary.confirmedCount,
                inProgressCount = calendar.monthlySummary.inProgressCount,
                myActionRequiredCount = calendar.monthlySummary.myActionRequiredCount,
            ),
            dayItems = calendar.dayItems.map {
                CalendarDayUiModel(
                    date = it.date,
                    isToday = it.isToday,
                    isSelected = it.isSelected,
                    indicators = it.indicators,
                )
            },
            selectedDateMeetings = calendar.selectedDateMeetings.map {
                CalendarMeetingItemUiModel(
                    roomId = it.roomId,
                    title = it.title,
                    statusText = it.statusText,
                    dateText = it.dateText,
                    timeText = it.timeText,
                    placeText = it.placeText,
                    participantText = it.participantText,
                    responseText = it.responseText,
                    actionType = it.actionType,
                    ctaText = it.ctaText,
                    isConfirmed = it.isConfirmed,
                )
            },
        )
    }

    fun roomUiState(roomId: String): MeetingRoomUiState? {
        revision
        val room = repository.getRoom(roomId) ?: return null
        val participants = repository.getParticipants(room.id)
        val currentId = repository.getCurrentParticipantId(room.id) ?: return null
        val currentParticipant = participants.firstOrNull { it.id == currentId } ?: return null
        val availabilities = repository.getAvailabilities(room.id)
        val summaries = aggregateAvailability(room, participants, availabilities)
        val startLocations = repository.getStartLocations(room.id)
        val preferences = repository.getTravelPreferences(room.id)
        val placeVotes = repository.getPlaceVotes(room.id)
        return MeetingRoomUiState(
            room = room,
            participants = participants,
            currentParticipant = currentParticipant,
            selectedAvailability = availabilityDraft,
            summaries = summaries,
            recommendations = recommendDates(room, participants, availabilities),
            selectedSummary = summaries.firstOrNull { it.date == selectedDate },
            startLocations = startLocations,
            currentStartLocation = startLocations.firstOrNull { it.participantId == currentId },
            currentTransportMode = preferences.firstOrNull { it.participantId == currentId }?.transportMode
                ?: TransportMode.UNKNOWN,
            areaRecommendations = repository.getAreaRecommendations(room.id),
            locationSearchResults = locationSearchResults,
            isCalculatingAreas = isCalculatingAreas,
            placeCandidates = sortPlacesByVotes(repository.getPlaceCandidates(room.id), placeVotes),
            placeVotes = placeVotes,
            isSearchingPlaces = isSearchingPlaces,
        )
    }

    fun dispatch(action: UiAction) {
        when (action) {
            UiAction.OpenHome -> navigateTo(AppRoute.Home)
            UiAction.OpenCalendar -> navigateTo(AppRoute.Calendar)
            UiAction.OpenMyPage -> navigateTo(AppRoute.MyPage)
            UiAction.OpenCreateRoom -> navigateTo(AppRoute.CreateRoom)
            is UiAction.OpenJoinRoom -> navigateTo(AppRoute.JoinRoom(action.roomCode))
            is UiAction.OpenRoom -> openRoom(action.roomId)
            is UiAction.SelectCalendarDate -> calendarSelectedDate = action.date
            UiAction.OpenPreviousCalendarMonth -> {
                calendarMonth = calendarMonth.previous()
                calendarSelectedDate = calendarMonth.firstDay
            }
            UiAction.OpenNextCalendarMonth -> {
                calendarMonth = calendarMonth.next()
                calendarSelectedDate = calendarMonth.firstDay
            }
            UiAction.OpenTodayCalendarMonth -> {
                val today = currentLocalDate()
                calendarMonth = CalendarMonth.from(today)
                calendarSelectedDate = today
            }
            is UiAction.ChangeCalendarFilter -> calendarFilter = action.filter
            UiAction.NavigateBack -> navigateBack()
        }
    }

    suspend fun createRoom(input: CreateRoomInput) {
        if (input.hostNickname.isBlank() || input.title.isBlank()) {
            emitMessage("방장 닉네임과 약속 이름을 입력해주세요.")
            return
        }
        if (input.endDate < input.startDate) {
            emitMessage("종료일은 시작일보다 빠를 수 없습니다.")
            return
        }
        if (input.startDate < currentLocalDate()) {
            emitMessage("시작일은 오늘 또는 이후 날짜로 선택해주세요.")
            return
        }
        if (input.maxParticipants < 2) {
            emitMessage("최대 인원은 2명 이상으로 설정해주세요.")
            return
        }
        val now = Clock.System.now()
        val roomId = generateRoomCode()
        val hostId = generateId("host")
        val room = MeetingRoom(
            id = roomId,
            title = input.title.trim(),
            description = input.description.trim().ifBlank { null },
            meetingType = input.meetingType,
            dateRangeStart = input.startDate,
            dateRangeEnd = input.endDate,
            minParticipants = input.minParticipants.coerceAtLeast(1),
            maxParticipants = input.maxParticipants.coerceAtLeast(2),
            responseDeadline = input.responseDeadline,
            hostParticipantId = hostId,
            requiredParticipantIds = if (input.hostIsRequired) listOf(hostId) else emptyList(),
            status = MeetingStatus.COLLECTING_AVAILABILITY,
            createdAt = now,
            updatedAt = now,
        )
        val host = Participant(
            id = hostId,
            roomId = roomId,
            nickname = input.hostNickname.trim(),
            isHost = true,
            isRequired = input.hostIsRequired,
            joinedAt = now,
        )
        runCatching { repository.createRoom(room, host) }
            .onSuccess {
                revision++
                openRoom(roomId, replaceCurrent = true)
                emitMessage("약속방을 만들었습니다. 코드를 공유해 참여자를 초대하세요.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("약속방을 만들지 못했습니다.")) }
    }

    suspend fun joinRoom(roomCode: String, nickname: String) {
        val room = repository.getRoomForJoin(roomCode)
        if (room == null) {
            emitMessage("방 코드를 확인해주세요.")
            return
        }
        if (nickname.isBlank()) {
            emitMessage("닉네임을 입력해주세요.")
            return
        }
        val participant = Participant(
            id = generateId("guest"),
            roomId = room.id,
            nickname = nickname.trim(),
            isHost = false,
            isRequired = false,
            joinedAt = Clock.System.now(),
        )
        runCatching { repository.joinRoom(participant) }
            .onSuccess {
                revision++
                openRoom(room.id, replaceCurrent = true)
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("참여하지 못했습니다.")) }
    }

    suspend fun leaveRoom(roomId: String) {
        val participantId = repository.getCurrentParticipantId(roomId) ?: return
        runCatching { repository.leaveRoom(roomId, participantId) }
            .onSuccess {
                revision++
                backStack = emptyList()
                route = AppRoute.Home
                emitMessage("약속방에서 나갔습니다.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("약속방에서 나가지 못했습니다.")) }
    }

    suspend fun deleteRoom(roomId: String) {
        val room = repository.getRoom(roomId) ?: return
        if (repository.getCurrentParticipantId(roomId) != room.hostParticipantId) {
            emitMessage("방장만 약속을 삭제할 수 있습니다.")
            return
        }
        runCatching { repository.deleteRoom(roomId) }
            .onSuccess {
                revision++
                backStack = emptyList()
                route = AppRoute.Home
                emitMessage("약속을 삭제했습니다.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("약속을 삭제하지 못했습니다.")) }
    }

    suspend fun refreshRoom(roomId: String) {
        runCatching { repository.refreshRoom(roomId) }
            .onSuccess { revision++ }
    }

    fun handleCurrentRoomUnavailable(roomId: String) {
        if (route != AppRoute.MeetingRoom(roomId)) return
        backStack = emptyList()
        route = AppRoute.Home
        revision++
        emitMessage("약속방 정보를 찾을 수 없어요.")
    }

    suspend fun collectRoomUpdates(roomId: String) {
        repository.observeRoom(roomId).collect {
            revision++
        }
    }

    suspend fun refreshDashboard() {
        val now = Clock.System.now()
        val lastRefresh = lastManualRefreshAt
        if (lastRefresh != null && now - lastRefresh < MANUAL_REFRESH_COOLDOWN) {
            val remainingSeconds = (MANUAL_REFRESH_COOLDOWN - (now - lastRefresh)).inWholeSeconds.coerceAtLeast(1)
            emitMessage("새로고침은 ${remainingSeconds}초 후에 다시 할 수 있어요.")
            return
        }
        runCatching { repository.refreshRooms() }
            .onSuccess {
                lastManualRefreshAt = now
                revision++
                emitMessage("최신 약속 정보를 불러왔어요.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("약속 정보를 새로고침하지 못했습니다.")) }
    }

    fun cycleAvailability(date: LocalDate) {
        val next = cycleStatus(availabilityDraft[date])
        availabilityDraft = if (next == null) availabilityDraft - date else availabilityDraft + (date to next)
    }

    suspend fun saveAvailability(roomId: String) {
        val participantId = repository.getCurrentParticipantId(roomId) ?: return
        runCatching { repository.saveAvailabilities(roomId, participantId, availabilityDraft) }
            .onSuccess {
                revision++
                emitMessage("가능한 날짜를 저장했습니다.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("가능한 날짜를 저장하지 못했습니다.")) }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    suspend fun confirmDate(roomId: String, date: LocalDate) {
        val room = repository.getRoom(roomId) ?: return
        if (repository.getCurrentParticipantId(roomId) != room.hostParticipantId) {
            emitMessage("방장만 날짜를 확정할 수 있습니다.")
            return
        }
        runCatching { repository.confirmDate(roomId, date) }
            .onSuccess {
                eventPublisher.publish(MeetingEvent.DateConfirmed(roomId, date, Clock.System.now()))
                revision++
                emitMessage("약속 날짜를 확정했습니다.")
            }
            .onFailure { emitMessage(it.meetingRepositoryErrorMessage("약속 날짜를 확정하지 못했습니다.")) }
    }

    suspend fun searchLocations(query: String) {
        locationSearchResults = locationSearchProvider.search(query)
    }

    suspend fun useCurrentLocation(roomId: String) {
        val result = locationSearchProvider.getCurrentLocation()
        if (result == null) {
            emitMessage("현재 위치를 가져오지 못했습니다.")
            return
        }
        saveStartLocation(roomId, result)
        emitMessage("현재 위치 샘플을 저장했습니다.")
    }

    fun saveStartLocation(roomId: String, result: LocationSearchResult) {
        val participantId = repository.getCurrentParticipantId(roomId) ?: return
        repository.saveStartLocation(
            UserStartLocation(
                participantId = participantId,
                roomId = roomId,
                label = result.label,
                address = result.address,
                latitude = result.point.latitude,
                longitude = result.point.longitude,
                privacyLevel = LocationPrivacyLevel.EXACT_PRIVATE,
                updatedAt = Clock.System.now(),
            ),
        )
        locationSearchResults = emptyList()
        revision++
        emitMessage("출발 위치를 저장했습니다.")
    }

    fun saveTransportMode(roomId: String, transportMode: TransportMode) {
        val participantId = repository.getCurrentParticipantId(roomId) ?: return
        repository.saveTransportMode(roomId, participantId, transportMode)
        revision++
    }

    suspend fun calculateAreaRecommendations(
        roomId: String,
        candidates: List<com.garam.whenwheremeet.domain.model.MeetingAreaCandidate>,
    ) {
        val room = repository.getRoom(roomId) ?: return
        if (room.confirmedDate == null) {
            emitMessage("날짜를 먼저 확정해주세요.")
            return
        }
        val locations = repository.getStartLocations(roomId)
        val participants = repository.getParticipants(roomId)
        if (locations.size < participants.size) {
            emitMessage("모든 참여자가 출발 위치를 입력한 뒤 계산할 수 있습니다.")
            return
        }
        isCalculatingAreas = true
        try {
            val recommendations = recommendMeetingAreas(
                meetingType = room.meetingType,
                participants = participants,
                locations = locations,
                preferences = repository.getTravelPreferences(roomId),
                candidates = candidates,
            )
            repository.saveAreaRecommendations(roomId, recommendations)
            revision++
            emitMessage("추천 지역 TOP 3를 계산했습니다.")
        } finally {
            isCalculatingAreas = false
        }
    }

    fun selectAreaCandidate(roomId: String, candidateId: String) {
        val room = repository.getRoom(roomId) ?: return
        if (repository.getCurrentParticipantId(roomId) != room.hostParticipantId) {
            emitMessage("방장만 후보 지역을 선택할 수 있습니다.")
            return
        }
        repository.selectAreaCandidate(roomId, candidateId)
        revision++
        emitMessage("후보 지역을 선택했습니다.")
    }

    suspend fun searchPlaceCandidates(roomId: String) {
        val room = repository.getRoom(roomId) ?: return
        val selectedAreaId = room.selectedAreaCandidateId
        if (selectedAreaId == null) {
            emitMessage("추천 지역을 먼저 선택해주세요.")
            return
        }
        val selectedArea = repository.getAreaRecommendations(roomId)
            .firstOrNull { it.candidate.id == selectedAreaId }
        if (selectedArea == null) {
            emitMessage("선택한 추천 지역 정보를 찾을 수 없습니다.")
            return
        }
        isSearchingPlaces = true
        try {
            val places = placeSearchProvider.searchPlaces(
                center = selectedArea.candidate.point,
                meetingType = room.meetingType,
                radiusMeters = 1500,
                limit = 10,
            )
            val scored = scorePlaces(
                places = places,
                center = selectedArea.candidate.point,
                meetingType = room.meetingType,
                selectedAreaRecommendation = selectedArea,
            )
            repository.savePlaceCandidates(roomId, scored)
            revision++
            emitMessage("장소 후보를 찾았습니다.")
        } finally {
            isSearchingPlaces = false
        }
    }

    fun votePlace(roomId: String, placeId: String, voteType: PlaceVoteType) {
        val participantId = repository.getCurrentParticipantId(roomId) ?: return
        repository.savePlaceVote(roomId, placeId, participantId, voteType)
        revision++
    }

    fun confirmPlace(roomId: String, place: PlaceCandidate) {
        val room = repository.getRoom(roomId) ?: return
        if (repository.getCurrentParticipantId(roomId) != room.hostParticipantId) {
            emitMessage("방장만 최종 장소를 확정할 수 있습니다.")
            return
        }
        if (repository.getPlaceCandidates(roomId).none { it.place.id == place.id }) {
            emitMessage("현재 장소 후보에 포함되지 않은 장소입니다.")
            return
        }
        repository.confirmPlace(roomId, place)
        revision++
        emitMessage("최종 약속 장소를 확정했습니다.")
    }

    fun openMap(place: PlaceCandidate) {
        if (place.mapUrl == null) {
            emitMessage("열 수 있는 지도 링크가 없습니다.")
        } else {
            event = UiEvent.OpenMap(place)
        }
    }

    fun openConfirmedPlaceMap(roomId: String) {
        val place = repository.getRoom(roomId)?.confirmedPlace
        if (place == null) {
            emitMessage("확정된 장소가 아직 없습니다.")
        } else {
            openMap(place)
        }
    }

    fun requestShare(roomId: String) {
        val room = repository.getRoom(roomId) ?: return
        val participantCount = repository.getParticipants(roomId).size
        val text = if (room.confirmedDate != null) {
            buildConfirmedShareText(room, participantCount)
        } else {
            "[약속 조율 요청]\n약속명: ${room.title}\n가능한 날짜를 선택해주세요.\n\n참여하기:\n${buildRoomJoinLink(room.id)}\n방 코드: ${room.id}\n\n앱이 없다면 설치 후 방 코드를 입력해주세요."
        }
        event = UiEvent.Share(text)
    }

    fun consumeEvent() {
        event = null
    }

    private fun openRoom(roomId: String, replaceCurrent: Boolean = false) {
        val participantId = repository.getCurrentParticipantId(roomId)
        if (participantId == null) {
            navigateTo(AppRoute.JoinRoom(roomId), replaceCurrent = replaceCurrent)
            return
        }
        availabilityDraft = repository.getAvailabilities(roomId)
            .filter { it.participantId == participantId }
            .associate { it.date to it.status }
        selectedDate = null
        locationSearchResults = emptyList()
        navigateTo(AppRoute.MeetingRoom(roomId), replaceCurrent = replaceCurrent)
    }

    private fun navigateTo(nextRoute: AppRoute, replaceCurrent: Boolean = false) {
        if (route == nextRoute) return
        backStack = if (replaceCurrent) {
            backStack
        } else {
            (backStack + route).takeLast(MAX_BACK_STACK_SIZE)
        }
        route = nextRoute
    }

    private fun navigateBack() {
        val previousRoute = backStack.lastOrNull()
        if (previousRoute == null) {
            route = AppRoute.Home
            return
        }
        backStack = backStack.dropLast(1)
        route = previousRoute
    }

    private fun emitMessage(message: String) {
        event = UiEvent.Message(message)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        do {
            code = (1..6).joinToString("") { chars[Random.nextInt(chars.length)].toString() }
        } while (repository.getRoom(code) != null)
        return code
    }

    private fun generateId(prefix: String): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(1000, 9999)}"

    private fun meetingOverviews(): List<MeetingOverview> = repository.getRooms().map { room ->
        MeetingOverview(
            room = room,
            participants = repository.getParticipants(room.id),
            currentParticipantId = repository.getCurrentParticipantId(room.id),
            availabilities = repository.getAvailabilities(room.id),
            startLocations = repository.getStartLocations(room.id),
            areaRecommendations = repository.getAreaRecommendations(room.id),
            placeCandidates = repository.getPlaceCandidates(room.id),
            placeVotes = repository.getPlaceVotes(room.id),
        )
    }

    private companion object {
        val MANUAL_REFRESH_COOLDOWN = 30.seconds
        const val MAX_BACK_STACK_SIZE = 20
    }
}

private fun com.garam.whenwheremeet.domain.usecase.HomeMeetingCard.toUiModel(): HomeMeetingCardUiModel =
    HomeMeetingCardUiModel(
        roomId = roomId,
        title = title,
        statusText = statusText,
        dateText = dateText,
        timeText = timeText,
        placeText = placeText,
        participantText = participantText,
        responseText = responseText,
        travelTimeText = travelTimeText,
        actionType = actionType,
        ctaText = ctaText,
        isConfirmed = isConfirmed,
    )

private fun Throwable.meetingRepositoryErrorMessage(fallback: String): String {
    val rawMessage = message.orEmpty()
    return when {
        rawMessage.contains("database (default) does not exist", ignoreCase = true) ||
            rawMessage.contains("NOT_FOUND", ignoreCase = true) ->
            "Firestore 데이터베이스를 찾지 못했어요. Firebase 프로젝트와 데이터베이스 ID 설정을 확인해주세요."
        rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Firestore 접근 권한이 없어요. 로그인 상태와 Firestore 보안 규칙을 확인해주세요."
        rawMessage.isNotBlank() -> rawMessage
        else -> fallback
    }
}

fun LocalDate.toKoreanDate(): String {
    val weekday = when (dayOfWeek.ordinal) {
        0 -> "월요일"
        1 -> "화요일"
        2 -> "수요일"
        3 -> "목요일"
        4 -> "금요일"
        5 -> "토요일"
        else -> "일요일"
    }
    return "${year}년 ${monthNumber}월 ${dayOfMonth}일 $weekday"
}
