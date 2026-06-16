package com.garam.whenwheremeet.platform

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.garam.whenwheremeet.domain.model.PlaceCandidate

@Composable
actual fun rememberMapLauncher(): MapLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : MapLauncher {
            override fun openMap(place: PlaceCandidate) {
                val url = place.mapUrl ?: return
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}
