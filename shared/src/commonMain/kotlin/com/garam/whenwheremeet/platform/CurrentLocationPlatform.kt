package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import com.garam.whenwheremeet.domain.model.GeoPoint

sealed interface CurrentLocationResult {
    data class Success(val point: GeoPoint) : CurrentLocationResult
    data object PermissionDenied : CurrentLocationResult
    data object Unavailable : CurrentLocationResult
    data object Unsupported : CurrentLocationResult
}

interface CurrentLocationPlatform {
    val isSupported: Boolean

    fun requestCurrentLocation(onResult: (CurrentLocationResult) -> Unit)
}

@Composable
expect fun rememberCurrentLocationPlatform(): CurrentLocationPlatform
