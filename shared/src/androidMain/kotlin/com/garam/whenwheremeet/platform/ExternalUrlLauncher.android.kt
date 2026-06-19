package com.garam.whenwheremeet.platform

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExternalUrlLauncher(): ExternalUrlLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ExternalUrlLauncher {
            override fun openUrl(url: String) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }
}
