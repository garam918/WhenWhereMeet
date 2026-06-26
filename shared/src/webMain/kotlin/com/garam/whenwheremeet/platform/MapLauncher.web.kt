package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import kotlinx.browser.window

@Composable
actual fun rememberMapLauncher(): MapLauncher = remember {
    object : MapLauncher {
        override fun openMap(place: PlaceCandidate) {
            val url = place.mapUrl ?: return
            window.open(url = url, target = "_blank")
        }
    }
}
