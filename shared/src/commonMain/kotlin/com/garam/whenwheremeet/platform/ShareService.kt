package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable

interface ShareService {
    fun share(text: String)
}

@Composable
expect fun rememberShareService(): ShareService
