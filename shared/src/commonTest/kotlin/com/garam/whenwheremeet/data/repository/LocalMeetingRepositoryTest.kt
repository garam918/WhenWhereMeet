package com.garam.whenwheremeet.data.repository

import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.MeetingStatus
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class LocalMeetingRepositoryTest {
    @Test
    fun confirmingPlaceChangesRoomStatusAndKeepsDate() {
        val repository = LocalMeetingRepository(MemoryStorage())
        val now = Instant.parse("2026-06-01T00:00:00Z")
        val room = MeetingRoom(
            id = "room",
            title = "테스트",
            meetingType = MeetingType.MEAL,
            dateRangeStart = LocalDate(2026, 6, 20),
            dateRangeEnd = LocalDate(2026, 6, 21),
            minParticipants = 1,
            hostParticipantId = "host",
            status = MeetingStatus.PLACE_SELECTING,
            confirmedDate = LocalDate(2026, 6, 21),
            selectedAreaCandidateId = "area",
            createdAt = now,
            updatedAt = now,
        )
        repository.createRoom(
            room,
            Participant("host", "room", "방장", true, false, now),
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

    private class MemoryStorage : KeyValueStorage {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) {
            values[key] = value
        }
    }
}
