package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.garam.whenwheremeet.domain.usecase.ExtractRoomCodeUseCase

private var pendingRoomCode by mutableStateOf<String?>(null)

fun submitIosDeepLinkUrl(url: String) {
    pendingRoomCode = ExtractRoomCodeUseCase()(url)
}

@Composable
actual fun PlatformDeepLinkEffect(onRoomCode: (String) -> Unit) {
    val roomCode = pendingRoomCode
    LaunchedEffect(roomCode) {
        roomCode?.let(onRoomCode)
        if (pendingRoomCode == roomCode) pendingRoomCode = null
    }
}
