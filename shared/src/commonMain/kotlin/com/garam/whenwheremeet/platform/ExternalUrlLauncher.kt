package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable

interface ExternalUrlLauncher {
    fun openUrl(url: String)
}

@Composable
expect fun rememberExternalUrlLauncher(): ExternalUrlLauncher
