package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.model.isMeetingConfirmed
import kotlinx.datetime.LocalDate

enum class HomeActionType {
    INPUT_AVAILABILITY,
    INPUT_START_LOCATION,
    VOTE_PLACE,
    CONFIRM_MEETING,
    VIEW_CONFIRMED,
    NONE,
}

data class MeetingOverview(
    val room: MeetingRoom,
    val participants: List<Participant>,
    val currentParticipantId: String?,
    val availabilities: List<Availability>,
    val startLocations: List<UserStartLocation>,
    val areaRecommendations: List<AreaRecommendation>,
    val placeCandidates: List<ScoredPlaceCandidate>,
    val placeVotes: List<PlaceVote>,
    val destinationStationProposals: List<DestinationStationProposal> = emptyList(),
    val destinationStationVotes: List<DestinationStationVote> = emptyList(),
)

data class HomeDashboard(
    val summary: HomeSummary,
    val actionItems: List<HomeActionItem>,
    val upcomingConfirmedMeetings: List<HomeMeetingCard>,
    val inProgressMeetings: List<HomeMeetingCard>,
    val pastMeetings: List<HomeMeetingCard>,
)

data class HomeSummary(
    val confirmedCount: Int,
    val pendingResponseCount: Int,
    val placeVoteRequiredCount: Int,
)

data class HomeActionItem(
    val roomId: String,
    val title: String,
    val actionType: HomeActionType,
    val statusText: String,
    val description: String,
    val dueText: String?,
    val ctaText: String,
)

data class HomeMeetingCard(
    val roomId: String,
    val title: String,
    val statusText: String,
    val dateText: String,
    val timeText: String?,
    val placeText: String?,
    val participantText: String,
    val responseText: String?,
    val travelTimeText: String?,
    val actionType: HomeActionType,
    val ctaText: String,
    val isConfirmed: Boolean,
)

class ResolveHomeActionTypeUseCase {
    operator fun invoke(meeting: MeetingOverview): HomeActionType {
        val participantId = meeting.currentParticipantId ?: return HomeActionType.NONE
        return when (meeting.room.status) {
            MeetingStatus.COLLECTING_AVAILABILITY -> {
                val hasAvailability = meeting.availabilities.any { it.participantId == participantId }
                if (hasAvailability) HomeActionType.NONE else HomeActionType.INPUT_AVAILABILITY
            }
            MeetingStatus.DATE_CONFIRMED -> {
                val hasStartLocation = meeting.startLocations.any { it.participantId == participantId }
                if (hasStartLocation) HomeActionType.NONE else HomeActionType.INPUT_START_LOCATION
            }
            MeetingStatus.PLACE_SELECTING -> {
                val validStationIds = meeting.destinationStationProposals.mapTo(mutableSetOf()) { it.station.id }
                val hasVote = meeting.destinationStationVotes.any {
                    it.participantId == participantId && it.stationId in validStationIds
                } ||
                    meeting.placeVotes.any { it.participantId == participantId }
                if (hasVote) HomeActionType.NONE else HomeActionType.VOTE_PLACE
            }
            MeetingStatus.PLACE_CONFIRMED,
            MeetingStatus.MEETING_CONFIRMED -> HomeActionType.VIEW_CONFIRMED
            MeetingStatus.DRAFT,
            MeetingStatus.CANCELLED -> HomeActionType.NONE
        }
    }
}

