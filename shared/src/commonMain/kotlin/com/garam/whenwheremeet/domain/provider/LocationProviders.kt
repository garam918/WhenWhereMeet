package com.garam.whenwheremeet.domain.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.TravelTimeResult

interface LocationSearchProvider {
    suspend fun search(query: String): List<LocationSearchResult>
    suspend fun getCurrentLocation(): LocationSearchResult?
}

interface TravelTimeProvider {
    suspend fun getTravelTime(
        origin: GeoPoint,
        destination: GeoPoint,
        transportMode: TransportMode,
    ): TravelTimeResult
}
