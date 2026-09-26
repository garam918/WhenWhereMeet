package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.AvailabilityStatus
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.LocationPrivacyLevel
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.UserStartLocation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class RoomSyncCoordinatorTest {
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val room = MeetingRoom(
        id = "ROOM01",
        title = "동기화 테스트",
        meetingType = MeetingType.MEAL,
        dateRangeStart = LocalDate(2026, 10, 10),
        dateRangeEnd = LocalDate(2026, 10, 12),
        minParticipants = 2,
        maxParticipants = 4,
        hostParticipantId = "host",
        status = MeetingStatus.COLLECTING_AVAILABILITY,
        createdAt = now,
        updatedAt = now,
    )
    private val host = Participant("host", room.id, "방장", isHost = true, joinedAt = now, accountId = "host-uid")
    private val guest = Participant("guest", room.id, "참여자", isHost = false, joinedAt = now, accountId = "guest-uid")

    @Test
    fun firstSyncLoadsEveryCollectionOnceAndSavesRevisions() = runTest {
        val fixture = fixture()
        val revisions = RoomSyncRevisions(collections = mapOf(RoomSyncCollection.PARTICIPANTS.key to 2L))

        fixture.coordinator.applyRemoteRoom(room, revisions)

        assertEquals(
            listOf("participants", "availabilities", "startLocations", "proposals", "votes"),
            fixture.remote.calls,
        )
        assertEquals(listOf(host, guest), fixture.local.getParticipants(room.id))
        assertEquals(2, fixture.local.getAvailabilities(room.id).size)
        assertEquals(revisions, fixture.local.getAppliedRoomSyncRevisions(room.id))
    }

    @Test
    fun unchangedRevisionsSkipAllSubcollectionReads() = runTest {
        val fixture = fixture()
        val revisions = RoomSyncRevisions(availabilities = mapOf("guest" to 1L))
        fixture.coordinator.applyRemoteRoom(room, revisions)
        fixture.remote.calls.clear()

        fixture.coordinator.applyRemoteRoom(room.copy(title = "제목 변경"), revisions)

        assertEquals(emptyList(), fixture.remote.calls)
        assertEquals("제목 변경", fixture.local.getRoom(room.id)?.title)
    }

    @Test
    fun changedParticipantAvailabilityReloadsOnlyThatParticipant() = runTest {
        val fixture = fixture()
        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions())
        fixture.remote.calls.clear()
        fixture.remote.availabilities = listOf(
            availability("host", 10, AvailabilityStatus.MAYBE),
            availability("guest", 11, AvailabilityStatus.UNAVAILABLE),
        )

        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions().incrementedAvailability("guest"))

        assertEquals(listOf("availabilities:guest"), fixture.remote.calls)
        val stored = fixture.local.getAvailabilities(room.id).associate { it.participantId to it.status }
        // 번호가 바뀌지 않은 방장 응답은 다시 받지 않고 기존 값을 유지한다.
        assertEquals(AvailabilityStatus.AVAILABLE, stored["host"])
        assertEquals(AvailabilityStatus.UNAVAILABLE, stored["guest"])
    }

    @Test
    fun removedParticipantAvailabilityIsDroppedAfterReload() = runTest {
        val fixture = fixture()
        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions())
        fixture.remote.availabilities = listOf(availability("host", 10, AvailabilityStatus.AVAILABLE))

        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions().incrementedAvailability("guest"))

        assertEquals(listOf("host"), fixture.local.getAvailabilities(room.id).map { it.participantId })
    }

    @Test
    fun ownCommitAdvancesAppliedRevisionSoItsEchoIsSkipped() = runTest {
        val fixture = fixture()
        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions())
        fixture.remote.calls.clear()

        fixture.coordinator.commitOwnChange(room.id, applyRevision = { it.incrementedAvailability("host") }) {}
        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions().incrementedAvailability("host"))

        assertEquals(emptyList(), fixture.remote.calls)
    }

    @Test
    fun otherWriteAfterOwnCommitIsStillFetched() = runTest {
        val fixture = fixture()
        fixture.coordinator.applyRemoteRoom(room, RoomSyncRevisions())
        fixture.remote.calls.clear()

        fixture.coordinator.commitOwnChange(
            room.id,
            applyRevision = { it.incremented(RoomSyncCollection.DESTINATION_STATION_VOTES) },
        ) {}
        val remoteAfterTwoVotes = RoomSyncRevisions()
            .incremented(RoomSyncCollection.DESTINATION_STATION_VOTES)
            .incremented(RoomSyncCollection.DESTINATION_STATION_VOTES)
        fixture.coordinator.applyRemoteRoom(room, remoteAfterTwoVotes)

        assertEquals(listOf("votes"), fixture.remote.calls)
    }

    @Test
    fun clearedLocalCacheForcesFullSyncAgain() = runTest {
        val fixture = fixture()
        val revisions = RoomSyncRevisions(collections = mapOf(RoomSyncCollection.PARTICIPANTS.key to 1L))
        fixture.coordinator.applyRemoteRoom(room, revisions)
        fixture.local.clearLocalCache()
        fixture.local.importRoom(room, listOf(host), host.id)
        fixture.remote.calls.clear()

        fixture.coordinator.applyRemoteRoom(room, revisions)

        assertEquals(5, fixture.remote.calls.size)
    }

    @Test
    fun unknownCurrentParticipantIsResolvedFromAccountDuringFullSync() = runTest {
        val local = LocalMeetingRepository(MemoryStorage())
        local.importRoom(room, emptyList(), currentParticipantId = null)
        val remote = FakeRoomSyncRemoteSource(listOf(host, guest), emptyList())
        val coordinator = RoomSyncCoordinator(local, remote, currentAccountId = { "guest-uid" })

        coordinator.applyRemoteRoom(room, RoomSyncRevisions())

        assertEquals("guest", local.getCurrentParticipantId(room.id))
    }

    private fun fixture(): Fixture {
        val local = LocalMeetingRepository(MemoryStorage())
        local.importRoom(room, listOf(host), host.id)
        val remote = FakeRoomSyncRemoteSource(
            participants = listOf(host, guest),
            availabilities = listOf(
                availability("host", 10, AvailabilityStatus.AVAILABLE),
                availability("guest", 10, AvailabilityStatus.MAYBE),
            ),
        )
        return Fixture(local, remote, RoomSyncCoordinator(local, remote, currentAccountId = { "host-uid" }))
    }

    private fun availability(participantId: String, day: Int, status: AvailabilityStatus) =
        Availability(room.id, participantId, LocalDate(2026, 10, day), status, now)

    private class Fixture(
        val local: LocalMeetingRepository,
        val remote: FakeRoomSyncRemoteSource,
        val coordinator: RoomSyncCoordinator,
    )

    private class FakeRoomSyncRemoteSource(
        var participants: List<Participant>,
        var availabilities: List<Availability>,
    ) : RoomSyncRemoteSource {
        val calls = mutableListOf<String>()

        override suspend fun loadParticipants(roomId: String): List<Participant> {
            calls += "participants"
            return participants
        }

        override suspend fun loadAvailabilities(roomId: String): List<Availability> {
            calls += "availabilities"
            return availabilities
        }

        override suspend fun loadParticipantAvailabilities(roomId: String, participantId: String): List<Availability> {
            calls += "availabilities:$participantId"
            return availabilities.filter { it.participantId == participantId }
        }

        override suspend fun loadVisibleStartLocations(roomId: String, currentParticipantId: String): List<UserStartLocation> {
            calls += "startLocations"
            return listOf(
                UserStartLocation(
                    participantId = currentParticipantId,
                    roomId = roomId,
                    label = "서울역",
                    latitude = 37.55,
                    longitude = 126.97,
                    privacyLevel = LocationPrivacyLevel.AREA_ONLY_VISIBLE,
                    updatedAt = Instant.parse("2026-10-01T00:00:00Z"),
                ),
            )
        }

        override suspend fun loadDestinationStationProposals(roomId: String): List<DestinationStationProposal> {
            calls += "proposals"
            return emptyList()
        }

        override suspend fun loadDestinationStationVotes(roomId: String): List<DestinationStationVote> {
            calls += "votes"
            return emptyList()
        }
    }

    private class MemoryStorage : KeyValueStorage {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }
}
