package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.FriendProfile
import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import web.http.RequestMethod
import web.http.fetch
import web.http.GET
import web.http.POST
import web.http.text
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant

class WebFirestoreMeetingRepository(
    private val local: LocalMeetingRepository,
    private val config: WebFirebaseConfig,
    private val auth: WebFirebaseAuth,
) : MeetingRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val roomSync = RoomSyncCoordinator(
        local = local,
        remote = object : RoomSyncRemoteSource {
            override suspend fun loadParticipants(roomId: String) =
                this@WebFirestoreMeetingRepository.loadParticipants(roomId)
            override suspend fun loadAvailabilities(roomId: String) =
                this@WebFirestoreMeetingRepository.loadAvailabilities(roomId)
            override suspend fun loadParticipantAvailabilities(roomId: String, participantId: String) =
                this@WebFirestoreMeetingRepository.loadParticipantAvailabilities(roomId, participantId)
            override suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String) =
                this@WebFirestoreMeetingRepository.loadVisibleStartLocations(roomId, currentParticipantId)
            override suspend fun loadDestinationStationProposals(roomId: String) =
                this@WebFirestoreMeetingRepository.loadDestinationStationProposals(roomId)
            override suspend fun loadDestinationStationVotes(roomId: String) =
                this@WebFirestoreMeetingRepository.loadDestinationStationVotes(roomId)
        },
        currentAccountId = { auth.currentSession()?.uid },
    )

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
        val roomIds = queryCurrentUserParticipantDocuments()
            .map { it.fields().stringField("roomId") }
            .filter { it.isNotBlank() }
            .distinct()
        roomIds.forEach { refreshRoom(it) }
        local.retainRooms(roomIds.toSet())
    }

    override fun clearLocalCache() = local.clearLocalCache()

    override suspend fun getRoomForJoin(roomIdOrCode: String): MeetingRoom? {
        val code = roomIdOrCode.normalizedRoomCode()
        local.getRoom(code)?.let { return it }
        val codeDoc = getDocument("roomCodes/$code")
        val roomId = codeDoc?.fields()?.stringField("roomId") ?: code
        val roomDoc = getDocument("meetingRooms/$roomId") ?: return null
        val room = roomDoc.fields().toMeetingRoom()
        val currentParticipant = loadCurrentUserParticipant(room.id)
        local.importRoom(
            room,
            listOfNotNull(currentParticipant),
            local.getCurrentParticipantId(room.id) ?: currentParticipant?.id,
        )
        if (currentParticipant != null) {
            applyRoomDocument(room.id, roomDoc)
        }
        return local.getRoom(room.id) ?: room
    }

    override suspend fun refreshRoom(roomId: String) {
        val remoteRoomId = local.getRoom(roomId)?.id ?: roomId.normalizedRoomCode()
        applyRoomDocument(remoteRoomId, getDocument("meetingRooms/$remoteRoomId"))
    }

    // 방 문서 1건으로 변경 번호를 비교하고, 바뀐 하위 컬렉션만 다시 읽는다.
    private suspend fun applyRoomDocument(roomId: String, roomDoc: JsonObject?) {
        if (roomDoc == null) {
            roomSync.removeLocalRoom(roomId)
            return
        }
        val fields = roomDoc.fields()
        roomSync.applyRemoteRoom(
            remoteRoom = fields.toMeetingRoom(),
            remoteRevisions = RoomSyncRevisions(
                collections = fields.longMapField(ROOM_SYNC_REVISIONS_FIELD),
                availabilities = fields.longMapField(ROOM_AVAILABILITY_REVISIONS_FIELD),
            ),
        )
    }

    override suspend fun refreshFriends() {
        local.importFriends(
            listDocuments("users/${currentAuthUid()}/friends").map { it.fields().toFriendProfile() },
        )
    }

    // REST에는 리스너가 없어 폴링한다. 변경이 없으면 한 번에 방 문서 1건만 읽는다.
    override fun observeRoom(roomId: String): Flow<Unit> = flow {
        while (true) {
            refreshRoom(roomId)
            emit(Unit)
            delay(5_000)
        }
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
        val authUid = currentAuthUid()
        val writes = mutableListOf(
            updateWrite("meetingRooms/$code", roomToStore.toFirestoreFields(participantCount = allParticipants.size, hostAuthUid = authUid), exists = false),
            updateWrite("roomCodes/$code", roomCodeFields(code, code, room.createdAt), exists = false),
            updateWrite("meetingRooms/$code/participants/${host.id}", hostToStore.toFirestoreFields(authUid), exists = false),
            updateWrite(
                "meetingRooms/$code/members/$authUid",
                roomMemberFields(authUid, host.id, role = "host", joined = true, updatedAt = host.joinedAt),
                exists = false,
            ),
            updateWrite("meetingRooms/$code/nicknameKeys/${host.nickname.nicknameKey()}", nicknameFields(host.id), exists = false),
        )
        invitedToStore.forEach { invited ->
            val invitedAuthUid = requireNotNull(invited.accountId)
            writes += updateWrite(
                "meetingRooms/$code/participants/${invited.id}",
                invited.toFirestoreFields(invitedAuthUid),
                exists = false,
            )
            writes += updateWrite(
                "meetingRooms/$code/members/$invitedAuthUid",
                roomMemberFields(invitedAuthUid, invited.id, role = "participant", joined = false, updatedAt = invited.joinedAt),
                exists = false,
            )
            writes += updateWrite(
                "meetingRooms/$code/nicknameKeys/${invited.nickname.nicknameKey()}",
                nicknameFields(invited.id),
                exists = false,
            )
        }
        commit(*writes.toTypedArray())
        local.createRoom(roomToStore, hostToStore.copy(accountId = authUid), invitedToStore)
        // 새 방에는 변경 번호 필드가 없으므로(0) 방금 쓴 로컬 데이터가 최신이다.
        local.saveAppliedRoomSyncRevisions(code, RoomSyncRevisions())
    }

    override suspend fun joinRoom(participant: Participant) {
        val room = getRoomForJoin(participant.roomId)
            ?: throw IllegalArgumentException("방 코드를 확인해주세요.")
        val participantToStore = participant.copy(roomId = room.id)
        val authUid = currentAuthUid()
        val existingParticipant = loadCurrentUserParticipant(room.id)
        if (existingParticipant != null) {
            if (existingParticipant.isInvited) {
                commit(
                    updateWrite(
                        "meetingRooms/${room.id}/participants/${existingParticipant.id}",
                        existingParticipant.copy(
                            isInvited = false,
                            joinedAt = participant.joinedAt,
                        ).toFirestoreFields(authUid),
                        exists = true,
                    ),
                    updateWrite(
                        "meetingRooms/${room.id}/members/$authUid",
                        roomMemberFields(
                            authUid,
                            existingParticipant.id,
                            role = "participant",
                            joined = true,
                            updatedAt = participant.joinedAt,
                        ),
                        exists = true,
                    ),
                    roomTransformWrite(room.id, listOf(syncRevisionFieldPath(RoomSyncCollection.PARTICIPANTS))),
                )
            }
            // 참여자 목록은 방 화면의 동기화가 변경 번호를 보고 다시 받는다.
            local.importRoom(room, local.getParticipants(room.id).ifEmpty { listOf(existingParticipant) }, existingParticipant.id)
            return
        }
        val currentRoomDoc = requireNotNull(getDocument("meetingRooms/${room.id}")) { "방 코드를 확인해주세요." }
        val currentParticipantCount = currentRoomDoc.fields().intField("participantCount")
        val currentMaxParticipants = currentRoomDoc.fields().intField("maxParticipants").coerceAtLeast(room.maxParticipants)
        require(currentParticipantCount < currentMaxParticipants) { "정원이 가득 차서 참여할 수 없습니다." }
        require(getDocument("meetingRooms/${room.id}/nicknameKeys/${participant.nickname.nicknameKey()}") == null) {
            "이미 사용 중인 닉네임입니다."
        }
        commit(
            roomUpdateWrite(
                roomId = room.id,
                fields = buildJsonObject {
                    put("participantCount", integerValue(currentParticipantCount + 1))
                    put("updatedAt", stringValue(participant.joinedAt.toKoreaIsoString()))
                },
                incrementFieldPaths = listOf(syncRevisionFieldPath(RoomSyncCollection.PARTICIPANTS)),
            ),
            updateWrite("meetingRooms/${room.id}/participants/${participant.id}", participantToStore.toFirestoreFields(authUid), exists = false),
            updateWrite(
                "meetingRooms/${room.id}/members/$authUid",
                roomMemberFields(authUid, participant.id, role = "participant", joined = true, updatedAt = participant.joinedAt),
                exists = false,
            ),
            updateWrite("meetingRooms/${room.id}/nicknameKeys/${participant.nickname.nicknameKey()}", nicknameFields(participant.id), exists = false),
        )
        // 새로 참여한 방은 적용된 변경 번호를 비워 방 화면에서 전체를 한 번 받게 한다.
        local.importRoom(room, listOf(participantToStore.copy(accountId = authUid)), participant.id)
        local.removeAppliedRoomSyncRevisions(room.id)
    }

    override suspend fun leaveRoom(roomId: String, participantId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.hostParticipantId != participantId) { "방장은 방을 나갈 수 없습니다." }
        val participant = getParticipants(room.id).firstOrNull { it.id == participantId }
            ?: throw IllegalArgumentException("참여자 정보를 찾을 수 없습니다.")
        val currentRoomDoc = requireNotNull(getDocument("meetingRooms/${room.id}")) { "방 정보를 찾을 수 없습니다." }
        val participantCount = (currentRoomDoc.fields().intField("participantCount") - 1).coerceAtLeast(1)
        val writes = mutableListOf<JsonObject>()
        writes += roomUpdateWrite(
            roomId = room.id,
            fields = buildJsonObject {
                put("participantCount", integerValue(participantCount))
                put("updatedAt", stringValue(Clock.System.now().toKoreaIsoString()))
            },
            incrementFieldPaths = RoomSyncCollection.entries.map(::syncRevisionFieldPath) +
                availabilityRevisionFieldPath(participantId),
        )
        writes += deleteWrite("meetingRooms/${room.id}/participants/$participantId")
        writes += deleteWrite("meetingRooms/${room.id}/members/${currentAuthUid()}")
        writes += deleteWrite("meetingRooms/${room.id}/nicknameKeys/${participant.nickname.nicknameKey()}")
        loadParticipantAvailabilities(room.id, participantId)
            .forEach { writes += deleteWrite("meetingRooms/${room.id}/availabilities/${participantId}-${it.date}") }
        writes += deleteWrite("meetingRooms/${room.id}/startLocations/$participantId")
        writes += deleteWrite("meetingRooms/${room.id}/startLocationSummaries/$participantId")
        writes += deleteWrite("meetingRooms/${room.id}/destinationStationProposals/$participantId")
        writes += deleteWrite("meetingRooms/${room.id}/destinationStationVotes/$participantId")
        commit(*writes.toTypedArray())
        local.leaveRoom(room.id, participantId)
    }

    override suspend fun deleteRoom(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        // Cloud Functions recursively removes private subcollections after the room document is deleted.
        commit(
            deleteWrite("roomCodes/${room.id.normalizedRoomCode()}"),
            deleteWrite("meetingRooms/${room.id}"),
        )
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
            val writes = mutableListOf<JsonObject>()
            changes.deletedDates.forEach {
                writes += deleteWrite("meetingRooms/${room.id}/availabilities/$participantId-$it")
            }
            changes.upserts.forEach { (date, status) ->
                writes += updateWrite(
                    "meetingRooms/${room.id}/availabilities/$participantId-$date",
                    Availability(room.id, participantId, date, status, now).toFirestoreFields(),
                    exists = null,
                )
            }
            writes += roomTransformWrite(room.id, listOf(availabilityRevisionFieldPath(participantId)))
            commit(*writes.toTypedArray())
            local.saveAvailabilities(room.id, participantId, values)
        }
    }

    override suspend fun confirmDate(roomId: String, date: LocalDate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val dateChanged = room.confirmedDate != null && room.confirmedDate != date
        commit(
            roomUpdateWrite(
                roomId = room.id,
                fields = buildJsonObject {
                    put("status", stringValue(MeetingStatus.DATE_CONFIRMED.name))
                    put("confirmedDate", stringValue(date.toString()))
                    put("updatedAt", stringValue(Clock.System.now().toKoreaIsoString()))
                },
                // 마스크에 있지만 fields에 없는 필드는 삭제된다.
                clearedFieldPaths = if (dateChanged) listOf("confirmedPlace") else emptyList(),
            ),
        )
        local.confirmDate(room.id, date)
    }

    override suspend fun confirmMeetingWithoutPlace(roomId: String) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(room.confirmedDate != null) { "날짜를 먼저 확정해주세요." }
        commit(
            roomUpdateWrite(
                roomId = room.id,
                fields = buildJsonObject {
                    put("status", stringValue(MeetingStatus.MEETING_CONFIRMED.name))
                    put("updatedAt", stringValue(Clock.System.now().toKoreaIsoString()))
                },
                clearedFieldPaths = listOf("confirmedPlace"),
            ),
        )
        local.confirmMeetingWithoutPlace(room.id)
    }

    override suspend fun saveStartLocation(location: UserStartLocation) {
        val room = getRoom(location.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.START_LOCATION_SUMMARIES) },
        ) {
            commit(
                updateWrite(
                    "meetingRooms/${room.id}/startLocations/${location.participantId}",
                    location.toFirestoreFields(),
                    exists = null,
                ),
                updateWrite(
                    "meetingRooms/${room.id}/startLocationSummaries/${location.participantId}",
                    location.toFirestoreSummaryFields(),
                    exists = null,
                ),
                roomTransformWrite(room.id, listOf(syncRevisionFieldPath(RoomSyncCollection.START_LOCATION_SUMMARIES))),
            )
            local.saveStartLocation(location.copy(roomId = room.id))
        }
    }
    override fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode) = local.saveTransportMode(roomId, participantId, transportMode)
    override fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>) = local.saveAreaRecommendations(roomId, recommendations)
    override fun selectAreaCandidate(roomId: String, candidateId: String) = local.selectAreaCandidate(roomId, candidateId)
    override fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>) = local.savePlaceCandidates(roomId, candidates)
    override fun savePlaceVote(roomId: String, placeId: String, participantId: String, voteType: PlaceVoteType) = local.savePlaceVote(roomId, placeId, participantId, voteType)
    override suspend fun saveDestinationStationProposal(proposal: DestinationStationProposal) {
        val room = getRoom(proposal.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        val normalized = proposal.copy(roomId = room.id)
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.DESTINATION_STATION_PROPOSALS) },
        ) {
            commit(
                roomUpdateWrite(
                    roomId = room.id,
                    fields = buildJsonObject {
                        put("status", stringValue(MeetingStatus.PLACE_SELECTING.name))
                        put("updatedAt", stringValue(proposal.updatedAt.toKoreaIsoString()))
                    },
                    clearedFieldPaths = listOf("confirmedPlace"),
                    incrementFieldPaths = listOf(syncRevisionFieldPath(RoomSyncCollection.DESTINATION_STATION_PROPOSALS)),
                ),
                updateWrite(
                    "meetingRooms/${room.id}/destinationStationProposals/${proposal.participantId}",
                    normalized.toFirestoreFields(),
                    exists = null,
                ),
            )
            local.saveDestinationStationProposal(normalized)
        }
    }

    override suspend fun saveDestinationStationVote(vote: DestinationStationVote) {
        val room = getRoom(vote.roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        require(getDestinationStationProposals(room.id).any { it.station.id == vote.stationId }) {
            "현재 후보 목록에 없는 역입니다."
        }
        val normalized = vote.copy(roomId = room.id)
        roomSync.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.DESTINATION_STATION_VOTES) },
        ) {
            commit(
                roomUpdateWrite(
                    roomId = room.id,
                    fields = buildJsonObject {
                        put("status", stringValue(MeetingStatus.PLACE_SELECTING.name))
                        put("updatedAt", stringValue(vote.updatedAt.toKoreaIsoString()))
                    },
                    incrementFieldPaths = listOf(syncRevisionFieldPath(RoomSyncCollection.DESTINATION_STATION_VOTES)),
                ),
                updateWrite(
                    "meetingRooms/${room.id}/destinationStationVotes/${vote.participantId}",
                    normalized.toFirestoreFields(),
                    exists = null,
                ),
            )
            local.saveDestinationStationVote(normalized)
        }
    }

    override suspend fun confirmPlace(roomId: String, place: PlaceCandidate) {
        val room = getRoom(roomId) ?: throw IllegalArgumentException("방 정보를 찾을 수 없습니다.")
        commit(
            roomUpdateWrite(
                roomId = room.id,
                fields = buildJsonObject {
                    put("status", stringValue(MeetingStatus.PLACE_CONFIRMED.name))
                    put("confirmedPlace", placeCandidateValue(place))
                    put("updatedAt", stringValue(Clock.System.now().toKoreaIsoString()))
                },
            ),
        )
        local.confirmPlace(room.id, place)
    }

    private suspend fun loadParticipants(roomId: String): List<Participant> =
        listDocuments("meetingRooms/$roomId/participants").map { it.fields().toParticipant() }

    private suspend fun loadCurrentUserParticipant(roomId: String): Participant? =
        queryCurrentUserParticipantDocuments()
            .firstOrNull { it.fields().stringField("roomId") == roomId }
            ?.fields()
            ?.toParticipant()

    private suspend fun loadAvailabilities(roomId: String): List<Availability> =
        listDocuments("meetingRooms/$roomId/availabilities").map { it.fields().toAvailability() }

    private suspend fun loadParticipantAvailabilities(roomId: String, participantId: String): List<Availability> =
        runEqualityQuery(
            parentPath = "meetingRooms/$roomId",
            collectionId = "availabilities",
            allDescendants = false,
            fieldPath = "participantId",
            value = participantId,
        ).map { it.fields().toAvailability() }

    private suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String): List<UserStartLocation> {
        val ownLocation = getDocument("meetingRooms/$roomId/startLocations/$currentParticipantId")
            ?.fields()
            ?.toStartLocation()
        val summaries = listDocuments("meetingRooms/$roomId/startLocationSummaries")
            .map { it.fields().toStartLocationSummary() }
            .filterNot { it.participantId == ownLocation?.participantId }
        return summaries + listOfNotNull(ownLocation)
    }

    private suspend fun loadDestinationStationProposals(roomId: String): List<DestinationStationProposal> =
        listDocuments("meetingRooms/$roomId/destinationStationProposals").map { it.fields().toDestinationStationProposal() }

    private suspend fun loadDestinationStationVotes(roomId: String): List<DestinationStationVote> =
        listDocuments("meetingRooms/$roomId/destinationStationVotes").map { it.fields().toDestinationStationVote() }

    private suspend fun getDocument(path: String): JsonObject? {
        val response = fetch(documentUrl(path), jsonRequest(RequestMethod.GET, bearerToken = currentIdToken()))
        val responseText = response.text()
        if (response.status.toInt() == 404) return null
        if (!response.ok) throw IllegalStateException(firebaseErrorMessage(response.status.toInt(), responseText))
        return json.parseToJsonElement(responseText).jsonObject
    }

    private suspend fun listDocuments(path: String): List<JsonObject> {
        val response = fetch(documentUrl(path), jsonRequest(RequestMethod.GET, bearerToken = currentIdToken()))
        val responseText = response.text()
        if (response.status.toInt() == 404) return emptyList()
        if (!response.ok) throw IllegalStateException(firebaseErrorMessage(response.status.toInt(), responseText))
        val body = json.parseToJsonElement(responseText).jsonObject
        return body["documents"]?.jsonArray?.map { it.jsonObject }.orEmpty()
    }

    private suspend fun queryCurrentUserParticipantDocuments(): List<JsonObject> =
        runEqualityQuery(
            parentPath = null,
            collectionId = "participants",
            allDescendants = true,
            fieldPath = "authUid",
            value = currentAuthUid(),
        )

    private suspend fun runEqualityQuery(
        parentPath: String?,
        collectionId: String,
        allDescendants: Boolean,
        fieldPath: String,
        value: String,
    ): List<JsonObject> {
        val body = buildJsonObject {
            put("structuredQuery", buildJsonObject {
                put("from", buildJsonArray {
                    add(buildJsonObject {
                        put("collectionId", collectionId)
                        put("allDescendants", allDescendants)
                    })
                })
                put("where", buildJsonObject {
                    put("fieldFilter", buildJsonObject {
                        put("field", buildJsonObject { put("fieldPath", fieldPath) })
                        put("op", "EQUAL")
                        put("value", stringValue(value))
                    })
                })
            })
        }.toString()
        val parentUrl = parentPath?.let(::documentUrl)
            ?: "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents"
        val response = fetch(
            url = "$parentUrl:runQuery",
            init = jsonRequest(RequestMethod.POST, body, currentIdToken()),
        )
        val responseText = response.text()
        if (!response.ok) throw IllegalStateException(firebaseErrorMessage(response.status.toInt(), responseText))
        return json.parseToJsonElement(responseText).jsonArray.mapNotNull { result ->
            result.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun commit(vararg writes: JsonObject) {
        if (writes.isEmpty()) return
        val body = buildJsonObject {
            put("writes", JsonArray(writes.toList()))
        }.toString()
        val response = fetch(
            url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents:commit",
            init = jsonRequest(RequestMethod.POST, body, currentIdToken()),
        )
        val responseText = response.text()
        if (!response.ok) throw IllegalStateException(firebaseErrorMessage(response.status.toInt(), responseText))
    }

    private fun currentAuthUid(): String {
        val session = requireNotNull(auth.currentSession()) { "로그인 정보를 확인할 수 없습니다." }
        require(!session.isAnonymous) { "Google 또는 Apple 계정으로 로그인해주세요." }
        return session.uid
    }

    private suspend fun currentIdToken(): String {
        return requireNotNull(auth.currentIdToken()) {
            "로그인이 필요해요. Google 또는 Apple 계정으로 다시 로그인해주세요."
        }
    }

    private fun documentUrl(path: String): String =
        "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/${config.databaseId}/documents/${path.encodePath()}"

    private fun documentName(path: String): String =
        "projects/${config.projectId}/databases/${config.databaseId}/documents/$path"

    private fun updateWrite(path: String, fields: JsonObject, exists: Boolean?): JsonObject = buildJsonObject {
        put("update", buildJsonObject {
            put("name", documentName(path))
            put("fields", fields)
        })
        if (exists != null) {
            put("currentDocument", buildJsonObject { put("exists", exists) })
        }
    }

    private fun deleteWrite(path: String): JsonObject = buildJsonObject {
        put("delete", documentName(path))
    }

    /**
     * 방 문서의 지정한 필드만 갱신한다. 전체 교체를 하면 변경 번호 필드가 지워지므로 항상 마스크를 쓴다.
     * [clearedFieldPaths]는 마스크에만 넣어 필드를 삭제한다.
     */
    private fun roomUpdateWrite(
        roomId: String,
        fields: JsonObject,
        clearedFieldPaths: List<String> = emptyList(),
        incrementFieldPaths: List<String> = emptyList(),
    ): JsonObject = buildJsonObject {
        put("update", buildJsonObject {
            put("name", documentName("meetingRooms/$roomId"))
            put("fields", fields)
        })
        put("updateMask", buildJsonObject {
            put("fieldPaths", buildJsonArray {
                (fields.keys + clearedFieldPaths).forEach { add(JsonPrimitive(it)) }
            })
        })
        if (incrementFieldPaths.isNotEmpty()) {
            put("updateTransforms", incrementTransforms(incrementFieldPaths))
        }
        put("currentDocument", buildJsonObject { put("exists", true) })
    }

    private fun roomTransformWrite(roomId: String, incrementFieldPaths: List<String>): JsonObject = buildJsonObject {
        put("transform", buildJsonObject {
            put("document", documentName("meetingRooms/$roomId"))
            put("fieldTransforms", incrementTransforms(incrementFieldPaths))
        })
        put("currentDocument", buildJsonObject { put("exists", true) })
    }

    private fun incrementTransforms(fieldPaths: List<String>): JsonArray = buildJsonArray {
        fieldPaths.forEach { path ->
            add(buildJsonObject {
                put("fieldPath", path)
                put("increment", integerValue(1))
            })
        }
    }

    private fun syncRevisionFieldPath(collection: RoomSyncCollection): String =
        "$ROOM_SYNC_REVISIONS_FIELD.${collection.key}"

    // 참여자 ID에는 '-'가 들어가므로 REST 필드 경로에서는 백틱으로 감싸야 한다.
    private fun availabilityRevisionFieldPath(participantId: String): String =
        "$ROOM_AVAILABILITY_REVISIONS_FIELD.`${participantId.replace("\\", "\\\\").replace("`", "\\`")}`"

    private fun firebaseErrorMessage(status: Int, responseText: String): String {
        val parsed = runCatching { json.parseToJsonElement(responseText).jsonObject }.getOrNull()
        val message = parsed?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        return when {
            status == 401 -> "웹 로그인 정보가 만료됐어요. 다시 시작해주세요."
            status == 403 -> "Firestore 접근 권한이 없어요. 보안 규칙과 웹 Firebase 설정을 확인해주세요."
            status == 404 -> "Firestore 데이터를 찾지 못했어요."
            !message.isNullOrBlank() -> message
            else -> "Firestore 요청에 실패했어요. ($status)"
        }
    }
}

private fun MeetingRoom.toFirestoreFields(participantCount: Int, hostAuthUid: String?): JsonObject = buildJsonObject {
    put("id", stringValue(id))
    put("title", stringValue(title))
    description?.let { put("description", stringValue(it)) }
    put("meetingType", stringValue(meetingType.name))
    put("dateRangeStart", stringValue(dateRangeStart.toString()))
    put("dateRangeEnd", stringValue(dateRangeEnd.toString()))
    put("minParticipants", integerValue(minParticipants))
    put("maxParticipants", integerValue(maxParticipants))
    put("participantCount", integerValue(participantCount))
    responseDeadline?.let { put("responseDeadline", stringValue(it.toString())) }
    put("hostParticipantId", stringValue(hostParticipantId))
    hostAuthUid?.let { put("hostAuthUid", stringValue(it)) }
    put("status", stringValue(status.name))
    confirmedDate?.let { put("confirmedDate", stringValue(it.toString())) }
    confirmedPlace?.let { put("confirmedPlace", placeCandidateValue(it)) }
    put("createdAt", stringValue(createdAt.toKoreaIsoString()))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun Participant.toFirestoreFields(authUid: String): JsonObject = buildJsonObject {
    put("id", stringValue(id))
    put("roomId", stringValue(roomId))
    put("nickname", stringValue(nickname))
    put("isHost", booleanValue(isHost))
    put("authUid", stringValue(authUid))
    put("isInvited", booleanValue(isInvited))
    put("joinedAt", stringValue(joinedAt.toKoreaIsoString()))
}

private fun roomMemberFields(
    authUid: String,
    participantId: String,
    role: String,
    joined: Boolean,
    updatedAt: Instant,
): JsonObject = buildJsonObject {
    put("authUid", stringValue(authUid))
    put("participantId", stringValue(participantId))
    put("role", stringValue(role))
    put("joined", booleanValue(joined))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun Availability.toFirestoreFields(): JsonObject = buildJsonObject {
    put("roomId", stringValue(roomId))
    put("participantId", stringValue(participantId))
    put("date", stringValue(date.toString()))
    put("status", stringValue(status.name))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun placeCandidateValue(place: PlaceCandidate): JsonObject = buildJsonObject {
    put("mapValue", buildJsonObject {
        put("fields", buildJsonObject {
            put("id", stringValue(place.id))
            put("name", stringValue(place.name))
            put("category", stringValue(place.category.name))
            place.address?.let { put("address", stringValue(it)) }
            place.roadAddress?.let { put("roadAddress", stringValue(it)) }
            put("latitude", doubleValue(place.latitude))
            put("longitude", doubleValue(place.longitude))
            place.phoneNumber?.let { put("phoneNumber", stringValue(it)) }
            place.rating?.let { put("rating", doubleValue(it)) }
            place.reviewCount?.let { put("reviewCount", integerValue(it)) }
            place.openingHoursSummary?.let { put("openingHoursSummary", stringValue(it)) }
            place.mapUrl?.let { put("mapUrl", stringValue(it)) }
            put("source", stringValue(place.source.name))
        })
    })
}

private fun UserStartLocation.toFirestoreFields(): JsonObject = buildJsonObject {
    put("participantId", stringValue(participantId))
    put("roomId", stringValue(roomId))
    put("label", stringValue(label))
    address?.let { put("address", stringValue(it)) }
    put("latitude", doubleValue(latitude))
    put("longitude", doubleValue(longitude))
    put("privacyLevel", stringValue(privacyLevel.name))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun UserStartLocation.toFirestoreSummaryFields(): JsonObject = buildJsonObject {
    put("participantId", stringValue(participantId))
    put("roomId", stringValue(roomId))
    put("label", stringValue(label))
    put("approximateLatitude", doubleValue(latitude.roundToAreaPrecision()))
    put("approximateLongitude", doubleValue(longitude.roundToAreaPrecision()))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun DestinationStationProposal.toFirestoreFields(): JsonObject = buildJsonObject {
    put("roomId", stringValue(roomId))
    put("participantId", stringValue(participantId))
    put("stationId", stringValue(station.id))
    put("stationName", stringValue(station.name))
    put("latitude", doubleValue(station.latitude))
    put("longitude", doubleValue(station.longitude))
    put("lines", arrayValue(station.lines.map(::stringValue)))
    put("region", stringValue(station.region))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun DestinationStationVote.toFirestoreFields(): JsonObject = buildJsonObject {
    put("roomId", stringValue(roomId))
    put("participantId", stringValue(participantId))
    put("stationId", stringValue(stationId))
    put("updatedAt", stringValue(updatedAt.toKoreaIsoString()))
}

private fun roomCodeFields(code: String, roomId: String, createdAt: Instant): JsonObject = buildJsonObject {
    put("code", stringValue(code))
    put("roomId", stringValue(roomId))
    put("createdAt", stringValue(createdAt.toKoreaIsoString()))
}

private fun nicknameFields(participantId: String): JsonObject = buildJsonObject {
    put("participantId", stringValue(participantId))
}

private fun JsonObject.toMeetingRoom(): MeetingRoom = MeetingRoom(
    id = stringField("id"),
    title = stringField("title"),
    description = optionalStringField("description"),
    meetingType = enumValueOf(optionalStringField("meetingType") ?: MeetingType.OTHER.name),
    dateRangeStart = LocalDate.parse(stringField("dateRangeStart")),
    dateRangeEnd = LocalDate.parse(stringField("dateRangeEnd")),
    minParticipants = intField("minParticipants"),
    maxParticipants = intField("maxParticipants"),
    responseDeadline = optionalStringField("responseDeadline")?.let(LocalDate::parse),
    hostParticipantId = stringField("hostParticipantId"),
    status = enumValueOf(optionalStringField("status") ?: MeetingStatus.COLLECTING_AVAILABILITY.name),
    confirmedDate = optionalStringField("confirmedDate")?.let(LocalDate::parse),
    confirmedPlace = this["confirmedPlace"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject?.toPlaceCandidate(),
    createdAt = stringField("createdAt").parseFirestoreInstant(),
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toParticipant(): Participant = Participant(
    id = stringField("id"),
    roomId = stringField("roomId"),
    nickname = stringField("nickname"),
    isHost = boolField("isHost"),
    joinedAt = stringField("joinedAt").parseFirestoreInstant(),
    accountId = optionalStringField("authUid"),
    isInvited = boolField("isInvited"),
)

private fun JsonObject.toFriendProfile(): FriendProfile = FriendProfile(
    userId = stringField("userId"),
    nickname = stringField("nickname"),
    sharedMeetingCount = intField("sharedMeetingCount"),
    lastMetAt = stringField("lastMetAt").parseFirestoreInstant(),
)

private fun JsonObject.toAvailability(): Availability = Availability(
    roomId = stringField("roomId"),
    participantId = stringField("participantId"),
    date = LocalDate.parse(stringField("date")),
    status = enumValueOf(stringField("status")),
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toStartLocation(): UserStartLocation = UserStartLocation(
    participantId = stringField("participantId"),
    roomId = stringField("roomId"),
    label = stringField("label"),
    address = optionalStringField("address"),
    latitude = doubleField("latitude"),
    longitude = doubleField("longitude"),
    privacyLevel = enumValueOf(optionalStringField("privacyLevel") ?: "AREA_ONLY_VISIBLE"),
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toStartLocationSummary(): UserStartLocation = UserStartLocation(
    participantId = stringField("participantId"),
    roomId = stringField("roomId"),
    label = stringField("label"),
    address = null,
    latitude = doubleField("approximateLatitude"),
    longitude = doubleField("approximateLongitude"),
    privacyLevel = LocationPrivacyLevel.AREA_ONLY_VISIBLE,
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toDestinationStationProposal(): DestinationStationProposal = DestinationStationProposal(
    roomId = stringField("roomId"),
    participantId = stringField("participantId"),
    station = TransitStation(
        id = stringField("stationId"),
        name = stringField("stationName"),
        latitude = doubleField("latitude"),
        longitude = doubleField("longitude"),
        lines = stringArrayField("lines"),
        region = optionalStringField("region").orEmpty(),
    ),
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toDestinationStationVote(): DestinationStationVote = DestinationStationVote(
    roomId = stringField("roomId"),
    participantId = stringField("participantId"),
    stationId = stringField("stationId"),
    updatedAt = stringField("updatedAt").parseFirestoreInstant(),
)

private fun JsonObject.toPlaceCandidate(): PlaceCandidate = PlaceCandidate(
    id = stringField("id"),
    name = stringField("name"),
    category = enumValueOf(optionalStringField("category") ?: "ETC"),
    address = optionalStringField("address"),
    roadAddress = optionalStringField("roadAddress"),
    latitude = doubleField("latitude"),
    longitude = doubleField("longitude"),
    phoneNumber = optionalStringField("phoneNumber"),
    rating = optionalDoubleField("rating"),
    reviewCount = optionalIntField("reviewCount"),
    openingHoursSummary = optionalStringField("openingHoursSummary"),
    mapUrl = optionalStringField("mapUrl"),
    source = enumValueOf(optionalStringField("source") ?: "CUSTOM"),
)

private fun JsonObject.fields(): JsonObject = this["fields"]?.jsonObject ?: JsonObject(emptyMap())

private fun JsonObject.stringField(key: String): String =
    this[key]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content.orEmpty()

private fun JsonObject.optionalStringField(key: String): String? =
    stringField(key).takeIf { it.isNotBlank() }

private fun JsonObject.intField(key: String): Int {
    val field = this[key]?.jsonObject ?: return 0
    return field["integerValue"]?.jsonPrimitive?.content?.toIntOrNull()
        ?: field["doubleValue"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
        ?: 0
}

private fun JsonObject.optionalIntField(key: String): Int? =
    if (containsKey(key)) intField(key) else null

private fun JsonObject.longMapField(key: String): Map<String, Long> =
    this[key]?.jsonObject
        ?.get("mapValue")?.jsonObject
        ?.get("fields")?.jsonObject
        ?.mapValues { (_, value) ->
            val field = value.jsonObject
            field["integerValue"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: field["doubleValue"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toLong()
                ?: 0L
        }
        .orEmpty()

private fun JsonObject.boolField(key: String): Boolean =
    this[key]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.content == "true"

private fun JsonObject.doubleField(key: String): Double {
    val field = this[key]?.jsonObject ?: return 0.0
    return field["doubleValue"]?.jsonPrimitive?.content?.toDoubleOrNull()
        ?: field["integerValue"]?.jsonPrimitive?.content?.toDoubleOrNull()
        ?: 0.0
}

private fun JsonObject.optionalDoubleField(key: String): Double? =
    if (containsKey(key)) doubleField(key) else null

private fun JsonObject.stringArrayField(key: String): List<String> =
    this[key]?.jsonObject
        ?.get("arrayValue")?.jsonObject
        ?.get("values")?.jsonArray
        ?.mapNotNull { it.jsonObject["stringValue"]?.jsonPrimitive?.contentOrNull }
        .orEmpty()

private fun stringValue(value: String): JsonObject = buildJsonObject { put("stringValue", value) }
private fun integerValue(value: Int): JsonObject = buildJsonObject { put("integerValue", value.toString()) }
private fun doubleValue(value: Double): JsonObject = buildJsonObject { put("doubleValue", value) }
private fun booleanValue(value: Boolean): JsonObject = buildJsonObject { put("booleanValue", value) }
private fun arrayValue(values: List<JsonElement>): JsonObject = buildJsonObject {
    put("arrayValue", buildJsonObject { put("values", buildJsonArray { values.forEach { add(it) } }) })
}

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

private fun String.encodePath(): String =
    split('/').joinToString("/") { it.urlEncode() }

private fun String.urlEncode(): String =
    encodeToByteArray().joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xff
        when {
            value in 'A'.code..'Z'.code -> value.toChar().toString()
            value in 'a'.code..'z'.code -> value.toChar().toString()
            value in '0'.code..'9'.code -> value.toChar().toString()
            value == '-'.code || value == '_'.code || value == '.'.code || value == '~'.code -> value.toChar().toString()
            else -> "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
    }
