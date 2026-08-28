package com.garam.whenwheremeet.domain.usecase

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.TransitStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindNearestTransitStationUseCaseTest {
    private val findNearest = FindNearestTransitStationUseCase()

    @Test
    fun returnsStationClosestToCurrentCoordinates() {
        val stations = listOf(
            station("seoul", "서울역", 37.5547, 126.9706),
            station("gangnam", "강남역", 37.4979, 127.0276),
        )

        val result = findNearest(GeoPoint(37.4981, 127.0274), stations)

        assertEquals("gangnam", result?.id)
    }

    @Test
    fun returnsNullWhenStationSnapshotIsEmpty() {
        assertNull(findNearest(GeoPoint(37.5, 127.0), emptyList()))
    }

    private fun station(id: String, name: String, latitude: Double, longitude: Double) = TransitStation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        lines = listOf("테스트선"),
        region = "서울",
    )
}
