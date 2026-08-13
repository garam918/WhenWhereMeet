package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.FriendProfile
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.TransitStation
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.KoreaTimeZone
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class FirestoreMeetingRepository(
    private val local: LocalMeetingRepository,
    private val firestore: FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId = FIRESTORE_DATABASE_ID),
) : MeetingRepository {
    override fun getRooms(): List<MeetingRoom> = local.getRooms()
    override fun getRoom(roomIdOrCode: String): MeetingRoom? = local.getRoom(roomIdOrCode)
    override fun getParticipants(roomId: String): List<Participant> = local.getParticipants(roomId)
    override fun getAvailabilities(roomId: String): List<Availability> = local.getAvailabilities(roomId)
    override fun getCurrentParticipantId(roomId: String): String? = local.getCurrentParticipantId(roomId)
    override fun getFriends(): List<FriendProfile> = local.getFriends()
    override fun getStartLocations(roomId: String): List<UserStartLocation> = local.getStartLocations(roomId)
    override fun getTravelPreferences(roomId: String): List<ParticipantTravelPreference> = local.getTravelPreferences(roomId)
    override fun getAreaRecommendations(roomId: String): List<AreaRecommendation> = local.getAreaRecommendations(roomId)
    override fun getPlaceCandidates(roomId: String): List<ScoredPlaceCandidate> = local.getPlaceCandidates(roomId)
    override fun getPlaceVotes(roomId: String): List<PlaceVote> = local.getPlaceVotes(roomId)
    override fun getDestinationStationProposals(roomId: String): List<DestinationStationProposal> =
        local.getDestinationStationProposals(roomId)
    override fun getDestinationStationVotes(roomId: String): List<DestinationStationVote> =
        local.getDestinationStationVotes(roomId)

    override suspend fun refreshRooms() {
        val authUid = currentAuthUid()
        val roomIds = firestore.collectionGroup(PARTICIPANTS)
            .where { "authUid" equalTo authUid }
            .get()
            .documents
            .map { it.data<FirestoreParticipant>().roomId }
            .filter { it.isNotBlank() }
            .distinct()
        roomIds.forEach { refreshRoom(it) }
        local.retainRooms(roomIds.toSet())
    }

    override fun clearLocalCache() = local.clearLocalCache()

    override suspend fun getRoomForJoin(roomIdOrCode: String): MeetingRoom? {
        val code = roomIdOrCode.normalizedRoomCode()
        local.getRoom(code)?.let { return it }
        val codeSnapshot = roomCodes.document(code).get()
        val roomId = if (codeSnapshot.exists) {
            codeSnapshot.data<FirestoreRoomCode>().roomId
        } else {
            code
        }
        val roomSnapshot = rooms.document(roomId).get()
        if (!roomSnapshot.exists) return null
        val room = roomSnapshot.data<FirestoreMeetingRoom>().toDomain()
        val participants = loadParticipants(room.id)
        val currentParticipantId = local.getCurrentParticipantId(room.id)
            ?: participants.firstOrNull { it.accountId == currentAuthUid() }?.id
        local.importRoom(room, participants, currentParticipantId)
        local.importStartLocations(room.id, loadStartLocations(room.id))
        local.importDestinationStationProposals(room.id, loadDestinationStationProposals(room.id))
        local.importDestinationStationVotes(room.id, loadDestinationStationVotes(room.id))
        return room
    }

    override suspend fun refreshRoom(roomId: String) {
        val localRoom = local.getRoom(roomId)
        val firestoreRoomId = localRoom?.id ?: roomId.normalizedRoomCode()
        val roomSnapshot = rooms.document(firestoreRoomId).get()
        if (!roomSnapshot.exists) {
            if (localRoom != null) {
                local.deleteRoom(localRoom.id)
            }
            return
        }
        val remoteRoom = roomSnapshot.data<FirestoreMeetingRoom>().toDomain()
        val participants = loadParticipants(remoteRoom.id)
        local.importRoom(
            room = mergeRemoteRoom(remoteRoom),
            participants = participants,
            currentParticipantId = local.getCurrentParticipantId(remoteRoom.id)
                ?: participants.firstOrNull { it.accountId == currentAuthUid() }?.id,
        )
        local.importAvailabilities(remoteRoom.id, loadAvailabilities(remoteRoom.id))
        local.importStartLocations(remoteRoom.id, loadStartLocations(remoteRoom.id))
        local.importDestinationStationProposals(remoteRoom.id, loadDestinationStationProposals(remoteRoom.id))
        local.importDestinationStationVotes(remoteRoom.id, loadDestinationStationVotes(remoteRoom.id))
    }

    override suspend fun refreshFriends() {
        val friends = firestore.collection(USERS)
            .document(currentAuthUid())
            .collection(FRIENDS)
            .get()
            .documents
            .map { it.data<FirestoreFriend>().toDomain() }
        local.importFriends(friends)
    }

    override fun observeRoom(roomId: String): Flow<Unit> {
        val firestoreRoomId = local.getRoom(roomId)?.id ?: roomId.normalizedRoomCode()
        val roomRef = rooms.document(firestoreRoomId)
        return merge(
            roomRef.snapshots.map { snapshot ->
                if (snapshot.exists) {
                    local.importRoomMetadata(mergeRemoteRoom(snapshot.data<FirestoreMeetingRoom>().toDomain()))
                } else {
                    local.getRoom(firestoreRoomId)?.let { local.deleteRoom(it.id) }
                }
                Unit
            },
            roomRef.collection(PARTICIPANTS).snapshots.map { snapshot ->
                local.importParticipants(
                    firestoreRoomId,
                    snapshot.documents.map { it.data<FirestoreParticipant>().toDomain() },
                )
            },
            roomRef.collection(AVAILABILITIES).snapshots.map { snapshot ->
                local.importAvailabilities(
                    firestoreRoomId,
                    snapshot.documents.map { it.data<FirestoreAvailability>().toDomain() },
                )
            },
            roomRef.collection(START_LOCATIONS).snapshots.map { snapshot ->
                local.importStartLocations(
                    firestoreRoomId,
                    snapshot.documents.map { it.data<FirestoreStartLocation>().toDomain() },
                )
            },
            roomRef.collection(DESTINATION_STATION_PROPOSALS).snapshots.map { snapshot ->
                local.importDestinationStationProposals(
                    firestoreRoomId,
                    snapshot.documents.map { it.data<FirestoreDestinationStationProposal>().toDomain() },
                )
            },
            roomRef.collection(DESTINATION_STATION_VOTES).snapshots.map { snapshot ->
                local.importDestinationStationVotes(
                    firestoreRoomId,
                    snapshot.documents.map { it.data<FirestoreDestinationStationVote>().toDomain() },
                )
            },
        )
    }

    override suspend fun createRoom(
        room: MeetingRoom,
        host: Participant,
        invitedParticipants: List<Participant>,
    ) {
        val code = room.id.normalizedRoomCode()
        require(room.maxParticipants in 2..8) { "최대 인원은 2명에서 8명 사이여야 합니다." }
        require(1 + invitedParticipants.size <= room.maxParticipants) { "초대 인원이 방 정원을 초과했습니다." }
        val roomToStore = room.copy(id = code)
        val hostToStore = host.copy(roomId = code)
        val invitedToStore = invitedParticipants.map { it.copy(roomId = code, isInvited = true) }
        require(invitedToStore.all { it.accountId != null }) { "친구 계정 정보를 확인해주세요." }
        val allParticipants = listOf(hostToStore) + invitedToStore
        require(allParticipants.map { it.nickname.lowercase() }.distinct().size == allParticipants.size) {
            "같은 닉네임의 친구를 중복으로 초대할 수 없습니다."
        }
        val hostAuthUid = currentAuthUid()
        firestore.runTransaction {
            val codeRef = roomCodes.document(code)
            require(!get(codeRef).exists) { "이미 존재하는 방 코드입니다." }
            val roomRef = rooms.document(code)
            set(roomRef, FirestoreMeetingRoom.from(roomToStore, participantCount = allParticipants.size, hostAuthUid = hostAuthUid))
            set(codeRef, FirestoreRoomCode(code = code, roomId = code, createdAt = room.createdAt.toKoreaIsoString()))
            set(roomRef.collection(PARTICIPANTS).document(host.id), FirestoreParticipant.from(hostToStore, authUid = hostAuthUid))
            set(roomRef.collection(NICKNAMES).document(host.nickname.nicknameKey()), FirestoreNickname(participantId = host.id))
            invitedToStore.forEach { invited ->
                set(
                    roomRef.collection(PARTICIPANTS).document(invited.id),
                    FirestoreParticipant.from(invited, authUid = requireNotNull(invited.accountId)),
                )
                set(
                    roomRef.collection(NICKNAMES).document(invited.nickname.nicknameKey()),
                    FirestoreNickname(participantId = invited.id),
                )
            }
        }
        local.createRoom(roomToStore, hostToStore.copy(accountId = hostAuthUid), invitedToStore)
    }

    override suspend fun joinRoom(participant: Participant) {
        val room = getRoomForJoin(participant.roomId)
            ?: throw IllegalArgumentException("방 코드를 확인해주세요.")
        val participantToStore = participant.copy(roomId = room.id)
        val nicknameKey = participant.nickname.nicknameKey()
        val authUid = currentAuthUid()
        val existingParticipant = loadParticipants(room.id).firstOrNull { it.accountId == authUid }
        if (existingParticipant != null) {
            if (existingParticipant.isInvited) {
                val participantRef = rooms.document(room.id).collection(PARTICIPANTS).document(existingParticipant.id)
                val storedParticipant = participantRef.get().data<FirestoreParticipant>()
                participantRef.set(
                    storedParticipant.copy(
                        isInvited = false,
                        joinedAt = participant.joinedAt.toKoreaIsoString(),
                    ),
                )
            }
            local.importRoom(room, loadParticipants(room.id), existingParticipant.id)
            return
        }
        firestore.runTransaction {
            val roomRef = rooms.document(room.id)
            val roomSnapshot = get(roomRef)
            require(roomSnapshot.exists) { "방 코드를 확인해주세요." }
            val current = roomSnapshot.data<FirestoreMeetingRoom>()
            require(current.participantCount < current.maxParticipants) { "정원이 가득 차서 참여할 수 없습니다." }
            val nicknameRef = roomRef.collection(NICKNAMES).document(nicknameKey)
            require(!get(nicknameRef).exists) { "이미 사용 중인 닉네임입니다." }
            set(roomRef, current.copy(participantCount = current.participantCount + 1, updatedAt = participant.joinedAt.toKoreaIsoString()))
            set(roomRef.collection(PARTICIPANTS).document(participant.id), FirestoreParticipant.from(participantToStore, authUid = authUid))
            set(nicknameRef, FirestoreNickname(participantId = participant.id))
        }
        local.importRoom(
            room,
            (loadParticipants(room.id) + participantToStore.copy(accountId = authUid)).distinctBy { it.id },
            participant.id,
        )
    }

    override suspend fun leaveRoom(roomId: String, participantId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.hostParticipantId != participantId) { "방장은 방을 나갈 수 없습니다." }
        val participant = getParticipants(roomId).firstOrNull { it.id == participantId }
            ?: throw IllegalArgumentException("참여자 정보를 찾을 수 없습니다.")
        val participantAvailabilities = loadAvailabilities(room.id).filter { it.participantId == participantId }
        firestore.runTransaction {
            val roomRef = rooms.document(room.id)
            val roomSnapshot = get(roomRef)
            require(roomSnapshot.exists) { "방 정보를 찾을 수 없습니다." }
            val current = roomSnapshot.data<FirestoreMeetingRoom>()
            val now = kotlin.time.Clock.System.now()
            set(
                roomRef,
                current.copy(
                    participantCount = (current.participantCount - 1).coerceAtLeast(1),
                    updatedAt = now.toKoreaIsoString(),
                ),
            )
            delete(roomRef.collection(PARTICIPANTS).document(participantId))
            delete(roomRef.collection(NICKNAMES).document(participant.nickname.nicknameKey()))
            participantAvailabilities.forEach { delete(availabilityDocument(room.id, participantId, it.date)) }
            delete(roomRef.collection(DESTINATION_STATION_PROPOSALS).document(participantId))
            delete(roomRef.collection(DESTINATION_STATION_VOTES).document(participantId))
        }
        local.leaveRoom(roomId, participantId)
    }

    override suspend fun deleteRoom(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        ensureCurrentUserCanDeleteRoom(room)
        val batch = firestore.batch()
        loadParticipants(room.id).forEach {
            batch.delete(rooms.document(room.id).collection(PARTICIPANTS).document(it.id))
            batch.delete(rooms.document(room.id).collection(NICKNAMES).document(it.nickname.nicknameKey()))
        }
        loadAvailabilities(room.id).forEach {
            batch.delete(availabilityDocument(room.id, it.participantId, it.date))
        }
        loadStartLocations(room.id).forEach {
            batch.delete(rooms.document(room.id).collection(START_LOCATIONS).document(it.participantId))
        }
        loadDestinationStationProposals(room.id).forEach {
            batch.delete(rooms.document(room.id).collection(DESTINATION_STATION_PROPOSALS).document(it.participantId))
        }
        loadDestinationStationVotes(room.id).forEach {
            batch.delete(rooms.document(room.id).collection(DESTINATION_STATION_VOTES).document(it.participantId))
        }
        batch.delete(roomCodes.document(room.id.normalizedRoomCode()))
        batch.delete(rooms.document(room.id))
        batch.commit()
        local.deleteRoom(room.id)
    }

    override suspend fun saveAvailabilities(roomId: String, participantId: String, values: Map<LocalDate, AvailabilityStatus>) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val previous = loadAvailabilities(room.id).filter { it.participantId == participantId }
        val batch = firestore.batch()
        previous
            .filterNot { values.containsKey(it.date) }
            .forEach { batch.delete(availabilityDocument(room.id, participantId, it.date)) }
        values.forEach { (date, status) ->
            batch.set(
                availabilityDocument(room.id, participantId, date),
                FirestoreAvailability.from(Availability(room.id, participantId, date, status, kotlin.time.Clock.System.now())),
            )
        }
        batch.commit()
        local.saveAvailabilities(room.id, participantId, values)
    }
    override suspend fun confirmDate(roomId: String, date: LocalDate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val dateChanged = room.confirmedDate != null && room.confirmedDate != date
        val updated = room.copy(
            status = MeetingStatus.DATE_CONFIRMED,
            confirmedDate = date,
            selectedAreaCandidateId = if (dateChanged) null else room.selectedAreaCandidateId,
            confirmedPlace = if (dateChanged) null else room.confirmedPlace,
            updatedAt = kotlin.time.Clock.System.now(),
        )
        val previous = rooms.document(room.id).get().data<FirestoreMeetingRoom>()
        rooms.document(room.id).set(
            FirestoreMeetingRoom.from(
                room = updated,
                participantCount = loadParticipants(room.id).size,
                hostAuthUid = previous.hostAuthUid,
            ),
        )
        local.confirmDate(room.id, date)
    }
    override suspend fun confirmMeetingWithoutPlace(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.confirmedDate != null) { "날짜를 먼저 확정해주세요." }
        val previous = rooms.document(room.id).get().data<FirestoreMeetingRoom>()
        val updated = room.copy(
            status = MeetingStatus.MEETING_CONFIRMED,
            selectedAreaCandidateId = null,
            confirmedPlace = null,
            updatedAt = kotlin.time.Clock.System.now(),
        )
        rooms.document(room.id).set(
            FirestoreMeetingRoom.from(
                room = updated,
                participantCount = loadParticipants(room.id).size,
                hostAuthUid = previous.hostAuthUid,
            ),
        )
        local.confirmMeetingWithoutPlace(room.id)
    }
    override suspend fun saveStartLocation(location: UserStartLocation) {
        val room = getRoom(location.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        rooms.document(room.id).collection(START_LOCATIONS).document(location.participantId).set(FirestoreStartLocation.from(location))
        local.saveStartLocation(location)
    }
    override fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode) = local.saveTransportMode(roomId, participantId, transportMode)
    override fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>) = local.saveAreaRecommendations(roomId, recommendations)
    override fun selectAreaCandidate(roomId: String, candidateId: String) = local.selectAreaCandidate(roomId, candidateId)
    override fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>) = local.savePlaceCandidates(roomId, candidates)
    override fun savePlaceVote(roomId: String, placeId: String, participantId: String, voteType: PlaceVoteType) = local.savePlaceVote(roomId, placeId, participantId, voteType)
    override suspend fun saveDestinationStationProposal(proposal: DestinationStationProposal) {
        val room = getRoom(proposal.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val roomRef = rooms.document(room.id)
        val previous = roomRef.get().data<FirestoreMeetingRoom>()
        val normalized = proposal.copy(roomId = room.id)
        val updatedRoom = room.copy(
            status = MeetingStatus.PLACE_SELECTING,
            confirmedPlace = null,
            updatedAt = proposal.updatedAt,
        )
        val batch = firestore.batch()
        batch.set(
            roomRef,
            FirestoreMeetingRoom.from(updatedRoom, loadParticipants(room.id).size, previous.hostAuthUid),
        )
        batch.set(
            roomRef.collection(DESTINATION_STATION_PROPOSALS).document(proposal.participantId),
            FirestoreDestinationStationProposal.from(normalized),
        )
        batch.commit()
        local.saveDestinationStationProposal(normalized)
    }

    override suspend fun saveDestinationStationVote(vote: DestinationStationVote) {
        val room = getRoom(vote.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(getDestinationStationProposals(room.id).any { it.station.id == vote.stationId }) {
            "현재 후보 목록에 없는 역입니다."
        }
        val roomRef = rooms.document(room.id)
        val previous = roomRef.get().data<FirestoreMeetingRoom>()
        val normalized = vote.copy(roomId = room.id)
        val updatedRoom = room.copy(status = MeetingStatus.PLACE_SELECTING, updatedAt = vote.updatedAt)
        val batch = firestore.batch()
        batch.set(
            roomRef,
            FirestoreMeetingRoom.from(updatedRoom, loadParticipants(room.id).size, previous.hostAuthUid),
        )
        batch.set(
            roomRef.collection(DESTINATION_STATION_VOTES).document(vote.participantId),
            FirestoreDestinationStationVote.from(normalized),
        )
        batch.commit()
        local.saveDestinationStationVote(normalized)
    }

    override suspend fun confirmPlace(roomId: String, place: PlaceCandidate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val previous = rooms.document(room.id).get().data<FirestoreMeetingRoom>()
        val updated = room.copy(
            status = MeetingStatus.PLACE_CONFIRMED,
            confirmedPlace = place,
            updatedAt = kotlin.time.Clock.System.now(),
        )
        rooms.document(room.id).set(
            FirestoreMeetingRoom.from(
                room = updated,
                participantCount = loadParticipants(room.id).size,
                hostAuthUid = previous.hostAuthUid,
            ),
        )
        local.confirmPlace(room.id, place)
    }

    private suspend fun loadParticipants(roomId: String): List<Participant> =
        rooms.document(roomId).collection(PARTICIPANTS).get().documents.map { it.data<FirestoreParticipant>().toDomain() }

    private suspend fun loadAvailabilities(roomId: String): List<Availability> =
        rooms.document(roomId).collection(AVAILABILITIES).get().documents.map { it.data<FirestoreAvailability>().toDomain() }

    private suspend fun loadStartLocations(roomId: String): List<UserStartLocation> =
        rooms.document(roomId).collection(START_LOCATIONS).get().documents.map { it.data<FirestoreStartLocation>().toDomain() }

    private suspend fun loadDestinationStationProposals(roomId: String): List<DestinationStationProposal> =
        rooms.document(roomId).collection(DESTINATION_STATION_PROPOSALS).get().documents.map {
            it.data<FirestoreDestinationStationProposal>().toDomain()
        }

    private suspend fun loadDestinationStationVotes(roomId: String): List<DestinationStationVote> =
        rooms.document(roomId).collection(DESTINATION_STATION_VOTES).get().documents.map {
            it.data<FirestoreDestinationStationVote>().toDomain()
        }

    private fun availabilityDocument(roomId: String, participantId: String, date: LocalDate) =
        rooms.document(roomId).collection(AVAILABILITIES).document("$participantId-${date}")

    private fun mergeRemoteRoom(remoteRoom: MeetingRoom): MeetingRoom {
        val localRoom = local.getRoom(remoteRoom.id) ?: return remoteRoom
        return remoteRoom.copy(
            selectedAreaCandidateId = localRoom.selectedAreaCandidateId ?: remoteRoom.selectedAreaCandidateId,
            confirmedPlace = localRoom.confirmedPlace ?: remoteRoom.confirmedPlace,
        )
    }

    private suspend fun ensureCurrentUserCanDeleteRoom(room: MeetingRoom) {
        val currentUid = currentAuthUid()
        val roomRef = rooms.document(room.id)
        val snapshot = roomRef.get()
        require(snapshot.exists) { "방 정보를 찾을 수 없습니다." }
        val storedRoom = snapshot.data<FirestoreMeetingRoom>()
        val hostParticipantId = storedRoom.hostParticipantId.ifBlank { room.hostParticipantId }
        val hostParticipantRef = roomRef.collection(PARTICIPANTS).document(hostParticipantId)
        val hostParticipantSnapshot = hostParticipantRef.get()
        require(hostParticipantSnapshot.exists) { "방장 정보를 찾을 수 없습니다." }
        val hostParticipant = hostParticipantSnapshot.data<FirestoreParticipant>()
        when (hostParticipant.authUid) {
            currentUid -> return
            null -> {
                require(storedRoom.hostAuthUid == currentUid) { "방장 계정에서만 약속을 삭제할 수 있습니다." }
                hostParticipantRef.set(hostParticipant.copy(authUid = currentUid))
            }
            else -> throw IllegalArgumentException("방장 계정에서만 약속을 삭제할 수 있습니다.")
        }
    }

    private fun currentAuthUid(): String {
        val user = requireNotNull(Firebase.auth.currentUser) { "로그인 정보를 확인할 수 없습니다." }
        require(!user.isAnonymous) { "Google 또는 Apple 계정으로 로그인해주세요." }
        return user.uid
    }

    private val rooms get() = firestore.collection(ROOMS)
    private val roomCodes get() = firestore.collection(ROOM_CODES)

    private companion object {
        const val FIRESTORE_DATABASE_ID = "default"
        const val ROOMS = "meetingRooms"
        const val ROOM_CODES = "roomCodes"
        const val PARTICIPANTS = "participants"
        const val AVAILABILITIES = "availabilities"
        const val START_LOCATIONS = "startLocations"
        const val DESTINATION_STATION_PROPOSALS = "destinationStationProposals"
        const val DESTINATION_STATION_VOTES = "destinationStationVotes"
        const val NICKNAMES = "nicknameKeys"
        const val USERS = "users"
        const val FRIENDS = "friends"
    }
}

@Serializable
private data class FirestoreMeetingRoom(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val meetingType: String = MeetingType.OTHER.name,
    val dateRangeStart: String = "",
    val dateRangeEnd: String = "",
    val minParticipants: Int = 1,
    val maxParticipants: Int = 2,
    val participantCount: Int = 0,
    val responseDeadline: String? = null,
    val hostParticipantId: String = "",
    val hostAuthUid: String? = null,
    val status: String = MeetingStatus.COLLECTING_AVAILABILITY.name,
    val confirmedDate: String? = null,
    val confirmedPlace: FirestorePlaceCandidate? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    fun toDomain(): MeetingRoom = MeetingRoom(
        id = id,
        title = title,
        description = description,
        meetingType = enumValueOf(meetingType),
        dateRangeStart = LocalDate.parse(dateRangeStart),
        dateRangeEnd = LocalDate.parse(dateRangeEnd),
        minParticipants = minParticipants,
        maxParticipants = maxParticipants,
        responseDeadline = responseDeadline?.let(LocalDate::parse),
        hostParticipantId = hostParticipantId,
        status = enumValueOf(status),
        confirmedDate = confirmedDate?.let(LocalDate::parse),
        confirmedPlace = confirmedPlace?.toDomain(),
        createdAt = createdAt.parseFirestoreInstant(),
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(
            room: MeetingRoom,
            participantCount: Int,
            hostAuthUid: String?,
        ) = FirestoreMeetingRoom(
            id = room.id,
            title = room.title,
            description = room.description,
            meetingType = room.meetingType.name,
            dateRangeStart = room.dateRangeStart.toString(),
            dateRangeEnd = room.dateRangeEnd.toString(),
            minParticipants = room.minParticipants,
            maxParticipants = room.maxParticipants,
            participantCount = participantCount,
            responseDeadline = room.responseDeadline?.toString(),
            hostParticipantId = room.hostParticipantId,
            hostAuthUid = hostAuthUid,
            status = room.status.name,
            confirmedDate = room.confirmedDate?.toString(),
            confirmedPlace = room.confirmedPlace?.let(FirestorePlaceCandidate::from),
            createdAt = room.createdAt.toKoreaIsoString(),
            updatedAt = room.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestorePlaceCandidate(
    val id: String = "",
    val name: String = "",
    val category: String = "ETC",
    val address: String? = null,
    val roadAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phoneNumber: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val openingHoursSummary: String? = null,
    val mapUrl: String? = null,
    val source: String = "CUSTOM",
) {
    fun toDomain(): PlaceCandidate = PlaceCandidate(
        id = id,
        name = name,
        category = enumValueOf(category),
        address = address,
        roadAddress = roadAddress,
        latitude = latitude,
        longitude = longitude,
        phoneNumber = phoneNumber,
        rating = rating,
        reviewCount = reviewCount,
        openingHoursSummary = openingHoursSummary,
        mapUrl = mapUrl,
        source = enumValueOf(source),
    )

    companion object {
        fun from(place: PlaceCandidate) = FirestorePlaceCandidate(
            id = place.id,
            name = place.name,
            category = place.category.name,
            address = place.address,
            roadAddress = place.roadAddress,
            latitude = place.latitude,
            longitude = place.longitude,
            phoneNumber = place.phoneNumber,
            rating = place.rating,
            reviewCount = place.reviewCount,
            openingHoursSummary = place.openingHoursSummary,
            mapUrl = place.mapUrl,
            source = place.source.name,
        )
    }
}

@Serializable
private data class FirestoreParticipant(
    val id: String = "",
    val roomId: String = "",
    val nickname: String = "",
    val isHost: Boolean = false,
    val authUid: String? = null,
    val isInvited: Boolean = false,
    val joinedAt: String = "",
) {
    fun toDomain(): Participant = Participant(
        id = id,
        roomId = roomId,
        nickname = nickname,
        isHost = isHost,
        joinedAt = joinedAt.parseFirestoreInstant(),
        accountId = authUid,
        isInvited = isInvited,
    )

    companion object {
        fun from(participant: Participant, authUid: String) = FirestoreParticipant(
            id = participant.id,
            roomId = participant.roomId,
            nickname = participant.nickname,
            isHost = participant.isHost,
            authUid = authUid,
            isInvited = participant.isInvited,
            joinedAt = participant.joinedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreFriend(
    val userId: String = "",
    val nickname: String = "",
    val sharedMeetingCount: Int = 0,
    val lastMetAt: String = "",
) {
    fun toDomain() = FriendProfile(
        userId = userId,
        nickname = nickname,
        sharedMeetingCount = sharedMeetingCount,
        lastMetAt = lastMetAt.parseFirestoreInstant(),
    )
}

@Serializable
private data class FirestoreAvailability(
    val roomId: String = "",
    val participantId: String = "",
    val date: String = "",
    val status: String = AvailabilityStatus.UNAVAILABLE.name,
    val updatedAt: String = "",
) {
    fun toDomain(): Availability = Availability(
        roomId = roomId,
        participantId = participantId,
        date = LocalDate.parse(date),
        status = enumValueOf(status),
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(availability: Availability) = FirestoreAvailability(
            roomId = availability.roomId,
            participantId = availability.participantId,
            date = availability.date.toString(),
            status = availability.status.name,
            updatedAt = availability.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreStartLocation(
    val participantId: String = "",
    val roomId: String = "",
    val label: String = "",
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val privacyLevel: String = "AREA_ONLY_VISIBLE",
    val updatedAt: String = "",
) {
    fun toDomain(): UserStartLocation = UserStartLocation(
        participantId = participantId,
        roomId = roomId,
        label = label,
        address = address,
        latitude = latitude,
        longitude = longitude,
        privacyLevel = enumValueOf(privacyLevel),
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(location: UserStartLocation) = FirestoreStartLocation(
            participantId = location.participantId,
            roomId = location.roomId,
            label = location.label,
            address = location.address,
            latitude = location.latitude,
            longitude = location.longitude,
            privacyLevel = location.privacyLevel.name,
            updatedAt = location.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreDestinationStationProposal(
    val roomId: String = "",
    val participantId: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lines: List<String> = emptyList(),
    val region: String = "",
    val updatedAt: String = "",
) {
    fun toDomain(): DestinationStationProposal = DestinationStationProposal(
        roomId = roomId,
        participantId = participantId,
        station = TransitStation(stationId, stationName, latitude, longitude, lines, region),
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(proposal: DestinationStationProposal) = FirestoreDestinationStationProposal(
            roomId = proposal.roomId,
            participantId = proposal.participantId,
            stationId = proposal.station.id,
            stationName = proposal.station.name,
            latitude = proposal.station.latitude,
            longitude = proposal.station.longitude,
            lines = proposal.station.lines,
            region = proposal.station.region,
            updatedAt = proposal.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreDestinationStationVote(
    val roomId: String = "",
    val participantId: String = "",
    val stationId: String = "",
    val updatedAt: String = "",
) {
    fun toDomain(): DestinationStationVote = DestinationStationVote(
        roomId = roomId,
        participantId = participantId,
        stationId = stationId,
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(vote: DestinationStationVote) = FirestoreDestinationStationVote(
            roomId = vote.roomId,
            participantId = vote.participantId,
            stationId = vote.stationId,
            updatedAt = vote.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreRoomCode(
    val code: String = "",
    val roomId: String = "",
    val createdAt: String = "",
)

@Serializable
private data class FirestoreNickname(
    val participantId: String = "",
)

private fun String.normalizedRoomCode(): String = trim().uppercase()
private fun String.nicknameKey(): String = trim().lowercase()

private fun Instant.toKoreaIsoString(): String = toLocalDateTime(KoreaTimeZone).formatIsoWithOffset()

private fun String.parseFirestoreInstant(): Instant {
    val value = trim()
    if (value.endsWith("Z")) return Instant.parse(value)
    val offsetIndex = value.indexOfOffset()
    if (offsetIndex == -1) return Instant.parse(value)
    val localDateTime = LocalDateTime.parse(value.substring(0, offsetIndex))
    val offset = value.substring(offsetIndex)
    require(offset == "+09:00") { "지원하지 않는 시간대 형식입니다: $value" }
    return localDateTime.toInstant(UtcOffset(hours = 9))
}

private fun LocalDateTime.formatIsoWithOffset(): String =
    "${date}T${time.hour.twoDigits()}:${time.minute.twoDigits()}:${time.second.twoDigits()}+09:00"

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun String.indexOfOffset(): Int {
    val timeSeparator = indexOf('T')
    if (timeSeparator == -1) return -1
    val plus = indexOf('+', startIndex = timeSeparator)
    if (plus != -1) return plus
    return indexOf('-', startIndex = timeSeparator + 1)
}
