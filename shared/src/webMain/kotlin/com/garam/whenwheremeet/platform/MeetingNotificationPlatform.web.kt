package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMeetingNotificationPlatform(): MeetingNotificationPlatform = remember {
    object : MeetingNotificationPlatform {
        override val isSupported: Boolean = false

        override fun requestPermission(onResult: (Boolean) -> Unit) {
            onResult(false)
        }

        override suspend fun registerDevice(userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun unregisterDevice(userId: String) = Unit
    }
}
