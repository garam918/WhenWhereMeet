package com.garam.whenwheremeet.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class UserStartLocation(
    val participantId: String,
    val roomId: String,
    val label: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val privacyLevel: LocationPrivacyLevel,
    val updatedAt: Instant,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

@Serializable
enum class LocationPrivacyLevel {
    EXACT_PRIVATE,
    AREA_ONLY_VISIBLE,
}

@Serializable
enum class TransportMode(val label: String) {
    PUBLIC_TRANSIT("대중교통"),
    CAR("자동차"),
    WALK("도보"),
    BICYCLE("자전거"),
    UNKNOWN("미정"),
}

@Serializable
data class ParticipantTravelPreference(
    val participantId: String,
    val roomId: String,
    val transportMode: TransportMode,
    val updatedAt: Instant,
)

@Serializable
data class LocationSearchResult(
    val id: String,
    val label: String,
    val address: String? = null,
    val point: GeoPoint,
)

@Serializable
data class MeetingAreaCandidate(
    val id: String,
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val areaType: AreaType,
    val tags: List<String>,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

@Serializable
data class TransitStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lines: List<String>,
    val region: String,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

@Serializable
data class TransitDurationRecord(
    val originStationId: String,
    val destinationAreaId: String,
    val durationMinutes: Int,
    val transferCount: Int? = null,
)

@Serializable
enum class AreaType {
    STATION,
    HOT_PLACE,
    BUSINESS_DISTRICT,
    CUSTOM,
}

@Serializable
data class TravelTimeResult(
    val minutes: Int,
    val distanceKilometers: Double,
)

@Serializable
data class ParticipantTravelTime(
    val participantId: String,
    val participantNickname: String,
    val transportMode: TransportMode,
    val travelMinutes: Int,
)

@Serializable
data class AreaRecommendation(
    val candidate: MeetingAreaCandidate,
    val participantTravelTimes: List<ParticipantTravelTime>,
    val averageTravelMinutes: Int,
    val maxTravelMinutes: Int,
    val minTravelMinutes: Int,
    val travelTimeVariance: Double,
    val fairnessScore: Double,
    val recommendationReason: String,
)
