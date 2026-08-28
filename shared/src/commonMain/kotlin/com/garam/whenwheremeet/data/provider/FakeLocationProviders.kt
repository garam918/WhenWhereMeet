package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.LocationSearchResult
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.TravelTimeResult
import com.garam.whenwheremeet.domain.provider.LocationSearchProvider
import com.garam.whenwheremeet.domain.provider.TravelTimeProvider
import com.garam.whenwheremeet.domain.usecase.FindNearestTransitStationUseCase
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object SampleLocationData {
    val searchLocations = NationalTransitStationData.stations.map {
        location(it.id, it.name, it.latitude, it.longitude, "${it.region} · ${it.lines.joinToString("/")}")
    }

    val recommendedSearchLocations: List<LocationSearchResult> = listOf(
        "서울" to "서울역",
        "서울" to "강남역",
        "서울" to "홍대입구역",
        "경기" to "수원역",
        "인천" to "부평역",
        "부산" to "서면역",
        "대구" to "반월당역",
        "대전" to "대전역",
        "광주" to "광주송정역",
        "울산" to "태화강역",
    ).mapNotNull { (region, stationName) ->
        searchLocations.firstOrNull {
            it.label == stationName && it.address.orEmpty().startsWith(region)
        }
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

class NationalStationSearchProvider(
    private val findNearestStation: FindNearestTransitStationUseCase = FindNearestTransitStationUseCase(),
) : LocationSearchProvider {
    private val stationSearchEntries by lazy {
        SampleLocationData.searchLocations.map { location ->
            StationSearchEntry(location, location.stationSearchKeys())
        }
    }

    override suspend fun search(query: String): List<LocationSearchResult> {
        val normalizedQuery = query.normalizeStationSearchTerm()
        if (normalizedQuery.isBlank()) return SampleLocationData.recommendedSearchLocations

        return stationSearchEntries.asSequence()
            .filter { entry -> entry.searchKeys.any { it.contains(normalizedQuery) } }
            .sortedWith(
                compareBy<StationSearchEntry> {
                    it.location.stationSearchRank(normalizedQuery)
                }.thenBy { it.location.label }
                    .thenBy { it.location.address.orEmpty() },
            )
            .map { it.location }
            .toList()
    }

    override suspend fun findNearestStation(currentLocation: GeoPoint): LocationSearchResult? =
        findNearestStation(currentLocation, NationalTransitStationData.stations)?.let { station ->
            LocationSearchResult(
                id = station.id,
                label = station.name,
                address = "${station.region} · ${station.lines.joinToString("/")}",
                point = station.point,
            )
        }
}

@Deprecated("Use NationalStationSearchProvider", ReplaceWith("NationalStationSearchProvider"))
typealias FakeLocationSearchProvider = NationalStationSearchProvider

private data class StationSearchEntry(
    val location: LocationSearchResult,
    val searchKeys: List<String>,
)

private fun LocationSearchResult.stationSearchKeys(): List<String> {
    val metadata = address.orEmpty()
    val region = metadata.substringBefore(" · ")
    val province = region.substringBefore(' ')
    val lines = metadata.substringAfter(" · ", "").split('/').filter(String::isNotBlank)
    return (listOf(
        label,
        metadata,
        "$region $label",
        "$province $label",
        "$label $region",
    ) + lines.flatMap { line -> listOf("$line $label", "$label $line") })
        .map { it.normalizeStationSearchTerm() }
}

private fun LocationSearchResult.stationSearchRank(normalizedQuery: String): Int {
    val normalizedLabel = label.normalizeStationSearchTerm()
    val queryWithoutStationSuffix = normalizedQuery.removeSuffix("역")
    return when {
        normalizedLabel == normalizedQuery || normalizedLabel.removeSuffix("역") == queryWithoutStationSuffix -> 0
        normalizedLabel.startsWith(normalizedQuery) -> 1
        normalizedLabel.contains(normalizedQuery) -> 2
        else -> 3
    }
}

private fun String.normalizeStationSearchTerm(): String =
    lowercase().filterNot(Char::isWhitespace).replace("·", "")

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
