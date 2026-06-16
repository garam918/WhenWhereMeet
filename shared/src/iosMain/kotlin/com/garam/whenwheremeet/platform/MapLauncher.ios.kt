package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garam.whenwheremeet.domain.model.PlaceCandidate
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberMapLauncher(): MapLauncher = remember {
    object : MapLauncher {
        override fun openMap(place: PlaceCandidate) {
            val url = place.mapUrl?.let(NSURL::URLWithString) ?: return
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }
}
