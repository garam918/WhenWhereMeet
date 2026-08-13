package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.FriendProfile
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MAX_MEETING_PARTICIPANTS
import com.garam.whenwheremeet.domain.model.MIN_MEETING_PARTICIPANTS
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.PlaceSource
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

    override fun getFriends(): List<FriendProfile> = snapshot.friends.sortedWith(
        compareByDescending<FriendProfile> { it.lastMetAt }.thenBy { it.nickname.lowercase() },
    )

    fun importRoom(room: MeetingRoom, participants: List<Participant>, currentParticipantId: String?) {
        snapshot = snapshot.copy(
            rooms = snapshot.rooms.filterNot { it.id == room.id } + room,
            participants = snapshot.participants.filterNot { it.roomId == room.id } + participants,
            currentParticipantIds = if (currentParticipantId == null) {
                snapshot.currentParticipantIds - room.id
            } else {
                snapshot.currentParticipantIds + (room.id to currentParticipantId)
            },
        )
        persist()
    }

    fun importRoomMetadata(room: MeetingRoom) {
        snapshot = snapshot.copy(
            rooms = snapshot.rooms.filterNot { it.id == room.id } + room,
        )
        persist()
    }

    fun importParticipants(roomId: String, participants: List<Participant>) {
        snapshot = snapshot.copy(
            participants = snapshot.participants.filterNot { it.roomId == roomId } + participants,
        )
        persist()
    }

    fun importAvailabilities(roomId: String, availabilities: List<Availability>) {
        snapshot = snapshot.copy(
            availabilities = snapshot.availabilities.filterNot { it.roomId == roomId } + availabilities,
        )
        persist()
    }

    fun importStartLocations(roomId: String, startLocations: List<UserStartLocation>) {
        snapshot = snapshot.copy(
            startLocations = snapshot.startLocations.filterNot { it.roomId == roomId } + startLocations,
        )
        persist()
    }

    fun importDestinationStationProposals(roomId: String, proposals: List<DestinationStationProposal>) {
        snapshot = snapshot.copy(
            destinationStationProposals = snapshot.destinationStationProposals.filterNot { it.roomId == roomId } + proposals,
        )
        persist()
    }

    fun importDestinationStationVotes(roomId: String, votes: List<DestinationStationVote>) {
        snapshot = snapshot.copy(
            destinationStationVotes = snapshot.destinationStationVotes.filterNot { it.roomId == roomId } + votes,
        )
        persist()
    }

    fun importFriends(friends: List<FriendProfile>) {
        snapshot = snapshot.copy(friends = friends.distinctBy { it.userId })
        persist()
    }

    fun retainRooms(roomIds: Set<String>) {
        snapshot = snapshot.copy(
            rooms = snapshot.rooms.filter { it.id in roomIds },
            participants = snapshot.participants.filter { it.roomId in roomIds },
            availabilities = snapshot.availabilities.filter { it.roomId in roomIds },
            currentParticipantIds = snapshot.currentParticipantIds.filterKeys { it in roomIds },
            startLocations = snapshot.startLocations.filter { it.roomId in roomIds },
            travelPreferences = snapshot.travelPreferences.filter { it.roomId in roomIds },
            areaRecommendations = snapshot.areaRecommendations.filterKeys { it in roomIds },
            placeCandidates = snapshot.placeCandidates.filterKeys { it in roomIds },
            placeVotes = snapshot.placeVotes.filter { it.roomId in roomIds },
            destinationStationProposals = snapshot.destinationStationProposals.filter { it.roomId in roomIds },
            destinationStationVotes = snapshot.destinationStationVotes.filter { it.roomId in roomIds },
        )
        persist()
    }

    override fun clearLocalCache() {
        snapshot = AppSnapshot()
        storage.remove(STORAGE_KEY)
    }

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

    override fun getDestinationStationProposals(roomId: String): List<DestinationStationProposal> =
        snapshot.destinationStationProposals.filter { it.roomId == roomId }

    override fun getDestinationStationVotes(roomId: String): List<DestinationStationVote> =
        snapshot.destinationStationVotes.filter { it.roomId == roomId }

    override suspend fun createRoom(
        room: MeetingRoom,
        host: Participant,
        invitedParticipants: List<Participant>,
    ) {
        require(room.maxParticipants in MIN_MEETING_PARTICIPANTS..MAX_MEETING_PARTICIPANTS) {
            "최대 인원은 2명에서 8명 사이여야 합니다."
        }
        require(1 + invitedParticipants.size <= room.maxParticipants) { "초대 인원이 방 정원을 초과했습니다." }
        require(invitedParticipants.all { it.roomId == room.id && !it.isHost && it.isInvited }) {
            "친구 초대 정보를 확인해주세요."
        }
        val participantsToStore = listOf(host) + invitedParticipants
        require(participantsToStore.map { it.nickname.lowercase() }.distinct().size == participantsToStore.size) {
            "같은 닉네임의 친구를 중복으로 초대할 수 없습니다."
        }
        require(snapshot.rooms.none { it.id.equals(room.id, ignoreCase = true) }) { "이미 존재하는 방 코드입니다." }
        snapshot = snapshot.copy(
            rooms = snapshot.rooms + room,
            participants = snapshot.participants + participantsToStore,
            currentParticipantIds = snapshot.currentParticipantIds + (room.id to host.id),
        )
        persist()
    }

    override suspend fun joinRoom(participant: Participant) {
        val room = requireNotNull(getRoom(participant.roomId)) { "방 코드를 확인해주세요." }
        val existingParticipant = participant.accountId?.let { accountId ->
            getParticipants(room.id).firstOrNull { it.accountId == accountId }
        }
        if (existingParticipant != null && !existingParticipant.isInvited) {
            snapshot = snapshot.copy(
                currentParticipantIds = snapshot.currentParticipantIds + (room.id to existingParticipant.id),
            )
            persist()
            return
        }
        val invitedParticipant = participant.accountId?.let { accountId ->
            getParticipants(room.id).firstOrNull { it.isInvited && it.accountId == accountId }
        }
        if (invitedParticipant != null) {
            val joinedParticipant = invitedParticipant.copy(
                isInvited = false,
                joinedAt = participant.joinedAt,
            )
            snapshot = snapshot.copy(
                participants = snapshot.participants.map {
                    if (it.roomId == room.id && it.id == invitedParticipant.id) joinedParticipant else it
                },
                currentParticipantIds = snapshot.currentParticipantIds + (room.id to invitedParticipant.id),
                friends = mergedFriendsFor(
                    currentAccountId = joinedParticipant.accountId,
                    participants = getParticipants(room.id).filterNot { it.id == invitedParticipant.id || it.isInvited },
                    metAt = participant.joinedAt,
                ),
            )
            touchRoom(room.id)
            persist()
            return
        }
        val currentCount = getParticipants(participant.roomId).size
        require(currentCount < room.maxParticipants) { "정원이 가득 차서 참여할 수 없습니다." }
        val duplicate = snapshot.participants.any {
            it.roomId == participant.roomId && it.nickname.equals(participant.nickname, ignoreCase = true)
        }
        require(!duplicate) { "이미 사용 중인 닉네임입니다." }
        snapshot = snapshot.copy(
            participants = snapshot.participants + participant,
            currentParticipantIds = snapshot.currentParticipantIds + (participant.roomId to participant.id),
            friends = mergedFriendsFor(
                currentAccountId = participant.accountId,
                participants = getParticipants(room.id).filterNot { it.isInvited },
                metAt = participant.joinedAt,
            ),
        )
        touchRoom(participant.roomId)
        persist()
    }

    override suspend fun leaveRoom(roomId: String, participantId: String) {
        val room = requireNotNull(getRoom(roomId)) { "방 정보를 찾을 수 없습니다." }
        require(room.hostParticipantId != participantId) { "방장은 방을 나갈 수 없습니다." }
        snapshot = snapshot.copy(
            participants = snapshot.participants.filterNot { it.roomId == roomId && it.id == participantId },
            availabilities = snapshot.availabilities.filterNot { it.roomId == roomId && it.participantId == participantId },
            startLocations = snapshot.startLocations.filterNot { it.roomId == roomId && it.participantId == participantId },
            travelPreferences = snapshot.travelPreferences.filterNot { it.roomId == roomId && it.participantId == participantId },
            placeVotes = snapshot.placeVotes.filterNot { it.roomId == roomId && it.participantId == participantId },
            destinationStationProposals = snapshot.destinationStationProposals.filterNot {
                it.roomId == roomId && it.participantId == participantId
            },
            destinationStationVotes = snapshot.destinationStationVotes.filterNot {
                it.roomId == roomId && it.participantId == participantId
            },
            currentParticipantIds = snapshot.currentParticipantIds - roomId,
        )
        touchRoom(roomId)
        persist()
    }

    override suspend fun deleteRoom(roomId: String) {
        requireNotNull(getRoom(roomId)) { "방 정보를 찾을 수 없습니다." }
        snapshot = snapshot.copy(
            rooms = snapshot.rooms.filterNot { it.id == roomId },
            participants = snapshot.participants.filterNot { it.roomId == roomId },
            availabilities = snapshot.availabilities.filterNot { it.roomId == roomId },
            currentParticipantIds = snapshot.currentParticipantIds - roomId,
            startLocations = snapshot.startLocations.filterNot { it.roomId == roomId },
            travelPreferences = snapshot.travelPreferences.filterNot { it.roomId == roomId },
            areaRecommendations = snapshot.areaRecommendations - roomId,
            placeCandidates = snapshot.placeCandidates - roomId,
            placeVotes = snapshot.placeVotes.filterNot { it.roomId == roomId },
            destinationStationProposals = snapshot.destinationStationProposals.filterNot { it.roomId == roomId },
            destinationStationVotes = snapshot.destinationStationVotes.filterNot { it.roomId == roomId },
        )
        persist()
    }

    override suspend fun saveAvailabilities(
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

    override suspend fun confirmDate(roomId: String, date: LocalDate) {
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map { room ->
            if (room.id == roomId) {
                val dateChanged = room.confirmedDate != null && room.confirmedDate != date
                room.copy(
                    status = MeetingStatus.DATE_CONFIRMED,
                    confirmedDate = date,
                    selectedAreaCandidateId = if (dateChanged) null else room.selectedAreaCandidateId,
                    confirmedPlace = if (dateChanged) null else room.confirmedPlace,
                    updatedAt = now,
                )
            } else {
                room
            }
        })
        persist()
    }

    override suspend fun confirmMeetingWithoutPlace(roomId: String) {
        val room = requireNotNull(getRoom(roomId)) { "방 정보를 찾을 수 없습니다." }
        require(room.confirmedDate != null) { "날짜를 먼저 확정해주세요." }
        val now = Clock.System.now()
        snapshot = snapshot.copy(rooms = snapshot.rooms.map {
            if (it.id == roomId) {
                it.copy(
                    status = MeetingStatus.MEETING_CONFIRMED,
                    selectedAreaCandidateId = null,
                    confirmedPlace = null,
                    updatedAt = now,
                )
            } else {
                it
            }
        })
        persist()
    }

    override suspend fun saveStartLocation(location: UserStartLocation) {
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

    override suspend fun saveDestinationStationProposal(proposal: DestinationStationProposal) {
        val previousStationId = snapshot.destinationStationProposals.firstOrNull {
            it.roomId == proposal.roomId && it.participantId == proposal.participantId
        }?.station?.id
        val updatedProposals = snapshot.destinationStationProposals.filterNot {
            it.roomId == proposal.roomId && it.participantId == proposal.participantId
        } + proposal
        val stationStillProposed = previousStationId == null || updatedProposals.any {
            it.roomId == proposal.roomId && it.station.id == previousStationId
        }
        snapshot = snapshot.copy(
            destinationStationProposals = updatedProposals,
            destinationStationVotes = if (stationStillProposed) {
                snapshot.destinationStationVotes
            } else {
                snapshot.destinationStationVotes.filterNot {
                    it.roomId == proposal.roomId && it.stationId == previousStationId
                }
            },
            rooms = snapshot.rooms.map { room ->
                if (room.id == proposal.roomId) room.copy(
                    status = MeetingStatus.PLACE_SELECTING,
                    confirmedPlace = null,
                    updatedAt = proposal.updatedAt,
                ) else room
            },
        )
        persist()
    }

    override suspend fun saveDestinationStationVote(vote: DestinationStationVote) {
        require(snapshot.destinationStationProposals.any {
            it.roomId == vote.roomId && it.station.id == vote.stationId
        }) { "현재 후보 목록에 없는 역입니다." }
        snapshot = snapshot.copy(
            destinationStationVotes = snapshot.destinationStationVotes.filterNot {
                it.roomId == vote.roomId && it.participantId == vote.participantId
            } + vote,
            rooms = snapshot.rooms.map { room ->
                if (room.id == vote.roomId) room.copy(
                    status = MeetingStatus.PLACE_SELECTING,
                    updatedAt = vote.updatedAt,
                ) else room
            },
        )
        persist()
    }

    override suspend fun confirmPlace(roomId: String, place: PlaceCandidate) {
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
                    val confirmedStation = room.confirmedPlace?.let {
                        it.source == PlaceSource.CUSTOM && it.id.startsWith("station-")
                    } == true
                    val hasDestinationStationFlow = snapshot.destinationStationProposals.any {
                        it.roomId == roomId
                    }
                    if (confirmedStation || hasDestinationStationFlow) room else room.copy(
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

    private fun mergedFriendsFor(
        currentAccountId: String?,
        participants: List<Participant>,
        metAt: kotlin.time.Instant,
    ): List<FriendProfile> {
        if (currentAccountId == null) return snapshot.friends
        val byUserId = snapshot.friends.associateBy { it.userId }.toMutableMap()
        participants.forEach { other ->
            val friendUserId = other.accountId ?: return@forEach
            if (friendUserId == currentAccountId) return@forEach
            val previous = byUserId[friendUserId]
            byUserId[friendUserId] = FriendProfile(
                userId = friendUserId,
                nickname = other.nickname,
                sharedMeetingCount = (previous?.sharedMeetingCount ?: 0) + 1,
                lastMetAt = metAt,
            )
        }
        return byUserId.values.toList()
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
        val destinationStationProposals: List<DestinationStationProposal> = emptyList(),
        val destinationStationVotes: List<DestinationStationVote> = emptyList(),
        val friends: List<FriendProfile> = emptyList(),
    )

    private companion object {
        const val STORAGE_KEY = "meeting_mvp_snapshot_v1"
    }
}
