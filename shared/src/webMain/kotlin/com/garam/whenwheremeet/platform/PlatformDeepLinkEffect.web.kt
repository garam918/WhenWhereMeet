package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.garam.whenwheremeet.domain.usecase.ExtractRoomCodeUseCase
import kotlinx.browser.window

@Composable
actual fun PlatformDeepLinkEffect(onRoomCode: (String) -> Unit) {
    val currentCallback = rememberUpdatedState(onRoomCode)
    LaunchedEffect(Unit) {
        ExtractRoomCodeUseCase()(window.location.href)?.let(currentCallback.value)
    }
}
