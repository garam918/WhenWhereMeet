package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class LocalMeetingRepository(
    private val storage: KeyValueStorage,
) : MeetingRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var snapshot = storage.getString(STORAGE_KEY)
        ?.let { runCatching { json.decodeFromString<AppSnapshot>(it) }.getOrNull() }
        ?: AppSnapshot()

    override fun getRooms(): List<MeetingRoom> = snapshot.rooms.sortedByDescending { it.updatedAt }

    override fun getRoom(roomIdOrCode: String): MeetingRoom? = snapshot.rooms.firstOrNull {
        it.id.equals(roomIdOrCode.trim(), ignoreCase = true)
    }

    override fun getParticipants(roomId: String): List<Participant> =
        snapshot.participants.filter { it.roomId == roomId }.sortedBy { it.joinedAt }

    override fun getAvailabilities(roomId: String): List<Availability> =
        snapshot.availabilities.filter { it.roomId == roomId }

    override fun getCurrentParticipantId(roomId: String): String? = snapshot.currentParticipantIds[roomId]

    override fun getStartLocations(roomId: String): List<UserStartLocation> =
        snapshot.startLocations.filter { it.roomId == roomId }

    override fun getTravelPreferences(roomId: String): List<ParticipantTravelPreference> =
        snapshot.travelPreferences.filter { it.roomId == roomId }

    override fun getAreaRecommendations(roomId: String): List<AreaRecommendation> =
        snapshot.areaRecommendations[roomId].orEmpty()

    override fun getPlaceCandidates(roomId: String): List<ScoredPlaceCandidate> =
        snapshot.placeCandidates[roomId].orEmpty()

    override fun getPlaceVotes(roomId: String): List<PlaceVote> =
        snapshot.placeVotes.filter { it.roomId == roomId }

    override fun createRoom(room: MeetingRoom, host: Participant) {
        require(snapshot.rooms.none { it.id.equals(room.id, ignoreCase = true) }) { "이미 존재하는 방 코드입니다." }
        snapshot = snapshot.copy(
            rooms = snapshot.rooms + room,
            participants = snapshot.participants + host,
            currentParticipantIds = snapshot.currentParticipantIds + (room.id to host.id),
        )
        persist()
    }

    override fun joinRoom(participant: Participant) {
        val duplicate = snapshot.participants.any {
            it.roomId == participant.roomId && it.nickname.equals(participant.nickname, ignoreCase = true)
        }
        require(!duplicate) { "이미 사용 중인 닉네임입니다." }
        snapshot = snapshot.copy(
            participants = snapshot.participants + participant,
            currentParticipantIds = snapshot.currentParticipantIds + (participant.roomId to participant.id),
        )
        touchRoom(participant.roomId)
        persist()
    }

    override fun saveAvailabilities(
        roomId: String,
        participantId: String,
        values: Map<LocalDate, AvailabilityStatus>,
    ) {
        val now = Clock.System.now()
        val retained = snapshot.availabilities.filterNot {
            it.roomId == roomId && it.participantId == participantId
        }
        val replacements = values.map { (date, status) ->
            Availability(roomId, participantId, date, status, now)
        }
        snapshot = snapshot.copy(availabilities = retained + replacements)
        touchRoom(roomId)
        persist()
    }

    override fun confirmDate(roomId: String, date: LocalDate) {
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map { room ->
            if (room.id == roomId) room.copy(
                status = MeetingStatus.DATE_CONFIRMED,
                confirmedDate = date,
                updatedAt = now,
            ) else room
        })
        persist()
    }

    override fun saveStartLocation(location: UserStartLocation) {
        snapshot = snapshot.copy(
            startLocations = snapshot.startLocations.filterNot {
                it.roomId == location.roomId && it.participantId == location.participantId
            } + location,
        )
        invalidateAreaRecommendations(location.roomId)
        touchRoom(location.roomId)
        persist()
    }

    override fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode) {
        val preference = ParticipantTravelPreference(
            participantId = participantId,
            roomId = roomId,
            transportMode = transportMode,
            updatedAt = Clock.System.now(),
        )
        snapshot = snapshot.copy(
            travelPreferences = snapshot.travelPreferences.filterNot {
                it.roomId == roomId && it.participantId == participantId
            } + preference,
        )
        invalidateAreaRecommendations(roomId)
        touchRoom(roomId)
        persist()
    }

    override fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>) {
        snapshot = snapshot.copy(areaRecommendations = snapshot.areaRecommendations + (roomId to recommendations))
        touchRoom(roomId)
        persist()
    }

    override fun selectAreaCandidate(roomId: String, candidateId: String) {
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map { room ->
            if (room.id == roomId) room.copy(
                selectedAreaCandidateId = candidateId,
                status = MeetingStatus.PLACE_SELECTING,
                confirmedPlace = null,
                updatedAt = now,
            ) else room
        }, placeCandidates = snapshot.placeCandidates - roomId, placeVotes = snapshot.placeVotes.filterNot { it.roomId == roomId })
        persist()
    }

    override fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>) {
        snapshot = snapshot.copy(
            placeCandidates = snapshot.placeCandidates + (roomId to candidates),
            placeVotes = snapshot.placeVotes.filterNot { it.roomId == roomId },
        )
        touchRoom(roomId)
        persist()
    }

    override fun savePlaceVote(
        roomId: String,
        placeId: String,
        participantId: String,
        voteType: PlaceVoteType,
    ) {
        val vote = PlaceVote(roomId, placeId, participantId, voteType, Clock.System.now())
        snapshot = snapshot.copy(placeVotes = snapshot.placeVotes.filterNot {
            it.roomId == roomId && it.placeId == placeId && it.participantId == participantId
        } + vote)
        touchRoom(roomId)
        persist()
    }

    override fun confirmPlace(roomId: String, place: PlaceCandidate) {
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map { room ->
            if (room.id == roomId) room.copy(
                status = MeetingStatus.PLACE_CONFIRMED,
                confirmedPlace = place,
                updatedAt = now,
            ) else room
        })
        persist()
    }

    private fun touchRoom(roomId: String) {
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map { room ->
            if (room.id == roomId) room.copy(updatedAt = now) else room
        })
    }

    private fun invalidateAreaRecommendations(roomId: String) {
        snapshot = snapshot.copy(
            areaRecommendations = snapshot.areaRecommendations - roomId,
            rooms = snapshot.rooms.map { room ->
                if (room.id == roomId && room.confirmedDate != null) {
                    room.copy(
                        selectedAreaCandidateId = null,
                        confirmedPlace = null,
                        status = MeetingStatus.DATE_CONFIRMED,
                    )
                } else room
            },
            placeCandidates = snapshot.placeCandidates - roomId,
            placeVotes = snapshot.placeVotes.filterNot { it.roomId == roomId },
        )
    }

    private fun persist() {
        storage.putString(STORAGE_KEY, json.encodeToString(snapshot))
    }

    @Serializable
    private data class AppSnapshot(
        val rooms: List<MeetingRoom> = emptyList(),
        val participants: List<Participant> = emptyList(),
        val availabilities: List<Availability> = emptyList(),
        val currentParticipantIds: Map<String, String> = emptyMap(),
        val startLocations: List<UserStartLocation> = emptyList(),
        val travelPreferences: List<ParticipantTravelPreference> = emptyList(),
        val areaRecommendations: Map<String, List<AreaRecommendation>> = emptyMap(),
        val placeCandidates: Map<String, List<ScoredPlaceCandidate>> = emptyMap(),
        val placeVotes: List<PlaceVote> = emptyList(),
    )

    private companion object {
        const val STORAGE_KEY = "meeting_mvp_snapshot_v1"
    }
}
