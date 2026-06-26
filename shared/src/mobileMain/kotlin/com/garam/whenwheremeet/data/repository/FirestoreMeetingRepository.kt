package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
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
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.repository.MeetingRepository
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
import kotlinx.datetime.TimeZone
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
    override fun getStartLocations(roomId: String): List<UserStartLocation> = local.getStartLocations(roomId)
    override fun getTravelPreferences(roomId: String): List<ParticipantTravelPreference> = local.getTravelPreferences(roomId)
    override fun getAreaRecommendations(roomId: String): List<AreaRecommendation> = local.getAreaRecommendations(roomId)
    override fun getPlaceCandidates(roomId: String): List<ScoredPlaceCandidate> = local.getPlaceCandidates(roomId)
    override fun getPlaceVotes(roomId: String): List<PlaceVote> = local.getPlaceVotes(roomId)

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
        local.importRoom(room, loadParticipants(room.id), local.getCurrentParticipantId(room.id))
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
        local.importRoom(
            room = mergeRemoteRoom(remoteRoom),
            participants = loadParticipants(remoteRoom.id),
            currentParticipantId = local.getCurrentParticipantId(remoteRoom.id),
        )
        local.importAvailabilities(remoteRoom.id, loadAvailabilities(remoteRoom.id))
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
        )
    }

    override suspend fun createRoom(room: MeetingRoom, host: Participant) {
        val code = room.id.normalizedRoomCode()
        val roomToStore = room.copy(id = code, maxParticipants = room.maxParticipants.coerceAtLeast(2))
        val hostToStore = host.copy(roomId = code)
        val hostAuthUid = currentAuthUid()
        firestore.runTransaction {
            val codeRef = roomCodes.document(code)
            require(!get(codeRef).exists) { "이미 존재하는 방 코드입니다." }
            val roomRef = rooms.document(code)
            set(roomRef, FirestoreMeetingRoom.from(roomToStore, participantCount = 1, hostAuthUid = hostAuthUid))
            set(codeRef, FirestoreRoomCode(code = code, roomId = code, createdAt = room.createdAt.toKoreaIsoString()))
            set(roomRef.collection(PARTICIPANTS).document(host.id), FirestoreParticipant.from(hostToStore, authUid = hostAuthUid))
            set(roomRef.collection(NICKNAMES).document(host.nickname.nicknameKey()), FirestoreNickname(participantId = host.id))
        }
        local.createRoom(roomToStore, hostToStore)
    }

    override suspend fun joinRoom(participant: Participant) {
        val room = getRoomForJoin(participant.roomId)
            ?: throw IllegalArgumentException("방 코드를 확인해주세요.")
        val participantToStore = participant.copy(roomId = room.id)
        val nicknameKey = participant.nickname.nicknameKey()
        val authUid = currentAuthUid()
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
            (loadParticipants(room.id) + participantToStore).distinctBy { it.id },
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
                    requiredParticipantIds = current.requiredParticipantIds - participantId,
                    updatedAt = now.toKoreaIsoString(),
                ),
            )
            delete(roomRef.collection(PARTICIPANTS).document(participantId))
            delete(roomRef.collection(NICKNAMES).document(participant.nickname.nicknameKey()))
            participantAvailabilities.forEach { delete(availabilityDocument(room.id, participantId, it.date)) }
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
    override fun saveStartLocation(location: UserStartLocation) = local.saveStartLocation(location)
    override fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode) = local.saveTransportMode(roomId, participantId, transportMode)
    override fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>) = local.saveAreaRecommendations(roomId, recommendations)
    override fun selectAreaCandidate(roomId: String, candidateId: String) = local.selectAreaCandidate(roomId, candidateId)
    override fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>) = local.savePlaceCandidates(roomId, candidates)
    override fun savePlaceVote(roomId: String, placeId: String, participantId: String, voteType: PlaceVoteType) = local.savePlaceVote(roomId, placeId, participantId, voteType)
    override fun confirmPlace(roomId: String, place: PlaceCandidate) = local.confirmPlace(roomId, place)

    private suspend fun loadParticipants(roomId: String): List<Participant> =
        rooms.document(roomId).collection(PARTICIPANTS).get().documents.map { it.data<FirestoreParticipant>().toDomain() }

    private suspend fun loadAvailabilities(roomId: String): List<Availability> =
        rooms.document(roomId).collection(AVAILABILITIES).get().documents.map { it.data<FirestoreAvailability>().toDomain() }

    private fun availabilityDocument(roomId: String, participantId: String, date: LocalDate) =
        rooms.document(roomId).collection(AVAILABILITIES).document("$participantId-${date}")

    private fun mergeRemoteRoom(remoteRoom: MeetingRoom): MeetingRoom {
        val localRoom = local.getRoom(remoteRoom.id) ?: return remoteRoom
        return remoteRoom.copy(
            selectedAreaCandidateId = localRoom.selectedAreaCandidateId,
            confirmedPlace = localRoom.confirmedPlace,
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

    private fun currentAuthUid(): String =
        requireNotNull(Firebase.auth.currentUser?.uid) { "로그인 정보를 확인할 수 없습니다." }

    private val rooms get() = firestore.collection(ROOMS)
    private val roomCodes get() = firestore.collection(ROOM_CODES)

    private companion object {
        const val FIRESTORE_DATABASE_ID = "default"
        const val ROOMS = "meetingRooms"
        const val ROOM_CODES = "roomCodes"
        const val PARTICIPANTS = "participants"
        const val AVAILABILITIES = "availabilities"
        const val NICKNAMES = "nicknameKeys"
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
    val requiredParticipantIds: List<String> = emptyList(),
    val status: String = MeetingStatus.COLLECTING_AVAILABILITY.name,
    val confirmedDate: String? = null,
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
        requiredParticipantIds = requiredParticipantIds,
        status = enumValueOf(status),
        confirmedDate = confirmedDate?.let(LocalDate::parse),
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
            requiredParticipantIds = room.requiredParticipantIds,
            status = room.status.name,
            confirmedDate = room.confirmedDate?.toString(),
            createdAt = room.createdAt.toKoreaIsoString(),
            updatedAt = room.updatedAt.toKoreaIsoString(),
        )
    }
}

@Serializable
private data class FirestoreParticipant(
    val id: String = "",
    val roomId: String = "",
    val nickname: String = "",
    val isHost: Boolean = false,
    val isRequired: Boolean = false,
    val authUid: String? = null,
    val joinedAt: String = "",
) {
    fun toDomain(): Participant = Participant(id, roomId, nickname, isHost, isRequired, joinedAt.parseFirestoreInstant())

    companion object {
        fun from(participant: Participant, authUid: String) = FirestoreParticipant(
            id = participant.id,
            roomId = participant.roomId,
            nickname = participant.nickname,
            isHost = participant.isHost,
            isRequired = participant.isRequired,
            authUid = authUid,
            joinedAt = participant.joinedAt.toKoreaIsoString(),
        )
    }
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

private val KoreaTimeZone = TimeZone.of("Asia/Seoul")

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
