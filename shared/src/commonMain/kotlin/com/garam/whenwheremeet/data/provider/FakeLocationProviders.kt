package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.TravelTimeResult
import com.garam.whenwheremeet.domain.provider.LocationSearchProvider
import com.garam.whenwheremeet.domain.provider.TravelTimeProvider
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object SampleLocationData {
    val searchLocations = MetropolitanTransitData.stations.map {
        location(it.id, it.name, it.latitude, it.longitude, "${it.region} · ${it.lines.joinToString("/")}")
    }

    val areaCandidates = MetropolitanTransitData.areaCandidates

    private fun location(
        id: String,
        label: String,
        latitude: Double,
        longitude: Double,
        address: String,
    ) = LocationSearchResult(
        id = id,
        label = label,
        address = address,
        point = GeoPoint(latitude, longitude),
    )
}

class FakeLocationSearchProvider : LocationSearchProvider {
    override suspend fun search(query: String): List<LocationSearchResult> {
        val normalized = query.trim()
        return if (normalized.isBlank()) SampleLocationData.searchLocations else {
            SampleLocationData.searchLocations.filter {
                it.label.contains(normalized, ignoreCase = true) ||
                    it.address.orEmpty().contains(normalized, ignoreCase = true)
            }
        }
    }

    override suspend fun getCurrentLocation(): LocationSearchResult =
        SampleLocationData.searchLocations.first { it.id == "seoul" }
}

class FakeTravelTimeProvider : TravelTimeProvider {
    override suspend fun getTravelTime(
        origin: GeoPoint,
        destination: GeoPoint,
        transportMode: TransportMode,
    ): TravelTimeResult {
        val distance = haversineKilometers(origin, destination)
        val speed = when (transportMode) {
            TransportMode.PUBLIC_TRANSIT -> 24.0
            TransportMode.CAR -> 30.0
            TransportMode.WALK -> 4.5
            TransportMode.BICYCLE -> 14.0
            TransportMode.UNKNOWN -> 20.0
        }
        val overhead = when (transportMode) {
            TransportMode.PUBLIC_TRANSIT -> 12
            TransportMode.CAR -> 8
            TransportMode.WALK -> 2
            TransportMode.BICYCLE -> 4
            TransportMode.UNKNOWN -> 10
        }
        return TravelTimeResult(
            minutes = max(3, (distance / speed * 60).roundToInt() + overhead),
            distanceKilometers = distance,
        )
    }
}

private fun haversineKilometers(origin: GeoPoint, destination: GeoPoint): Double {
    val earthRadius = 6371.0
    val lat1 = origin.latitude * PI / 180
    val lat2 = destination.latitude * PI / 180
    val deltaLat = (destination.latitude - origin.latitude) * PI / 180
    val deltaLon = (destination.longitude - origin.longitude) * PI / 180
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}
