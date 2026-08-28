package com.garam.whenwheremeet.di

import com.garam.whenwheremeet.createMeetingRepository
import com.garam.whenwheremeet.data.local.KeyValueStorage
import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.data.provider.NationalStationSearchProvider
import com.garam.whenwheremeet.data.provider.FakePlaceSearchProvider
import com.garam.whenwheremeet.data.provider.GeminiPlaceRecommendationProvider
import com.garam.whenwheremeet.data.provider.MetropolitanMeetingAreaCandidateProvider
import com.garam.whenwheremeet.data.provider.StaticMetropolitanTransitTimeProvider
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.domain.provider.LocationSearchProvider
import com.garam.whenwheremeet.domain.provider.PlaceRecommendationProvider
import com.garam.whenwheremeet.domain.provider.PlaceSearchProvider
import com.garam.whenwheremeet.domain.provider.TravelTimeProvider
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.domain.usecase.RecommendMeetingAreasUseCase
import com.garam.whenwheremeet.presentation.state.MeetingAppState
import org.koin.dsl.module

val appModule = module {
    single<KeyValueStorage> { platformKeyValueStorage() }
    single { LocalMeetingRepository(get()) }
    single<MeetingRepository> { createMeetingRepository(get()) }

    single<LocationSearchProvider> { NationalStationSearchProvider() }
    single<TravelTimeProvider> { StaticMetropolitanTransitTimeProvider() }
    single { RecommendMeetingAreasUseCase(get()) }
    single<PlaceSearchProvider> { FakePlaceSearchProvider() }
    single<PlaceRecommendationProvider> { GeminiPlaceRecommendationProvider() }
    single { MetropolitanMeetingAreaCandidateProvider() }

    factory {
        MeetingAppState(
            repository = get(),
            locationSearchProvider = get(),
            recommendMeetingAreas = get(),
            placeSearchProvider = get(),
            placeRecommendationProvider = get(),
        )
    }
}
