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
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant

class FirestoreMeetingRepository(
    private val local: LocalMeetingRepository,
    private val firestore: FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId = FIRESTORE_DATABASE_ID),
) : MeetingRepository {
    private val roomSync = RoomSyncCoordinator(
        local = local,
        remote = object : RoomSyncRemoteSource {
            override suspend fun loadParticipants(roomId: String) =
                this@FirestoreMeetingRepository.loadParticipants(roomId)
            override suspend fun loadAvailabilities(roomId: String) =
                this@FirestoreMeetingRepository.loadAvailabilities(roomId)
            override suspend fun loadParticipantAvailabilities(roomId: String, participantId: String) =
                this@FirestoreMeetingRepository.loadParticipantAvailabilities(roomId, participantId)
            override suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String) =
                this@FirestoreMeetingRepository.loadVisibleStartLocations(roomId, currentParticipantId)
            override suspend fun loadDestinationStationProposals(roomId: String) =
                this@FirestoreMeetingRepository.loadDestinationStationProposals(roomId)
            override suspend fun loadDestinationStationVotes(roomId: String) =
                this@FirestoreMeetingRepository.loadDestinationStationVotes(roomId)
        },
        currentAccountId = { Firebase.auth.currentUser?.uid },
    )

    override val supportsRealtimeRoomUpdates: Boolean = true

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
        val currentParticipant = loadCurrentUserParticipant(room.id)
        val currentParticipantId = local.getCurrentParticipantId(room.id) ?: currentParticipant?.id
        local.importRoom(room, listOfNotNull(currentParticipant), currentParticipantId)
        if (currentParticipant != null) {
            applyRoomSnapshot(room.id, roomSnapshot)
        }
        return local.getRoom(room.id) ?: room
    }

    override suspend fun refreshRoom(roomId: String) {
        val firestoreRoomId = local.getRoom(roomId)?.id ?: roomId.normalizedRoomCode()
        applyRoomSnapshot(firestoreRoomId, rooms.document(firestoreRoomId).get())
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
        // 방 문서 하나만 구독하고, 변경 번호가 바뀐 하위 컬렉션만 다시 읽는다.
        // 내 쓰기의 추정값 스냅숏은 건너뛰고 서버 확정 스냅숏을 기준으로 비교한다.
        return rooms.document(firestoreRoomId)
            .snapshots(includeMetadataChanges = true)
            .filterNot { it.metadata.hasPendingWrites }
            .map { applyRoomSnapshot(firestoreRoomId, it) }
    }

    private suspend fun applyRoomSnapshot(roomId: String, snapshot: DocumentSnapshot) {
        if (!snapshot.exists) {
            // 캐시에 아직 없는 문서를 삭제로 오인하지 않도록 서버 응답일 때만 로컬에서 지운다.
            if (!snapshot.metadata.isFromCache) roomSync.removeLocalRoom(roomId)
            return
        }
        roomSync.applyRemoteRoom(
            remoteRoom = snapshot.data<FirestoreMeetingRoom>().toDomain(),
            remoteRevisions = snapshot.data<FirestoreRoomSyncFields>().toDomain(),
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
            set(
                roomRef.collection(MEMBERS).document(hostAuthUid),
                FirestoreRoomMember.from(hostToStore, authUid = hostAuthUid, role = "host", joined = true),
            )
            set(roomRef.collection(NICKNAMES).document(host.nickname.nicknameKey()), FirestoreNickname(participantId = host.id))
            invitedToStore.forEach { invited ->
                val invitedAuthUid = requireNotNull(invited.accountId)
                set(
                    roomRef.collection(PARTICIPANTS).document(invited.id),
                    FirestoreParticipant.from(invited, authUid = invitedAuthUid),
                )
                set(
                    roomRef.collection(MEMBERS).document(invitedAuthUid),
                    FirestoreRoomMember.from(invited, authUid = invitedAuthUid, role = "participant", joined = false),
                )
                set(
                    roomRef.collection(NICKNAMES).document(invited.nickname.nicknameKey()),
                    FirestoreNickname(participantId = invited.id),
                )
            }
        }
        local.createRoom(roomToStore, hostToStore.copy(accountId = hostAuthUid), invitedToStore)
        // 새 방에는 변경 번호 필드가 없으므로(0) 방금 쓴 로컬 데이터가 최신이다.
        local.saveAppliedRoomSyncRevisions(code, RoomSyncRevisions())
    }

    override suspend fun joinRoom(participant: Participant) {
        val room = getRoomForJoin(participant.roomId)
            ?: throw IllegalArgumentException("방 코드를 확인해주세요.")
        val participantToStore = participant.copy(roomId = room.id)
        val nicknameKey = participant.nickname.nicknameKey()
        val authUid = currentAuthUid()
        val existingParticipant = loadCurrentUserParticipant(room.id)
        if (existingParticipant != null) {
            if (existingParticipant.isInvited) {
                val roomRef = rooms.document(room.id)
                val participantRef = roomRef.collection(PARTICIPANTS).document(existingParticipant.id)
                val memberRef = roomRef.collection(MEMBERS).document(authUid)
                firestore.runTransaction {
                    val storedParticipant = get(participantRef).data<FirestoreParticipant>()
                    val storedMember = get(memberRef).data<FirestoreRoomMember>()
                    set(
                        participantRef,
                        storedParticipant.copy(
                            isInvited = false,
                            joinedAt = participant.joinedAt.toKoreaIsoString(),
                        ),
                    )
                    set(
                        memberRef,
                        storedMember.copy(joined = true, updatedAt = participant.joinedAt.toKoreaIsoString()),
                    )
                    updateFields(roomRef) {
                        syncRevisionPath(RoomSyncCollection.PARTICIPANTS) to FieldValue.increment(1)
                    }
                }
            }
            // 참여자 목록은 방 화면의 동기화가 변경 번호를 보고 다시 받는다.
            local.importRoom(room, local.getParticipants(room.id).ifEmpty { listOf(existingParticipant) }, existingParticipant.id)
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
            updateFields(roomRef) {
                "participantCount" to current.participantCount + 1
                "updatedAt" to participant.joinedAt.toKoreaIsoString()
                syncRevisionPath(RoomSyncCollection.PARTICIPANTS) to FieldValue.increment(1)
            }
            set(roomRef.collection(PARTICIPANTS).document(participant.id), FirestoreParticipant.from(participantToStore, authUid = authUid))
            set(
                roomRef.collection(MEMBERS).document(authUid),
                FirestoreRoomMember.from(participantToStore, authUid = authUid, role = "participant", joined = true),
            )
            set(nicknameRef, FirestoreNickname(participantId = participant.id))
        }
        // 새로 참여한 방은 적용된 변경 번호를 비워 방 화면에서 전체를 한 번 받게 한다.
        local.importRoom(room, listOf(participantToStore.copy(accountId = authUid)), participant.id)
        local.removeAppliedRoomSyncRevisions(room.id)
    }

    override suspend fun leaveRoom(roomId: String, participantId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.hostParticipantId != participantId) { "방장은 방을 나갈 수 없습니다." }
        val participant = getParticipants(roomId).firstOrNull { it.id == participantId }
            ?: throw IllegalArgumentException("참여자 정보를 찾을 수 없습니다.")
        val participantAvailabilities = loadParticipantAvailabilities(room.id, participantId)
        firestore.runTransaction {
            val roomRef = rooms.document(room.id)
            val roomSnapshot = get(roomRef)
            require(roomSnapshot.exists) { "방 정보를 찾을 수 없습니다." }
            val current = roomSnapshot.data<FirestoreMeetingRoom>()
            val now = Clock.System.now()
            updateFields(roomRef) {
                "participantCount" to (current.participantCount - 1).coerceAtLeast(1)
                "updatedAt" to now.toKoreaIsoString()
                RoomSyncCollection.entries.forEach { syncRevisionPath(it) to FieldValue.increment(1) }
                availabilityRevisionPath(participantId) to FieldValue.increment(1)
            }
            delete(roomRef.collection(PARTICIPANTS).document(participantId))
            delete(roomRef.collection(MEMBERS).document(currentAuthUid()))
            delete(roomRef.collection(NICKNAMES).document(participant.nickname.nicknameKey()))
            participantAvailabilities.forEach { delete(availabilityDocument(room.id, participantId, it.date)) }
            delete(roomRef.collection(START_LOCATIONS).document(participantId))
            delete(roomRef.collection(START_LOCATION_SUMMARIES).document(participantId))
            delete(roomRef.collection(DESTINATION_STATION_PROPOSALS).document(participantId))
            delete(roomRef.collection(DESTINATION_STATION_VOTES).document(participantId))
        }
        local.leaveRoom(roomId, participantId)
    }

    override suspend fun deleteRoom(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        ensureCurrentUserCanDeleteRoom(room)
        val batch = firestore.batch()
        // Cloud Functions recursively removes private subcollections after the room document is deleted.
        batch.delete(roomCodes.document(room.id.normalizedRoomCode()))
        batch.delete(rooms.document(room.id))
        batch.commit()
        local.deleteRoom(room.id)
    }

    override suspend fun saveAvailabilities(roomId: String, participantId: String, values: Map<LocalDate, AvailabilityStatus>) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val previous = local.getAvailabilities(room.id)
            .filter { it.participantId == participantId }
            .associate { it.date to it.status }
        // 로컬 캐시와 비교해 바뀐 날짜만 쓴다. 로컬은 변경 번호 동기화로 최신 상태를 유지한다.
        val changes = AvailabilityChanges.between(previous, values)
        if (changes.isEmpty) return
        roomSync.commitOwnChange(room.id, applyRevision = { it.incrementedAvailability(participantId) }) {
            val now = Clock.System.now()
            val batch = firestore.batch()
            changes.deletedDates.forEach { batch.delete(availabilityDocument(room.id, participantId, it)) }
            changes.upserts.forEach { (date, status) ->
                batch.set(
                    availabilityDocument(room.id, participantId, date),
                    FirestoreAvailability.from(Availability(room.id, participantId, date, status, now)),
                )
            }
            batch.updateFields(rooms.document(room.id)) {
                availabilityRevisionPath(participantId) to FieldValue.increment(1)
            }
            batch.commit()
            local.saveAvailabilities(room.id, participantId, values)
        }
    }
    override suspend fun confirmDate(roomId: String, date: LocalDate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val dateChanged = room.confirmedDate != null && room.confirmedDate != date
        rooms.document(room.id).updateFields {
            "status" to MeetingStatus.DATE_CONFIRMED.name
            "confirmedDate" to date.toString()
            if (dateChanged) "confirmedPlace" to (null as FirestorePlaceCandidate?)
            "updatedAt" to Clock.System.now().toKoreaIsoString()
        }
        local.confirmDate(room.id, date)
    }
    override suspend fun confirmMeetingWithoutPlace(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.confirmedDate != null) { "날짜를 먼저 확정해주세요." }
        rooms.document(room.id).updateFields {
            "status" to MeetingStatus.MEETING_CONFIRMED.name
            "confirmedPlace" to (null as FirestorePlaceCandidate?)
            "updatedAt" to Clock.System.now().toKoreaIsoString()
        }
        local.confirmMeetingWithoutPlace(room.id)
    }
    override suspend fun saveStartLocation(location: UserStartLocation) {
        val room = getRoom(location.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val roomRef = rooms.document(room.id)
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.START_LOCATION_SUMMARIES) },
        ) {
            val batch = firestore.batch()
            batch.set(
                roomRef.collection(START_LOCATIONS).document(location.participantId),
                FirestoreStartLocation.from(location),
            )
            batch.set(
                roomRef.collection(START_LOCATION_SUMMARIES).document(location.participantId),
                FirestoreStartLocationSummary.from(location),
            )
            batch.updateFields(roomRef) {
                syncRevisionPath(RoomSyncCollection.START_LOCATION_SUMMARIES) to FieldValue.increment(1)
            }
            batch.commit()
            local.saveStartLocation(location)
        }
    }
    override fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode) = local.saveTransportMode(roomId, participantId, transportMode)
    override fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>) = local.saveAreaRecommendations(roomId, recommendations)
    override fun selectAreaCandidate(roomId: String, candidateId: String) = local.selectAreaCandidate(roomId, candidateId)
    override fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>) = local.savePlaceCandidates(roomId, candidates)
    override fun savePlaceVote(roomId: String, placeId: String, participantId: String, voteType: PlaceVoteType) = local.savePlaceVote(roomId, placeId, participantId, voteType)
    override suspend fun saveDestinationStationProposal(proposal: DestinationStationProposal) {
        val room = getRoom(proposal.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val roomRef = rooms.document(room.id)
        val normalized = proposal.copy(roomId = room.id)
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.DESTINATION_STATION_PROPOSALS) },
        ) {
            val batch = firestore.batch()
            batch.updateFields(roomRef) {
                "status" to MeetingStatus.PLACE_SELECTING.name
                "confirmedPlace" to (null as FirestorePlaceCandidate?)
                "updatedAt" to proposal.updatedAt.toKoreaIsoString()
                syncRevisionPath(RoomSyncCollection.DESTINATION_STATION_PROPOSALS) to FieldValue.increment(1)
            }
            batch.set(
                roomRef.collection(DESTINATION_STATION_PROPOSALS).document(proposal.participantId),
                FirestoreDestinationStationProposal.from(normalized),
            )
            batch.commit()
            local.saveDestinationStationProposal(normalized)
        }
    }

    override suspend fun saveDestinationStationVote(vote: DestinationStationVote) {
        val room = getRoom(vote.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(getDestinationStationProposals(room.id).any { it.station.id == vote.stationId }) {
            "현재 후보 목록에 없는 역입니다."
        }
        val roomRef = rooms.document(room.id)
        val normalized = vote.copy(roomId = room.id)
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.DESTINATION_STATION_VOTES) },
        ) {
            val batch = firestore.batch()
            batch.updateFields(roomRef) {
                "status" to MeetingStatus.PLACE_SELECTING.name
                "updatedAt" to vote.updatedAt.toKoreaIsoString()
                syncRevisionPath(RoomSyncCollection.DESTINATION_STATION_VOTES) to FieldValue.increment(1)
            }
            batch.set(
                roomRef.collection(DESTINATION_STATION_VOTES).document(vote.participantId),
                FirestoreDestinationStationVote.from(normalized),
            )
            batch.commit()
            local.saveDestinationStationVote(normalized)
        }
    }

    override suspend fun confirmPlace(roomId: String, place: PlaceCandidate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        rooms.document(room.id).updateFields {
            "status" to MeetingStatus.PLACE_CONFIRMED.name
            "confirmedPlace" to FirestorePlaceCandidate.from(place)
            "updatedAt" to Clock.System.now().toKoreaIsoString()
        }
        local.confirmPlace(room.id, place)
    }

    private suspend fun loadParticipants(roomId: String): List<Participant> =
        rooms.document(roomId).collection(PARTICIPANTS).get().documents.map { it.data<FirestoreParticipant>().toDomain() }

    private suspend fun loadCurrentUserParticipant(roomId: String): Participant? =
        rooms.document(roomId)
            .collection(PARTICIPANTS)
            .where { "authUid" equalTo currentAuthUid() }
            .get()
            .documents
            .firstOrNull()
            ?.data<FirestoreParticipant>()
            ?.toDomain()

    private suspend fun loadAvailabilities(roomId: String): List<Availability> =
        rooms.document(roomId).collection(AVAILABILITIES).get().documents.map { it.data<FirestoreAvailability>().toDomain() }

    private suspend fun loadParticipantAvailabilities(roomId: String, participantId: String): List<Availability> =
        rooms.document(roomId)
            .collection(AVAILABILITIES)
            .where { "participantId" equalTo participantId }
            .get()
            .documents
            .map { it.data<FirestoreAvailability>().toDomain() }

    private suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String): List<UserStartLocation> {
        val roomRef = rooms.document(roomId)
        val ownSnapshot = roomRef.collection(START_LOCATIONS).document(currentParticipantId).get()
        val ownLocation = ownSnapshot.takeIf { it.exists }?.data<FirestoreStartLocation>()?.toDomain()
        val summaries = roomRef.collection(START_LOCATION_SUMMARIES)
            .get()
            .documents
            .map { it.data<FirestoreStartLocationSummary>().toDomain() }
        return mergeVisibleStartLocations(ownLocation, summaries)
    }

    private fun mergeVisibleStartLocations(
        ownLocation: UserStartLocation?,
        summaries: List<UserStartLocation>,
    ): List<UserStartLocation> = buildList {
        addAll(summaries.filterNot { it.participantId == ownLocation?.participantId })
        ownLocation?.let(::add)
    }

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

    private fun syncRevisionPath(collection: RoomSyncCollection) =
        FieldPath(ROOM_SYNC_REVISIONS_FIELD, collection.key)

    private fun availabilityRevisionPath(participantId: String) =
        FieldPath(ROOM_AVAILABILITY_REVISIONS_FIELD, participantId)

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
        const val MEMBERS = "members"
        const val AVAILABILITIES = "availabilities"
        const val START_LOCATIONS = "startLocations"
        const val START_LOCATION_SUMMARIES = "startLocationSummaries"
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

