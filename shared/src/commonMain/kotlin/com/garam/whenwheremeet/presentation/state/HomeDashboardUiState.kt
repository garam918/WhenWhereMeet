package com.garam.whenwheremeet.presentation.state

import com.garam.whenwheremeet.domain.usecase.HomeActionType

data class HomeDashboardUiState(
    val isLoading: Boolean = false,
    val summary: HomeSummaryUiModel = HomeSummaryUiModel(),
    val actionItems: List<HomeActionItemUiModel> = emptyList(),
    val upcomingConfirmedMeetings: List<HomeMeetingCardUiModel> = emptyList(),
    val inProgressMeetings: List<HomeMeetingCardUiModel> = emptyList(),
    val errorMessage: String? = null,
)

data class HomeSummaryUiModel(
    val confirmedCount: Int = 0,
    val pendingResponseCount: Int = 0,
    val placeVoteRequiredCount: Int = 0,
)

data class HomeActionItemUiModel(
    val roomId: String,
    val title: String,
    val actionType: HomeActionType,
    val statusText: String,
    val description: String,
    val dueText: String?,
    val ctaText: String,
)

data class HomeMeetingCardUiModel(
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
