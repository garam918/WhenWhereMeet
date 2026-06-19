package com.garam.whenwheremeet.domain.repository

import com.garam.whenwheremeet.domain.model.Availability
import com.garam.whenwheremeet.domain.model.MeetingRoom
import com.garam.whenwheremeet.domain.model.Participant
import com.garam.whenwheremeet.domain.model.AreaRecommendation
import com.garam.whenwheremeet.domain.model.ParticipantTravelPreference
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceVote
import com.garam.whenwheremeet.domain.model.PlaceVoteType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface MeetingRepository {
    fun getRooms(): List<MeetingRoom>
    fun getRoom(roomIdOrCode: String): MeetingRoom?
    fun getParticipants(roomId: String): List<Participant>
    fun getAvailabilities(roomId: String): List<Availability>
    fun getCurrentParticipantId(roomId: String): String?
    suspend fun getRoomForJoin(roomIdOrCode: String): MeetingRoom? = getRoom(roomIdOrCode)
    suspend fun refreshRoom(roomId: String) = Unit
    suspend fun refreshRooms() {
        getRooms().forEach { refreshRoom(it.id) }
    }
    fun observeRoom(roomId: String): Flow<Unit> = emptyFlow()
    suspend fun createRoom(room: MeetingRoom, host: Participant)
    suspend fun joinRoom(participant: Participant)
    suspend fun leaveRoom(roomId: String, participantId: String)
    suspend fun deleteRoom(roomId: String)
    suspend fun saveAvailabilities(roomId: String, participantId: String, values: Map<LocalDate, com.garam.whenwheremeet.domain.model.AvailabilityStatus>)
    suspend fun confirmDate(roomId: String, date: LocalDate)
    fun getStartLocations(roomId: String): List<UserStartLocation>
    fun getTravelPreferences(roomId: String): List<ParticipantTravelPreference>
    fun getAreaRecommendations(roomId: String): List<AreaRecommendation>
    fun saveStartLocation(location: UserStartLocation)
    fun saveTransportMode(roomId: String, participantId: String, transportMode: TransportMode)
    fun saveAreaRecommendations(roomId: String, recommendations: List<AreaRecommendation>)
    fun selectAreaCandidate(roomId: String, candidateId: String)
    fun getPlaceCandidates(roomId: String): List<ScoredPlaceCandidate>
    fun getPlaceVotes(roomId: String): List<PlaceVote>
    fun savePlaceCandidates(roomId: String, candidates: List<ScoredPlaceCandidate>)
    fun savePlaceVote(roomId: String, placeId: String, participantId: String, voteType: PlaceVoteType)
    fun confirmPlace(roomId: String, place: PlaceCandidate)
}
