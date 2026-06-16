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

interface MeetingRepository {
    fun getRooms(): List<MeetingRoom>
    fun getRoom(roomIdOrCode: String): MeetingRoom?
    fun getParticipants(roomId: String): List<Participant>
    fun getAvailabilities(roomId: String): List<Availability>
    fun getCurrentParticipantId(roomId: String): String?
    fun createRoom(room: MeetingRoom, host: Participant)
    fun joinRoom(participant: Participant)
    fun saveAvailabilities(roomId: String, participantId: String, values: Map<LocalDate, com.garam.whenwheremeet.domain.model.AvailabilityStatus>)
    fun confirmDate(roomId: String, date: LocalDate)
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
