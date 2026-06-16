package com.garam.whenwheremeet.data.provider

import com.garam.whenwheremeet.domain.model.GeoPoint
import com.garam.whenwheremeet.domain.model.MeetingType
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import com.garam.whenwheremeet.domain.model.PlaceCategory
import com.garam.whenwheremeet.domain.model.PlaceSource
import com.garam.whenwheremeet.domain.provider.PlaceSearchProvider

class FakePlaceSearchProvider : PlaceSearchProvider {
    override suspend fun searchPlaces(
        center: GeoPoint,
        meetingType: MeetingType,
        radiusMeters: Int,
        limit: Int,
    ): List<PlaceCandidate> {
        val categories = categoriesFor(meetingType)
        return sampleTemplates.mapIndexed { index, template ->
            val offset = (index + 1) * 0.0007
            PlaceCandidate(
                id = "fake-${center.latitude}-${center.longitude}-${template.id}",
                name = template.name,
                category = template.category,
                address = "선택 지역 인근 ${index + 1}번지",
                roadAddress = "선택 지역 중심로 ${10 + index}",
                latitude = center.latitude + if (index % 2 == 0) offset else -offset,
                longitude = center.longitude + if (index % 3 == 0) -offset else offset,
                phoneNumber = "02-000-${1000 + index}",
                rating = template.rating,
                reviewCount = template.reviewCount,
                openingHoursSummary = if (index % 4 == 0) null else "매일 11:00 - 22:00",
                mapUrl = "https://www.google.com/maps/search/?api=1&query=${center.latitude + offset},${center.longitude + offset}",
                source = PlaceSource.FAKE,
            )
        }.filter { it.category in categories || meetingType == MeetingType.OTHER }
            .take(limit.coerceAtLeast(1))
    }

    private fun categoriesFor(meetingType: MeetingType): Set<PlaceCategory> = when (meetingType) {
        MeetingType.MEAL -> setOf(PlaceCategory.RESTAURANT)
        MeetingType.CAFE -> setOf(PlaceCategory.CAFE)
        MeetingType.DRINKS -> setOf(PlaceCategory.BAR, PlaceCategory.RESTAURANT)
        MeetingType.STUDY -> setOf(PlaceCategory.STUDY_ROOM, PlaceCategory.CAFE)
        MeetingType.EXERCISE -> setOf(PlaceCategory.ACTIVITY)
        MeetingType.OTHER -> PlaceCategory.entries.toSet()
    }

    private data class Template(
        val id: String,
        val name: String,
        val category: PlaceCategory,
        val rating: Double,
        val reviewCount: Int,
    )

    private val sampleTemplates = listOf(
        Template("table", "모임하기 좋은 테이블", PlaceCategory.RESTAURANT, 4.6, 328),
        Template("kitchen", "오늘의 키친", PlaceCategory.RESTAURANT, 4.3, 154),
        Template("coffee", "라운드 커피", PlaceCategory.CAFE, 4.7, 412),
        Template("quiet", "콰이어트 스터디 라운지", PlaceCategory.STUDY_ROOM, 4.5, 96),
        Template("pub", "퇴근길 펍", PlaceCategory.BAR, 4.4, 231),
        Template("sports", "액티브 스포츠 센터", PlaceCategory.ACTIVITY, 4.2, 88),
        Template("cafe2", "미팅 포인트 카페", PlaceCategory.CAFE, 4.1, 72),
        Template("etc", "커뮤니티 라운지", PlaceCategory.ETC, 4.0, 41),
    )
}

// TODO: Implement KakaoPlaceSearchProvider, NaverPlaceSearchProvider, or
// GooglePlaceSearchProvider behind PlaceSearchProvider. Inject credentials from
// platform configuration or a server proxy; never place secrets in common code.
