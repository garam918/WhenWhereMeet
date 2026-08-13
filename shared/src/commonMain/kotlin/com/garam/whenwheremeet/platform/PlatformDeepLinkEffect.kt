package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformDeepLinkEffect(onRoomCode: (String) -> Unit)
