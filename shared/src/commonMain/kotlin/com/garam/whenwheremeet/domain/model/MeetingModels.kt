package com.garam.whenwheremeet.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MeetingRoom(
    val id: String,
    val title: String,
    val description: String? = null,
    val meetingType: MeetingType,
    val dateRangeStart: LocalDate,
    val dateRangeEnd: LocalDate,
    val minParticipants: Int,
    val responseDeadline: LocalDate? = null,
    val hostParticipantId: String,
    val requiredParticipantIds: List<String> = emptyList(),
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
    val isRequired: Boolean,
    val joinedAt: Instant,
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
    CANCELLED,
}

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
    val allRequiredAvailable: Boolean,
) {
    val totalParticipants: Int = availableParticipants.size + maybeParticipants.size +
        unavailableParticipants.size + unansweredParticipants.size
}

data class RecommendedDate(
    val rank: Int,
    val summary: DateAvailabilitySummary,
    val reason: String,
)
