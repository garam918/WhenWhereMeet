package com.garam.whenwheremeet.domain.provider

import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.ScoredPlaceCandidate

interface PlaceRecommendationProvider {
    suspend fun recommendPlaces(
        roomTitle: String,
        meetingType: MeetingType,
        startStations: List<String>,
        limit: Int = 5,
    ): List<ScoredPlaceCandidate>
}