class BuildHomeDashboardUseCase(
    private val resolveActionType: ResolveHomeActionTypeUseCase = ResolveHomeActionTypeUseCase(),
) {
    operator fun invoke(
        meetings: List<MeetingOverview>,
        today: LocalDate,
    ): HomeDashboard {
        val joinedMeetings = meetings.filter { it.currentParticipantId != null }
        val activeMeetings = joinedMeetings.filter { it.room.status != MeetingStatus.CANCELLED }
        val enriched = activeMeetings.map { it to resolveActionType(it) }
        val actionItems = enriched
            .filter { (_, actionType) -> actionType != HomeActionType.NONE && actionType != HomeActionType.VIEW_CONFIRMED }
            .sortedWith(compareBy<Pair<MeetingOverview, HomeActionType>> { it.first.room.responseDeadline ?: it.first.room.confirmedDate ?: it.first.room.dateRangeEnd }
                .thenBy { it.first.room.updatedAt })
            .map { (meeting, actionType) -> meeting.toActionItem(actionType, today) }

        val upcoming = enriched
            .filter { (meeting, _) ->
                meeting.room.status.isMeetingConfirmed &&
                    meeting.room.confirmedDate != null &&
                    meeting.room.confirmedDate >= today
            }
            .sortedBy { it.first.room.confirmedDate }
            .map { (meeting, actionType) -> meeting.toMeetingCard(actionType) }

        val inProgress = enriched
            .filter { (meeting, _) ->
                !meeting.room.status.isMeetingConfirmed &&
                    meeting.room.status != MeetingStatus.CANCELLED
            }
            .sortedWith(compareBy<Pair<MeetingOverview, HomeActionType>> { it.first.room.responseDeadline ?: it.first.room.confirmedDate ?: it.first.room.dateRangeEnd }
                .thenByDescending { it.first.room.updatedAt })
            .map { (meeting, actionType) -> meeting.toMeetingCard(actionType) }

        val past = joinedMeetings
            .filter { meeting ->
                meeting.room.status == MeetingStatus.CANCELLED ||
                    (meeting.room.status.isMeetingConfirmed && meeting.room.confirmedDate?.let { it < today } == true)
            }
            .sortedByDescending { it.room.confirmedDate ?: it.room.dateRangeEnd }
            .map { meeting -> meeting.toMeetingCard(resolveActionType(meeting)) }

        val upcomingConfirmedCount = activeMeetings.count {
            it.room.status.isMeetingConfirmed &&
                it.room.confirmedDate != null &&
                it.room.confirmedDate >= today
        }
        val pendingResponseCount = enriched.count { it.second == HomeActionType.INPUT_AVAILABILITY || it.second == HomeActionType.INPUT_START_LOCATION }
        val placeVoteRequiredCount = enriched.count { it.second == HomeActionType.VOTE_PLACE }

        return HomeDashboard(
            summary = HomeSummary(
                confirmedCount = upcomingConfirmedCount,
                pendingResponseCount = pendingResponseCount,
                placeVoteRequiredCount = placeVoteRequiredCount,
            ),
            actionItems = actionItems,
            upcomingConfirmedMeetings = upcoming,
            inProgressMeetings = inProgress,
            pastMeetings = past,
        )
    }

    private fun MeetingOverview.toActionItem(actionType: HomeActionType, today: LocalDate): HomeActionItem =
        HomeActionItem(
            roomId = room.id,
            title = actionTitle(actionType),
            actionType = actionType,
            statusText = room.status.statusText(),
            description = room.title,
            dueText = dueText(today),
            ctaText = actionType.ctaText(),
        )

    private fun MeetingOverview.toMeetingCard(actionType: HomeActionType): HomeMeetingCard =
        HomeMeetingCard(
            roomId = room.id,
            title = room.title,
            statusText = room.status.statusText(),
            dateText = room.confirmedDate?.shortDateText() ?: "${room.dateRangeStart.shortDateText()}부터",
            timeText = null,
            placeText = room.confirmedPlace?.name ?: selectedAreaName() ?: "장소 미정",
            participantText = "참여자 ${participants.size}명",
            responseText = responseText(),
            travelTimeText = travelTimeText(),
            actionType = actionType,
            ctaText = actionType.ctaText(),
            isConfirmed = room.status.isMeetingConfirmed,
        )

    private fun MeetingOverview.responseText(): String? {
        if (room.status != MeetingStatus.COLLECTING_AVAILABILITY) return null
        val responded = availabilities.map { it.participantId }.distinct().size
        return "응답 $responded/${participants.size}명"
    }

    private fun MeetingOverview.selectedAreaName(): String? =
        areaRecommendations.firstOrNull { it.candidate.id == room.selectedAreaCandidateId }?.candidate?.displayName

    private fun MeetingOverview.travelTimeText(): String? =
        areaRecommendations.firstOrNull { it.candidate.id == room.selectedAreaCandidateId }
            ?.let { "평균 ${it.averageTravelMinutes}분" }

    private fun MeetingOverview.dueText(today: LocalDate): String? {
        val dueDate = room.responseDeadline ?: room.confirmedDate ?: room.dateRangeEnd
        val days = dueDate.toEpochDays().toLong() - today.toEpochDays().toLong()
        return when {
            days < 0L -> "마감 지남"
            days == 0L -> "오늘까지"
            days == 1L -> "내일까지"
            else -> "${days}일 남음"
        }
    }
}

fun MeetingStatus.statusText(): String = when (this) {
    MeetingStatus.DRAFT -> "준비 중"
    MeetingStatus.COLLECTING_AVAILABILITY -> "날짜 조율 중"
    MeetingStatus.DATE_CONFIRMED -> "날짜 확정"
    MeetingStatus.PLACE_SELECTING -> "장소 선택 중"
    MeetingStatus.PLACE_CONFIRMED -> "약속 확정"
    MeetingStatus.MEETING_CONFIRMED -> "약속 확정 · 장소 미정"
    MeetingStatus.CANCELLED -> "취소됨"
}

fun HomeActionType.ctaText(): String = when (this) {
    HomeActionType.INPUT_AVAILABILITY -> "날짜 입력하기"
    HomeActionType.INPUT_START_LOCATION -> "출발역 입력하기"
    HomeActionType.VOTE_PLACE -> "후보역 투표하기"
    HomeActionType.CONFIRM_MEETING -> "확정 확인하기"
    HomeActionType.VIEW_CONFIRMED -> "상세 보기"
    HomeActionType.NONE -> "약속방 보기"
}

fun actionTitle(actionType: HomeActionType): String = when (actionType) {
    HomeActionType.INPUT_AVAILABILITY -> "가능한 날짜를 알려주세요"
    HomeActionType.INPUT_START_LOCATION -> "출발역을 입력해주세요"
    HomeActionType.VOTE_PLACE -> "만나고 싶은 역에 투표해주세요"
    HomeActionType.CONFIRM_MEETING -> "최종 확정을 확인해주세요"
    HomeActionType.VIEW_CONFIRMED -> "확정된 약속을 확인하세요"
    HomeActionType.NONE -> "진행 상황을 확인하세요"
}

fun LocalDate.shortDateText(): String = "${monthNumber}월 ${dayOfMonth}일"
