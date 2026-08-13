package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.DestinationStationProposal
import com.garam.whenwheremeet.domain.model.DestinationStationVote
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import com.garam.whenwheremeet.domain.model.TransitStation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant

class LocalMeetingRepositoryTest {
    @Test
    fun createRoomRejectsMoreThanEightParticipants() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "room",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 9,
            hostParticipantId = "host",
            status = MeetingStatus.COLLECTING_AVAILABILITY,
            createdAt = now,
            updatedAt = now,
        )

        assertFailsWith<IllegalArgumentException> {
            repository.createRoom(room, Participant("host", room.id, "방장", true, now))
        }
    }

    @Test
    fun confirmingPlaceChangesRoomStatusAndKeepsDate() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "room",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 2,
            hostParticipantId = "host",
            status = MeetingStatus.PLACE_SELECTING,
            confirmedDate = LocalDate(2026, 6, 21),
            selectedAreaCandidateId = "area",
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(
            room,
            Participant("host", "room", "방장", true, now),
        )
        val place = PlaceCandidate(
            id = "place",
            name = "확정 장소",
            category = PlaceCategory.RESTAURANT,
            latitude = 37.5,
            longitude = 127.0,
            source = PlaceSource.FAKE,
        )

        repository.confirmPlace(room.id, place)

        val updated = assertNotNull(repository.getRoom(room.id))
        assertEquals(MeetingStatus.PLACE_CONFIRMED, updated.status)
        assertEquals(LocalDate(2026, 6, 21), updated.confirmedDate)
        assertEquals(place, updated.confirmedPlace)
    }

    @Test
    fun changingConfirmedDateClearsConfirmedPlace() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val place = PlaceCandidate(
            id = "place",
            name = "기존 장소",
            category = PlaceCategory.RESTAURANT,
            latitude = 37.5,
            longitude = 127.0,
            source = PlaceSource.FAKE,
        )
        val room = MeetingRoom(
            id = "room",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 22),
            minParticipants = 1,
            maxParticipants = 2,
            hostParticipantId = "host",
            status = MeetingStatus.PLACE_CONFIRMED,
            confirmedDate = LocalDate(2026, 6, 21),
            selectedAreaCandidateId = "area",
            confirmedPlace = place,
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(room, Participant("host", room.id, "방장", true, now))

        repository.confirmDate(room.id, LocalDate(2026, 6, 22))

        val updated = assertNotNull(repository.getRoom(room.id))
        assertEquals(MeetingStatus.DATE_CONFIRMED, updated.status)
        assertEquals(LocalDate(2026, 6, 22), updated.confirmedDate)
        assertEquals(null, updated.selectedAreaCandidateId)
        assertEquals(null, updated.confirmedPlace)
    }

    @Test
    fun confirmingMeetingWithoutPlaceCreatesFinalStateAndKeepsDate() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "room",
            title = "장소 없는 약속",
            meetingType = MeetingType.OTHER,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 4,
            hostParticipantId = "host",
            status = MeetingStatus.DATE_CONFIRMED,
            confirmedDate = LocalDate(2026, 6, 21),
            selectedAreaCandidateId = "old-area",
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(room, Participant("host", room.id, "방장", true, now))

        repository.confirmMeetingWithoutPlace(room.id)

        val updated = assertNotNull(repository.getRoom(room.id))
        assertEquals(MeetingStatus.MEETING_CONFIRMED, updated.status)
        assertEquals(LocalDate(2026, 6, 21), updated.confirmedDate)
        assertEquals(null, updated.selectedAreaCandidateId)
        assertEquals(null, updated.confirmedPlace)
    }

    @Test
    fun invitedFriendBecomesFriendOnlyAfterJoining() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "ROOM12",
            title = "친구 초대 테스트",
            meetingType = MeetingType.CAFE,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 3,
            hostParticipantId = "host",
            status = MeetingStatus.COLLECTING_AVAILABILITY,
            createdAt = now,
            updatedAt = now,
        )
        val host = Participant("host", room.id, "방장", true, now, accountId = "host-user")
        val invited = Participant(
            "invite-1",
            room.id,
            "친구",
            false,
            now,
            accountId = "friend-user",
            isInvited = true,
        )
        repository.createRoom(room, host, listOf(invited))

        assertEquals(emptyList(), repository.getFriends())

        repository.joinRoom(
            Participant("temporary", room.id, "다른 닉네임", false, now, accountId = "friend-user"),
        )

        val joined = repository.getParticipants(room.id).first { it.id == invited.id }
        assertFalse(joined.isInvited)
        assertEquals(invited.id, repository.getCurrentParticipantId(room.id))
        assertEquals(listOf("host-user"), repository.getFriends().map { it.userId })
        assertEquals(1, repository.getFriends().single().sharedMeetingCount)
    }

    @Test
    fun joinRoomRejectsWhenRoomIsFullAndLeaveRemovesCurrentParticipant() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "ROOM12",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 2,
            hostParticipantId = "host",
            status = MeetingStatus.COLLECTING_AVAILABILITY,
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(room, Participant("host", room.id, "방장", true, now))
        repository.joinRoom(Participant("guest1", room.id, "손님", false, now))

        assertFailsWith<IllegalArgumentException> {
            repository.joinRoom(Participant("guest2", room.id, "다른손님", false, now))
        }

        repository.leaveRoom(room.id, "guest1")

        assertEquals(listOf("host"), repository.getParticipants(room.id).map { it.id })
        assertEquals(null, repository.getCurrentParticipantId(room.id))
    }

    @Test
    fun participantHasOneProposalAndOneVoteAtATime() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "room",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            maxParticipants = 2,
            hostParticipantId = "host",
            status = MeetingStatus.DATE_CONFIRMED,
            confirmedDate = LocalDate(2026, 6, 21),
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(room, Participant("host", room.id, "방장", true, now))
        val gangnam = TransitStation("gangnam", "강남역", 37.49, 127.02, listOf("2호선"), "서울")
        val hongdae = TransitStation("hongdae", "홍대입구역", 37.55, 126.92, listOf("2호선"), "서울")

        repository.saveDestinationStationProposal(DestinationStationProposal(room.id, "host", gangnam, now))
        repository.saveDestinationStationVote(DestinationStationVote(room.id, "host", gangnam.id, now))
        repository.saveDestinationStationProposal(DestinationStationProposal(room.id, "host", hongdae, now))
        repository.saveDestinationStationVote(DestinationStationVote(room.id, "host", hongdae.id, now))

        assertEquals(listOf(hongdae), repository.getDestinationStationProposals(room.id).map { it.station })
        assertEquals(listOf(hongdae.id), repository.getDestinationStationVotes(room.id).map { it.stationId })
        assertEquals(MeetingStatus.PLACE_SELECTING, repository.getRoom(room.id)?.status)
    }

    @Test
    fun retainRoomsKeepsOnlyMeetingsOwnedByTheCurrentAccount() = runTest {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val keptRoom = testRoom("KEEP12", now)
        val removedRoom = testRoom("DROP34", now)
        repository.createRoom(keptRoom, Participant("host", keptRoom.id, "첫 방장", true, now))
        repository.createRoom(removedRoom, Participant("host", removedRoom.id, "둘째 방장", true, now))

        repository.retainRooms(setOf(keptRoom.id))

        assertEquals(listOf(keptRoom.id), repository.getRooms().map { it.id })
        assertEquals(listOf("host"), repository.getParticipants(keptRoom.id).map { it.id })
        assertEquals(emptyList(), repository.getParticipants(removedRoom.id))
    }

    @Test
    fun clearLocalCacheRemovesPersistedMeetings() = runTest {
        val storage = MemoryStorage()
        val repository = LocalMeetingRepository(storage)
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = testRoom("CLEAR1", now)
        repository.createRoom(room, Participant("host", room.id, "방장", true, now))

        repository.clearLocalCache()

        assertEquals(emptyList(), repository.getRooms())
        assertEquals(emptyList(), LocalMeetingRepository(storage).getRooms())
    }

    private fun testRoom(id: String, now: Instant) = MeetingRoom(
        id = id,
        title = "테스트",
        meetingType = MeetingType.MEAL,
        dateRangeStart = LocalDate(2026, 6, 20),
        dateRangeEnd = LocalDate(2026, 6, 21),
        minParticipants = 1,
        maxParticipants = 2,
        hostParticipantId = "host",
        status = MeetingStatus.COLLECTING_AVAILABILITY,
        createdAt = now,
        updatedAt = now,
    )

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
