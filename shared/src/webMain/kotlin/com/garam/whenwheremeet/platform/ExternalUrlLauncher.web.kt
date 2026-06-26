package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberExternalUrlLauncher(): ExternalUrlLauncher = remember {
    object : ExternalUrlLauncher {
        override fun openUrl(url: String) {
            window.open(url = url, target = "_blank")
        }
    }
}
