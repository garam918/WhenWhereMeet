package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.provider.PlaceSearchProvider

data class KakaoPlaceApiConfig(val restApiKey: String)
data class NaverPlaceApiConfig(val clientId: String, val clientSecret: String)
data class GooglePlaceApiConfig(val apiKey: String)

class KakaoPlaceSearchProvider(
    private val config: KakaoPlaceApiConfig,
) : PlaceSearchProvider {
    override suspend fun searchPlaces(center: GeoPoint, meetingType: MeetingType, radiusMeters: Int, limit: Int): List<PlaceCandidate> {
        // TODO: Call Kakao Local API through a Ktor client or server proxy and map only required fields.
        error("Kakao Local API implementation requires injected credentials and an HTTP client")
    }
}

class NaverPlaceSearchProvider(
    private val config: NaverPlaceApiConfig,
) : PlaceSearchProvider {
    override suspend fun searchPlaces(center: GeoPoint, meetingType: MeetingType, radiusMeters: Int, limit: Int): List<PlaceCandidate> {
        // TODO: Prefer a server proxy because the client secret must not ship in a mobile binary.
        error("Naver place API implementation requires a secure server proxy")
    }
}

class GooglePlaceSearchProvider(
    private val config: GooglePlaceApiConfig,
) : PlaceSearchProvider {
    override suspend fun searchPlaces(center: GeoPoint, meetingType: MeetingType, radiusMeters: Int, limit: Int): List<PlaceCandidate> {
        // TODO: Call Google Places API with a platform-restricted key or server proxy.
        error("Google Places API implementation requires injected credentials and an HTTP client")
    }
}
