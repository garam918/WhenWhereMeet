package com.garam.whenwheremeet.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

const val MIN_MEETING_PARTICIPANTS = 2
const val MAX_MEETING_PARTICIPANTS = 8

@Serializable
data class MeetingRoom(
    val id: String,
    val title: String,
    val description: String? = null,
    val meetingType: MeetingType,
    val dateRangeStart: LocalDate,
    val dateRangeEnd: LocalDate,
    val minParticipants: Int,
    val maxParticipants: Int = minParticipants,
    val responseDeadline: LocalDate? = null,
    val hostParticipantId: String,
    val status: MeetingStatus,
    val confirmedDate: LocalDate? = null,
    val selectedAreaCandidateId: String? = null,
    val confirmedPlace: PlaceCandidate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class Participant(
    val id: String,
    val roomId: String,
    val nickname: String,
    val isHost: Boolean,
    val joinedAt: Instant,
    val accountId: String? = null,
    val isInvited: Boolean = false,
)

@Serializable
data class FriendProfile(
    val userId: String,
    val nickname: String,
    val sharedMeetingCount: Int,
    val lastMetAt: Instant,
)

@Serializable
data class Availability(
    val roomId: String,
    val participantId: String,
    val date: LocalDate,
    val status: AvailabilityStatus,
    val updatedAt: Instant,
)

@Serializable
enum class AvailabilityStatus {
    AVAILABLE,
    MAYBE,
    UNAVAILABLE,
}

@Serializable
enum class MeetingStatus {
    DRAFT,
    COLLECTING_AVAILABILITY,
    DATE_CONFIRMED,
    PLACE_SELECTING,
    PLACE_CONFIRMED,
    MEETING_CONFIRMED,
    CANCELLED,
}

val MeetingStatus.isMeetingConfirmed: Boolean
    get() = this == MeetingStatus.PLACE_CONFIRMED || this == MeetingStatus.MEETING_CONFIRMED

@Serializable
enum class MeetingType(val label: String) {
    MEAL("식사"),
    CAFE("카페"),
    DRINKS("술자리"),
    STUDY("스터디"),
    EXERCISE("운동"),
    OTHER("기타"),
}

data class DateAvailabilitySummary(
    val date: LocalDate,
    val availableParticipants: List<Participant>,
    val maybeParticipants: List<Participant>,
    val unavailableParticipants: List<Participant>,
    val unansweredParticipants: List<Participant>,
    val score: Int,
) {
    val totalParticipants: Int = availableParticipants.size + maybeParticipants.size +
        unavailableParticipants.size + unansweredParticipants.size
}

data class RecommendedDate(
    val rank: Int,
    val summary: DateAvailabilitySummary,
    val reason: String,
)
