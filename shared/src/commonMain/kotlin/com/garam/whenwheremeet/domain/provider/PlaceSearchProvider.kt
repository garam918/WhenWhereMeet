package com.garam.whenwheremeet.domain.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate

interface PlaceSearchProvider {
    suspend fun searchPlaces(
        center: GeoPoint,
        meetingType: MeetingType,
        radiusMeters: Int,
        limit: Int,
    ): List<PlaceCandidate>
}
