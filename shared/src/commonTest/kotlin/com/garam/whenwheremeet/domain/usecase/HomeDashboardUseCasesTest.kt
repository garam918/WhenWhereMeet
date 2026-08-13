package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.UserStartLocation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HomeDashboardUseCasesTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val today = LocalDate(2026, 6, 16)
    private val me = participant("me")

    @Test
    fun summaryCountsConfirmedPendingAndPlaceVoteRequiredMeetings() {
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = listOf(
                overview(room("confirmed", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 18))),
                overview(room("availability", MeetingStatus.COLLECTING_AVAILABILITY)),
                overview(room("location", MeetingStatus.DATE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 20))),
                overview(room("vote", MeetingStatus.PLACE_SELECTING, confirmedDate = LocalDate(2026, 6, 21))),
            ),
            today = today,
        )

        assertEquals(1, dashboard.summary.confirmedCount)
        assertEquals(2, dashboard.summary.pendingResponseCount)
        assertEquals(1, dashboard.summary.placeVoteRequiredCount)
    }

    @Test
    fun actionItemsOnlyIncludeMeetingsThatNeedMyInput() {
        val withAvailability = overview(
            room = room("done", MeetingStatus.COLLECTING_AVAILABILITY),
            availabilities = listOf(availability("done")),
        )
        val needsAvailability = overview(room("todo", MeetingStatus.COLLECTING_AVAILABILITY))

        val dashboard = BuildHomeDashboardUseCase()(listOf(withAvailability, needsAvailability), today)

        assertEquals(listOf("todo"), dashboard.actionItems.map { it.roomId })
        assertEquals(HomeActionType.INPUT_AVAILABILITY, dashboard.actionItems.single().actionType)
    }

    @Test
    fun meetingsWithoutCurrentParticipantAreExcludedFromDashboard() {
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = listOf(
                overview(room("joined", MeetingStatus.COLLECTING_AVAILABILITY)),
                overview(room("left", MeetingStatus.COLLECTING_AVAILABILITY), currentParticipantId = null),
            ),
            today = today,
        )

        assertEquals(listOf("joined"), dashboard.inProgressMeetings.map { it.roomId })
        assertEquals(listOf("joined"), dashboard.actionItems.map { it.roomId })
    }

    @Test
    fun upcomingConfirmedMeetingsAreSortedByNearestDateWithoutDroppingMeetings() {
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = listOf(
                overview(room("d", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 25))),
                overview(room("a", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 17))),
                overview(room("c", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 20))),
                overview(room("b", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 19))),
            ),
            today = today,
        )

        assertEquals(listOf("a", "b", "c", "d"), dashboard.upcomingConfirmedMeetings.map { it.roomId })
    }

    @Test
    fun summaryCountsUpcomingConfirmedMeetingsBeyondSevenDays() {
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = listOf(
                overview(room("later", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 7, 20))),
                overview(room("past", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 15))),
            ),
            today = today,
        )

        assertEquals(1, dashboard.summary.confirmedCount)
    }

    @Test
    fun meetingConfirmedWithoutPlaceAppearsAsUpcomingConfirmed() {
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = listOf(
                overview(room("no-place", MeetingStatus.MEETING_CONFIRMED, confirmedDate = LocalDate(2026, 6, 18))),
            ),
            today = today,
        )

        assertEquals(1, dashboard.summary.confirmedCount)
        assertEquals(listOf("no-place"), dashboard.upcomingConfirmedMeetings.map { it.roomId })
        assertEquals("장소 미정", dashboard.upcomingConfirmedMeetings.single().placeText)
    }

    @Test
    fun pastAndAllInProgressMeetingsRemainAvailableAfterAccountRestore() {
        val inProgress = (1..4).map { index ->
            overview(room("working-$index", MeetingStatus.COLLECTING_AVAILABILITY))
        }
        val dashboard = BuildHomeDashboardUseCase()(
            meetings = inProgress + overview(
                room("past", MeetingStatus.PLACE_CONFIRMED, confirmedDate = LocalDate(2026, 6, 15)),
            ),
            today = today,
        )

        assertEquals(4, dashboard.inProgressMeetings.size)
        assertEquals(listOf("past"), dashboard.pastMeetings.map { it.roomId })
    }

    private fun overview(
        room: MeetingRoom,
        availabilities: List<Availability> = emptyList(),
        startLocations: List<UserStartLocation> = emptyList(),
        currentParticipantId: String? = me.id,
    ) = MeetingOverview(
        room = room,
        participants = listOf(me),
        currentParticipantId = currentParticipantId,
        availabilities = availabilities,
        startLocations = startLocations,
        areaRecommendations = emptyList(),
        placeCandidates = emptyList(),
        placeVotes = emptyList(),
    )

    private fun room(id: String, status: MeetingStatus, confirmedDate: LocalDate? = null) = MeetingRoom(
        id = id,
        title = "약속 $id",
        meetingType = MeetingType.CAFE,
        dateRangeStart = LocalDate(2026, 6, 17),
        dateRangeEnd = LocalDate(2026, 6, 19),
        minParticipants = 1,
        responseDeadline = LocalDate(2026, 6, 17),
        hostParticipantId = me.id,
        status = status,
        confirmedDate = confirmedDate,
        createdAt = now,
        updatedAt = now,
    )

    private fun participant(id: String) = Participant(
        id = id,
        roomId = "room",
        nickname = id,
        isHost = true,
        joinedAt = now,
    )

    private fun availability(roomId: String) = Availability(
        roomId = roomId,
        participantId = me.id,
        date = LocalDate(2026, 6, 17),
        status = AvailabilityStatus.AVAILABLE,
        updatedAt = now,
    )

    @Suppress("unused")
    private fun startLocation(roomId: String) = UserStartLocation(
        participantId = me.id,
        roomId = roomId,
        label = "강남",
        latitude = 37.5,
        longitude = 127.0,
        privacyLevel = LocationPrivacyLevel.EXACT_PRIVATE,
        updatedAt = now,
    )
}
