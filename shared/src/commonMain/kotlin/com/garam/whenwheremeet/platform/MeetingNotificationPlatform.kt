package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable

interface MeetingNotificationPlatform {
    val isSupported: Boolean

    fun requestPermission(onResult: (Boolean) -> Unit)

    suspend fun registerDevice(userId: String): Result<Unit>

    suspend fun unregisterDevice(userId: String)
}

@Composable
expect fun rememberMeetingNotificationPlatform(): MeetingNotificationPlatform
