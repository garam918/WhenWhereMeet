package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.AreaType
import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.MeetingAreaCandidate
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
    val searchLocations = listOf(
        location("gangnam", "강남역", 37.4979, 127.0276),
        location("hongdae", "홍대입구역", 37.5572, 126.9254),
        location("jamsil", "잠실역", 37.5133, 127.1001),
        location("sadang", "사당역", 37.4765, 126.9816),
        location("seoul", "서울역", 37.5547, 126.9706),
        location("yongsan", "용산역", 37.5298, 126.9648),
        location("sindorim", "신도림역", 37.5088, 126.8913),
        location("bupyeong", "부평역", 37.4895, 126.7245),
        location("pangyo", "판교역", 37.3948, 127.1112),
        location("yeouido", "여의도역", 37.5216, 126.9242),
    )

    val areaCandidates = listOf(
        candidate("gangnam", "강남역", 37.4979, 127.0276, AreaType.HOT_PLACE, "식사", "카페", "술자리", "스터디"),
        candidate("sadang", "사당역", 37.4765, 126.9816, AreaType.STATION, "식사", "카페", "술자리"),
        candidate("sindorim", "신도림역", 37.5088, 126.8913, AreaType.STATION, "식사", "카페"),
        candidate("seoul", "서울역", 37.5547, 126.9706, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디"),
        candidate("yongsan", "용산역", 37.5298, 126.9648, AreaType.STATION, "식사", "카페", "술자리"),
        candidate("hongdae", "홍대입구역", 37.5572, 126.9254, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("yeouido", "여의도역", 37.5216, 126.9242, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디", "운동"),
        candidate("jamsil", "잠실역", 37.5133, 127.1001, AreaType.HOT_PLACE, "식사", "카페", "운동"),
        candidate("konkuk", "건대입구역", 37.5404, 127.0693, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("hapjeong", "합정역", 37.5495, 126.9138, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("pangyo", "판교역", 37.3948, 127.1112, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디"),
    )

    private fun location(id: String, label: String, latitude: Double, longitude: Double) = LocationSearchResult(
        id = id,
        label = label,
        address = "${label.removeSuffix("역")} 인근",
        point = GeoPoint(latitude, longitude),
    )

    private fun candidate(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
        areaType: AreaType,
        vararg tags: String,
    ) = MeetingAreaCandidate(id, name, name, latitude, longitude, areaType, tags.toList())
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
