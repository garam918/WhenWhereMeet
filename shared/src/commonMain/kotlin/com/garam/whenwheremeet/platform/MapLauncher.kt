package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import com.garam.whenwheremeet.domain.model.PlaceCandidate

interface MapLauncher {
    fun openMap(place: PlaceCandidate)
}

@Composable
expect fun rememberMapLauncher(): MapLauncher