// FirestoreMeetingRoom과 분리해, 방 문서를 쓸 때 변경 번호가 함께 덮어써지지 않게 한다.
@Serializable
private data class FirestoreRoomSyncFields(
    @SerialName(ROOM_SYNC_REVISIONS_FIELD)
    val syncRevisions: Map<String, Long> = emptyMap(),
    @SerialName(ROOM_AVAILABILITY_REVISIONS_FIELD)
    val availabilityRevisions: Map<String, Long> = emptyMap(),
) {
    fun toDomain() = RoomSyncRevisions(collections = syncRevisions, availabilities = availabilityRevisions)
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
private data class FirestoreRoomMember(
    val authUid: String = "",
    val participantId: String = "",
    val role: String = "participant",
    val joined: Boolean = true,
    val updatedAt: String = "",
) {
    companion object {
        fun from(
            participant: Participant,
            authUid: String,
            role: String,
            joined: Boolean,
        ) = FirestoreRoomMember(
            authUid = authUid,
            participantId = participant.id,
            role = role,
            joined = joined,
            updatedAt = participant.joinedAt.toKoreaIsoString(),
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
private data class FirestoreStartLocationSummary(
    val participantId: String = "",
    val roomId: String = "",
    val label: String = "",
    val approximateLatitude: Double = 0.0,
    val approximateLongitude: Double = 0.0,
    val updatedAt: String = "",
) {
    fun toDomain(): UserStartLocation = UserStartLocation(
        participantId = participantId,
        roomId = roomId,
        label = label,
        address = null,
        latitude = approximateLatitude,
        longitude = approximateLongitude,
        privacyLevel = com.garam.whenwheremeet.domain.model.LocationPrivacyLevel.AREA_ONLY_VISIBLE,
        updatedAt = updatedAt.parseFirestoreInstant(),
    )

    companion object {
        fun from(location: UserStartLocation) = FirestoreStartLocationSummary(
            participantId = location.participantId,
            roomId = location.roomId,
            label = location.label,
            approximateLatitude = location.latitude.roundToAreaPrecision(),
            approximateLongitude = location.longitude.roundToAreaPrecision(),
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
private fun Double.roundToAreaPrecision(): Double = round(this * 100.0) / 100.0

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
