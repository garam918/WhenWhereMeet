package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.UserStartLocation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** 방 문서에서 하위 컬렉션 변경 번호를 담는 필드 이름. 모든 클라이언트와 Functions가 같은 이름을 써야 한다. */
const val ROOM_SYNC_REVISIONS_FIELD = "syncRevisions"

/** 참여자별 가능 날짜 변경 번호를 담는 방 문서 필드 이름. */
const val ROOM_AVAILABILITY_REVISIONS_FIELD = "availabilityRevisions"

enum class RoomSyncCollection(val key: String) {
    PARTICIPANTS("participants"),
    START_LOCATION_SUMMARIES("startLocationSummaries"),
    DESTINATION_STATION_PROPOSALS("destinationStationProposals"),
    DESTINATION_STATION_VOTES("destinationStationVotes"),
}

/**
 * 하위 컬렉션을 쓸 때마다 같은 원자적 쓰기에서 증가시키는 변경 번호.
 * 필드가 없으면 0으로 본다.
 */
@Serializable
data class RoomSyncRevisions(
    val collections: Map<String, Long> = emptyMap(),
    val availabilities: Map<String, Long> = emptyMap(),
) {
    fun revisionOf(collection: RoomSyncCollection): Long = collections[collection.key] ?: 0L

    fun availabilityRevisionOf(participantId: String): Long = availabilities[participantId] ?: 0L

    fun incremented(collection: RoomSyncCollection): RoomSyncRevisions =
        copy(collections = collections + (collection.key to revisionOf(collection) + 1))

    fun incrementedAvailability(participantId: String): RoomSyncRevisions =
        copy(availabilities = availabilities + (participantId to availabilityRevisionOf(participantId) + 1))
}

data class RoomSyncPlan(
    val collections: Set<RoomSyncCollection>,
    val reloadAllAvailabilities: Boolean,
    val availabilityParticipantIds: Set<String>,
) {
    val isUpToDate: Boolean
        get() = collections.isEmpty() && !reloadAllAvailabilities && availabilityParticipantIds.isEmpty()

    companion object {
        val Full = RoomSyncPlan(
            collections = RoomSyncCollection.entries.toSet(),
            reloadAllAvailabilities = true,
            availabilityParticipantIds = emptySet(),
        )
    }
}

object RoomSyncPlanner {
    /**
     * 적용된 변경 번호가 없으면 전체를 다시 받는다.
     * 번호를 크기가 아니라 같은지로 비교해서, 값이 줄어든 경우에도 다시 받는다.
     */
    fun plan(remote: RoomSyncRevisions, applied: RoomSyncRevisions?): RoomSyncPlan {
        if (applied == null) return RoomSyncPlan.Full
        val collections = RoomSyncCollection.entries.filterTo(mutableSetOf()) {
            remote.revisionOf(it) != applied.revisionOf(it)
        }
        val participantIds = (remote.availabilities.keys + applied.availabilities.keys).filterTo(mutableSetOf()) {
            remote.availabilityRevisionOf(it) != applied.availabilityRevisionOf(it)
        }
        return RoomSyncPlan(
            collections = collections,
            reloadAllAvailabilities = false,
            availabilityParticipantIds = participantIds,
        )
    }
}

data class AvailabilityChanges(
    val upserts: Map<LocalDate, AvailabilityStatus>,
    val deletedDates: Set<LocalDate>,
) {
    val isEmpty: Boolean get() = upserts.isEmpty() && deletedDates.isEmpty()

    companion object {
        fun between(
            previous: Map<LocalDate, AvailabilityStatus>,
            next: Map<LocalDate, AvailabilityStatus>,
        ): AvailabilityChanges = AvailabilityChanges(
            upserts = next.filter { (date, status) -> previous[date] != status },
            deletedDates = previous.keys - next.keys,
        )
    }
}

/** 원격 저장소에서 방 하위 데이터를 읽는 최소 계약. 테스트에서는 fake로 대체한다. */
interface RoomSyncRemoteSource {
    suspend fun loadParticipants(roomId: String): List<Participant>
    suspend fun loadAvailabilities(roomId: String): List<Availability>
    suspend fun loadParticipantAvailabilities(roomId: String, participantId: String): List<Availability>
    suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String): List<UserStartLocation>
    suspend fun loadDestinationStationProposals(roomId: String): List<DestinationStationProposal>
    suspend fun loadDestinationStationVotes(roomId: String): List<DestinationStationVote>
}

