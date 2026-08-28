package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberCurrentLocationPlatform(): CurrentLocationPlatform = remember {
    object : CurrentLocationPlatform {
        override val isSupported: Boolean = false

        override fun requestCurrentLocation(onResult: (CurrentLocationResult) -> Unit) {
            onResult(CurrentLocationResult.Unsupported)
        }
    }
}
