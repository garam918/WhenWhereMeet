package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.AreaType
import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingAreaCandidate
import com.garam.whenwheremeet.domain.model.TransitDurationRecord
import com.garam.whenwheremeet.domain.model.TransitStation
import com.garam.whenwheremeet.domain.model.TransportMode
import com.garam.whenwheremeet.domain.model.TravelTimeResult
import com.garam.whenwheremeet.domain.model.UserStartLocation
import com.garam.whenwheremeet.domain.provider.TravelTimeProvider
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object MetropolitanTransitData {
    val stations = listOf(
        station("suwon", "수원역", 37.2657, 127.0001, "1호선", "수인분당선", region = "경기 수원"),
        station("anyang", "안양역", 37.4019, 126.9227, "1호선", region = "경기 안양"),
        station("bundang", "서현역", 37.3851, 127.1238, "수인분당선", region = "경기 성남"),
        station("pangyo", "판교역", 37.3948, 127.1112, "신분당선", "경강선", region = "경기 성남"),
        station("uijeongbu", "의정부역", 37.7381, 127.0459, "1호선", region = "경기 의정부"),
        station("ilsan", "정발산역", 37.6595, 126.7731, "3호선", region = "경기 고양"),
        station("bupyeong", "부평역", 37.4895, 126.7245, "1호선", "인천1호선", region = "인천 부평"),
        station("songdo", "인천대입구역", 37.3864, 126.6399, "인천1호선", region = "인천 송도"),
        station("gimpo_airport", "김포공항역", 37.5624, 126.8015, "5호선", "9호선", "공항철도", "김포골드라인", region = "서울 강서"),
        station("gangnam", "강남역", 37.4979, 127.0276, "2호선", "신분당선", region = "서울 강남"),
        station("jamsil", "잠실역", 37.5133, 127.1001, "2호선", "8호선", region = "서울 송파"),
        station("wangsimni", "왕십리역", 37.5612, 127.0371, "2호선", "5호선", "수인분당선", "경의중앙선", region = "서울 성동"),
        station("hongdae", "홍대입구역", 37.5572, 126.9254, "2호선", "공항철도", "경의중앙선", region = "서울 마포"),
        station("seoul", "서울역", 37.5547, 126.9706, "1호선", "4호선", "공항철도", "경의중앙선", region = "서울 중구"),
        station("sadang", "사당역", 37.4765, 126.9816, "2호선", "4호선", region = "서울 동작"),
        station("sindorim", "신도림역", 37.5088, 126.8913, "1호선", "2호선", region = "서울 구로"),
        station("yeouido", "여의도역", 37.5216, 126.9242, "5호선", "9호선", region = "서울 영등포"),
        station("konkuk", "건대입구역", 37.5404, 127.0693, "2호선", "7호선", region = "서울 광진"),
        station("hapjeong", "합정역", 37.5495, 126.9138, "2호선", "6호선", region = "서울 마포"),
        station("express_bus_terminal", "고속터미널역", 37.5048, 127.0049, "3호선", "7호선", "9호선", region = "서울 서초"),
        station("jongno3ga", "종로3가역", 37.5716, 126.9910, "1호선", "3호선", "5호선", region = "서울 종로"),
        station("guro_digital", "구로디지털단지역", 37.4853, 126.9015, "2호선", region = "서울 구로"),
    )

    val areaCandidates = listOf(
        candidate("gangnam", "강남역", 37.4979, 127.0276, AreaType.HOT_PLACE, "식사", "카페", "술자리", "스터디"),
        candidate("sadang", "사당역", 37.4765, 126.9816, AreaType.STATION, "식사", "카페", "술자리"),
        candidate("seoul", "서울역", 37.5547, 126.9706, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디"),
        candidate("hongdae", "홍대입구역", 37.5572, 126.9254, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("sindorim", "신도림역", 37.5088, 126.8913, AreaType.STATION, "식사", "카페"),
        candidate("yeouido", "여의도역", 37.5216, 126.9242, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디"),
        candidate("wangsimni", "왕십리역", 37.5612, 127.0371, AreaType.STATION, "식사", "카페", "술자리"),
        candidate("konkuk", "건대입구역", 37.5404, 127.0693, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("hapjeong", "합정역", 37.5495, 126.9138, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("express_bus_terminal", "고속터미널역", 37.5048, 127.0049, AreaType.STATION, "식사", "카페"),
        candidate("jongno3ga", "종로3가역", 37.5716, 126.9910, AreaType.STATION, "식사", "카페", "술자리"),
        candidate("jamsil", "잠실역", 37.5133, 127.1001, AreaType.HOT_PLACE, "식사", "카페", "운동"),
        candidate("guro_digital", "구로디지털단지역", 37.4853, 126.9015, AreaType.BUSINESS_DISTRICT, "식사", "카페", "술자리"),
        candidate("bupyeong", "부평역", 37.4895, 126.7245, AreaType.HOT_PLACE, "식사", "카페", "술자리"),
        candidate("pangyo", "판교역", 37.3948, 127.1112, AreaType.BUSINESS_DISTRICT, "식사", "카페", "스터디"),
    )

    val durationRecords = listOf(
        durations("suwon", 58, 42, 51, 72, 57, 64, 65, 72, 75, 50, 59, 66, 52, 92, 38),
        durations("anyang", 39, 20, 38, 55, 34, 45, 51, 58, 60, 32, 45, 53, 31, 76, 46),
        durations("bundang", 28, 42, 55, 70, 62, 60, 50, 42, 68, 38, 55, 35, 58, 92, 24),
        durations("pangyo", 24, 39, 53, 68, 60, 58, 48, 40, 66, 35, 53, 32, 56, 90, 0),
        durations("uijeongbu", 65, 73, 55, 72, 78, 70, 38, 50, 75, 64, 48, 58, 82, 105, 92),
        durations("ilsan", 70, 72, 48, 36, 47, 44, 61, 70, 34, 66, 54, 74, 60, 58, 94),
        durations("bupyeong", 74, 68, 49, 46, 39, 47, 69, 78, 44, 69, 62, 82, 43, 0, 95),
        durations("songdo", 88, 80, 72, 65, 62, 69, 84, 92, 63, 82, 76, 98, 66, 34, 108),
        durations("gimpo_airport", 66, 62, 38, 28, 32, 35, 55, 64, 30, 58, 46, 68, 42, 48, 88),
        durations("gangnam", 0, 18, 32, 36, 34, 30, 24, 20, 38, 14, 28, 26, 34, 74, 24),
        durations("jamsil", 26, 38, 42, 46, 48, 45, 26, 20, 50, 32, 36, 0, 48, 82, 32),
        durations("wangsimni", 24, 36, 22, 28, 36, 30, 0, 16, 31, 28, 18, 26, 40, 69, 48),
        durations("hongdae", 36, 35, 18, 0, 25, 15, 28, 36, 8, 33, 26, 46, 28, 46, 68),
        durations("seoul", 32, 33, 0, 18, 24, 17, 22, 32, 20, 27, 13, 42, 30, 49, 53),
        durations("sadang", 18, 0, 33, 35, 30, 34, 36, 38, 38, 14, 32, 38, 28, 68, 39),
        durations("sindorim", 34, 30, 24, 25, 0, 16, 36, 46, 25, 28, 33, 48, 12, 39, 60),
        durations("yeouido", 30, 34, 17, 15, 16, 0, 30, 45, 16, 28, 26, 45, 22, 47, 58),
        durations("konkuk", 20, 38, 32, 36, 46, 45, 16, 0, 38, 26, 25, 20, 50, 78, 40),
        durations("hapjeong", 38, 38, 20, 8, 25, 16, 31, 38, 0, 35, 28, 50, 30, 44, 66),
        durations("express_bus_terminal", 14, 14, 27, 33, 28, 28, 28, 26, 35, 0, 26, 32, 30, 69, 35),
        durations("jongno3ga", 28, 32, 13, 26, 33, 26, 18, 25, 28, 26, 0, 36, 39, 62, 53),
        durations("guro_digital", 34, 28, 30, 28, 12, 22, 40, 50, 30, 30, 39, 48, 0, 43, 56),
    ).flatten()

    val hubAreaIds = setOf("gangnam", "sadang", "seoul", "hongdae", "express_bus_terminal", "jongno3ga", "wangsimni")
    private val hubWeightByAreaId = mapOf(
        "gangnam" to 1.20,
        "sadang" to 1.15,
        "seoul" to 1.20,
        "hongdae" to 1.15,
        "sindorim" to 1.05,
        "yeouido" to 1.10,
        "wangsimni" to 1.05,
        "konkuk" to 1.08,
        "hapjeong" to 1.08,
        "express_bus_terminal" to 1.10,
        "jongno3ga" to 1.08,
        "jamsil" to 1.05,
    )

    fun hubWeight(areaId: String): Double = hubWeightByAreaId[areaId] ?: 1.0

    private fun station(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
        vararg lines: String,
        region: String,
    ) = TransitStation(id, name, latitude, longitude, lines.toList(), region)

    private fun candidate(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
        areaType: AreaType,
        vararg tags: String,
    ) = MeetingAreaCandidate(
        id = id,
        name = name,
        displayName = "$name · ${areaLabel(areaType)}",
        latitude = latitude,
        longitude = longitude,
        areaType = areaType,
        tags = tags.toList(),
    )

    private fun areaLabel(areaType: AreaType): String = when (areaType) {
        AreaType.STATION -> "환승 거점"
        AreaType.HOT_PLACE -> "주요 상권"
        AreaType.BUSINESS_DISTRICT -> "업무/상권"
        AreaType.CUSTOM -> "사용자 지정"
    }

    private fun durations(originStationId: String, vararg minutes: Int): List<TransitDurationRecord> {
        require(minutes.size == areaCandidates.size) { "duration count must match area candidates" }
        return areaCandidates.mapIndexed { index, candidate ->
            TransitDurationRecord(
                originStationId = originStationId,
                destinationAreaId = candidate.id,
                durationMinutes = minutes[index],
                transferCount = if (minutes[index] <= 25) 0 else if (minutes[index] <= 55) 1 else 2,
            )
        }
    }
}

class MetropolitanMeetingAreaCandidateProvider(
    private val candidates: List<MeetingAreaCandidate> = MetropolitanTransitData.areaCandidates,
) {
    fun getCandidates(
        startLocations: List<UserStartLocation>,
        limit: Int = 12,
    ): List<MeetingAreaCandidate> {
        if (startLocations.isEmpty()) return candidates.take(limit)
        val meanCenter = GeoPoint(
            latitude = startLocations.map { it.latitude }.average(),
            longitude = startLocations.map { it.longitude }.average(),
        )
        val medianCenter = GeoPoint(
            latitude = startLocations.map { it.latitude }.median(),
            longitude = startLocations.map { it.longitude }.median(),
        )
        val byMean = nearestCandidates(meanCenter, 8)
        val byMedian = nearestCandidates(medianCenter, 8)
        val hubs = candidates.filter { it.id in MetropolitanTransitData.hubAreaIds }

        return (byMean + byMedian + hubs)
            .distinctBy { it.id }
            .sortedBy { candidateScore(it, meanCenter, medianCenter) }
            .take(limit)
    }

    private fun nearestCandidates(center: GeoPoint, limit: Int): List<MeetingAreaCandidate> =
        candidates.sortedBy { haversineKilometers(center, it.point) }.take(limit)

    private fun candidateScore(candidate: MeetingAreaCandidate, meanCenter: GeoPoint, medianCenter: GeoPoint): Double {
        val centerDistance = (haversineKilometers(meanCenter, candidate.point) + haversineKilometers(medianCenter, candidate.point)) / 2
        val hubBonusKilometers = MetropolitanTransitData.hubWeight(candidate.id).coerceAtLeast(1.0) - 1.0
        return centerDistance - hubBonusKilometers * 2.5
    }
}

class StaticMetropolitanTransitTimeProvider(
    private val stations: List<TransitStation> = MetropolitanTransitData.stations,
    records: List<TransitDurationRecord> = MetropolitanTransitData.durationRecords,
) : TravelTimeProvider {
    private val recordByOriginAndDestination = records.associateBy { it.originStationId to it.destinationAreaId }

    override suspend fun getTravelTime(
        origin: GeoPoint,
        destination: GeoPoint,
        transportMode: TransportMode,
    ): TravelTimeResult {
        if (transportMode != TransportMode.PUBLIC_TRANSIT && transportMode != TransportMode.UNKNOWN) {
            return fallbackTravelTime(origin, destination, transportMode)
        }
        val originStation = stations.minBy { haversineKilometers(origin, it.point) }
        val destinationArea = MetropolitanTransitData.areaCandidates.minBy { haversineKilometers(destination, it.point) }
        val matrixMinutes = recordByOriginAndDestination[originStation.id to destinationArea.id]?.durationMinutes
        if (matrixMinutes == null) return fallbackTravelTime(origin, destination, TransportMode.PUBLIC_TRANSIT)

        val firstMileMinutes = walkingMinutes(origin, originStation.point)
        val lastMileMinutes = walkingMinutes(destinationArea.point, destination)
        return TravelTimeResult(
            minutes = max(3, matrixMinutes + firstMileMinutes + lastMileMinutes),
            distanceKilometers = haversineKilometers(origin, destination),
        )
    }

    private fun fallbackTravelTime(origin: GeoPoint, destination: GeoPoint, transportMode: TransportMode): TravelTimeResult {
        val distance = haversineKilometers(origin, destination)
        val speed = when (transportMode) {
            TransportMode.PUBLIC_TRANSIT, TransportMode.UNKNOWN -> 24.0
            TransportMode.CAR -> 30.0
            TransportMode.WALK -> 4.5
            TransportMode.BICYCLE -> 14.0
        }
        val overhead = when (transportMode) {
            TransportMode.PUBLIC_TRANSIT, TransportMode.UNKNOWN -> 12
            TransportMode.CAR -> 8
            TransportMode.WALK -> 2
            TransportMode.BICYCLE -> 4
        }
        return TravelTimeResult(
            minutes = max(3, (distance / speed * 60).roundToInt() + overhead),
            distanceKilometers = distance,
        )
    }
}

private fun walkingMinutes(origin: GeoPoint, destination: GeoPoint): Int =
    (haversineKilometers(origin, destination) * 1000 / 80.0).roundToInt()

private fun List<Double>.median(): Double {
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
}

private fun haversineKilometers(origin: GeoPoint, destination: GeoPoint): Double {
    val earthRadius = 6371.0
    val lat1 = origin.latitude * PI / 180
    val lat2 = destination.latitude * PI / 180
    val deltaLat = (destination.latitude - origin.latitude) * PI / 180
    val deltaLon = (destination.longitude - origin.longitude) * PI / 180
    val a = sin(deltaLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}