/**
 * 방 문서의 변경 번호를 로컬 캐시와 비교해 바뀐 하위 데이터만 다시 읽는다.
 * 동기화와 내 쓰기의 변경 번호 반영은 같은 잠금 안에서 처리해 번호가 앞서가지 않게 한다.
 */
class RoomSyncCoordinator(
    private val local: LocalMeetingRepository,
    private val remote: RoomSyncRemoteSource,
    private val currentAccountId: () -> String?,
) {
    private val mutex = Mutex()

    suspend fun applyRemoteRoom(remoteRoom: MeetingRoom, remoteRevisions: RoomSyncRevisions) = mutex.withLock {
        val roomId = remoteRoom.id
        val mergedRoom = mergeRemoteRoom(remoteRoom)
        val knownParticipantId = local.getCurrentParticipantId(roomId)
        val applied = local.getAppliedRoomSyncRevisions(roomId)?.takeIf { knownParticipantId != null }
        val plan = RoomSyncPlanner.plan(remoteRevisions, applied)

        if (RoomSyncCollection.PARTICIPANTS in plan.collections) {
            val participants = remote.loadParticipants(roomId)
            val accountId = currentAccountId()
            local.importRoom(
                room = mergedRoom,
                participants = participants,
                currentParticipantId = knownParticipantId
                    ?: participants.firstOrNull { accountId != null && it.accountId == accountId }?.id,
            )
        } else {
            local.importRoomMetadata(mergedRoom)
        }

        if (plan.reloadAllAvailabilities) {
            local.importAvailabilities(roomId, remote.loadAvailabilities(roomId))
        } else if (plan.availabilityParticipantIds.isNotEmpty()) {
            local.replaceParticipantAvailabilities(
                roomId = roomId,
                participantIds = plan.availabilityParticipantIds,
                availabilities = plan.availabilityParticipantIds.flatMap {
                    remote.loadParticipantAvailabilities(roomId, it)
                },
            )
        }

        if (RoomSyncCollection.START_LOCATION_SUMMARIES in plan.collections) {
            local.getCurrentParticipantId(roomId)?.let { participantId ->
                local.importStartLocations(roomId, remote.loadVisibleStartLocations(roomId, participantId))
            }
        }
        if (RoomSyncCollection.DESTINATION_STATION_PROPOSALS in plan.collections) {
            local.importDestinationStationProposals(roomId, remote.loadDestinationStationProposals(roomId))
        }
        if (RoomSyncCollection.DESTINATION_STATION_VOTES in plan.collections) {
            local.importDestinationStationVotes(roomId, remote.loadDestinationStationVotes(roomId))
        }
        local.saveAppliedRoomSyncRevisions(roomId, remoteRevisions)
    }

    suspend fun removeLocalRoom(roomId: String) = mutex.withLock {
        local.getRoom(roomId)?.let { local.deleteRoom(it.id) }
    }

    /**
     * 내 쓰기를 커밋한 뒤 로컬 변경 번호도 같은 만큼 올린다.
     * 그 사이 다른 사람이 쓰면 원격 번호가 더 커져 다음 동기화에서 다시 받는다.
     */
    suspend fun <T> commitOwnChange(
        roomId: String,
        applyRevision: (RoomSyncRevisions) -> RoomSyncRevisions,
        commit: suspend () -> T,
    ): T = mutex.withLock {
        val result = commit()
        local.updateAppliedRoomSyncRevisions(roomId, applyRevision)
        result
    }

    private fun mergeRemoteRoom(remoteRoom: MeetingRoom): MeetingRoom {
        val localRoom = local.getRoom(remoteRoom.id) ?: return remoteRoom
        return remoteRoom.copy(
            selectedAreaCandidateId = localRoom.selectedAreaCandidateId ?: remoteRoom.selectedAreaCandidateId,
            confirmedPlace = localRoom.confirmedPlace ?: remoteRoom.confirmedPlace,
        )
    }
}
