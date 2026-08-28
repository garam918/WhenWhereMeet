package com.garam.whenwheremeet.data.provider

import kotlinx.coroutines.test.runTest
import com.garam.whenwheremeet.domain.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NationalStationSearchProviderTest {
    private val provider = NationalStationSearchProvider()

    @Test
    fun stationSnapshotCoversEveryUrbanRailRegionWithUniqueIds() {
        val stations = NationalTransitStationData.stations

        assertTrue(stations.size >= 900)
        assertEquals(stations.size, stations.map { it.id }.distinct().size)
        listOf("서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "경남", "경북", "충남", "강원")
            .forEach { region -> assertTrue(stations.any { it.region.startsWith(region) }, region) }
    }

    @Test
    fun searchFindsStationsOutsideMetropolitanAreaByStationRegionAndLine() = runTest {
        assertEquals("서면역", provider.search("부산 서면").first().label)
        assertEquals("반월당역", provider.search("대구 1호선 반월당").first().label)
        assertEquals("정부청사역", provider.search("정부청사").first().label)
        assertEquals("상무역", provider.search("광주 상무").first().label)
        assertEquals("태화강역", provider.search("울산 태화강역").first().label)
        assertEquals("구리(구리전통시장)역", provider.search("구리전통시장").first().label)
        assertEquals("동탄역", provider.search("GTX-A 동탄").first().label)
    }

    @Test
    fun transferStationRowsAreMergedWithAllLines() = runTest {
        val gangnam = provider.search("강남역").first {
            it.address.orEmpty().startsWith("서울")
        }

        assertEquals("강남역", gangnam.label)
        assertTrue(gangnam.address.orEmpty().contains("2호선"))
        assertTrue(gangnam.address.orEmpty().contains("신분당선"))
    }

    @Test
    fun blankQueryReturnsNationwideHubRecommendations() = runTest {
        val results = provider.search("")

        assertEquals(10, results.size)
        assertTrue(results.any { it.label == "서울역" })
        assertTrue(results.any { it.label == "서면역" })
        assertTrue(results.any { it.label == "반월당역" })
    }

    @Test
    fun currentCoordinatesResolveToNearestStation() = runTest {
        val result = provider.findNearestStation(GeoPoint(37.4980, 127.0277))

        assertEquals("강남역", result?.label)
        assertTrue(result?.address.orEmpty().contains("2호선"))
    }
}
