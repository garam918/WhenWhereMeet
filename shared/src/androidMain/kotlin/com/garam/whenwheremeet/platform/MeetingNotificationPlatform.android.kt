package com.garam.whenwheremeet.platform

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.garam.whenwheremeet.shared.R
import com.tweener.alarmee.DEFAULT_NOTIFICATION_CHANNEL_ID
import com.tweener.alarmee.channel.AlarmeeNotificationChannel
import com.tweener.alarmee.configuration.AlarmeeAndroidPlatformConfiguration
import com.tweener.alarmee.rememberAlarmeeMobileService
import dev.gitlive.firebase.Firebase

@Composable
actual fun rememberMeetingNotificationPlatform(): MeetingNotificationPlatform {
    val context = LocalContext.current
    val alarmeeService = rememberAlarmeeMobileService(
        platformConfiguration = remember {
            AlarmeeAndroidPlatformConfiguration(
                notificationIconResId = R.drawable.ic_notification,
                notificationIconColor = Color(0xFF5146E5),
                notificationChannels = listOf(
                    AlarmeeNotificationChannel(
                        id = DEFAULT_NOTIFICATION_CHANNEL_ID,
                        name = "약속 알림",
                        importance = NotificationManager.IMPORTANCE_HIGH,
                    ),
                ),
                useExactScheduling = false,
            )
        },
        firebase = Firebase,
    )
    var pendingPermissionResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPermissionResult?.invoke(granted)
        pendingPermissionResult = null
    }

    return remember(context, alarmeeService, permissionLauncher) {
        AlarmeeMeetingNotificationPlatform(
            pushService = alarmeeService.push,
            platformName = "android",
            allowDeferredTokenRegistration = true,
            requestSystemPermission = { onResult ->
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> onResult(true)
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> onResult(true)
                    else -> {
                        pendingPermissionResult = onResult
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
        )
    }
}
