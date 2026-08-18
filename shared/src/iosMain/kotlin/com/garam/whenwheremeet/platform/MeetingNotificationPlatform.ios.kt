package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tweener.alarmee.configuration.AlarmeeIosPlatformConfiguration
import com.tweener.alarmee.rememberAlarmeeMobileService
import dev.gitlive.firebase.Firebase
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberMeetingNotificationPlatform(): MeetingNotificationPlatform {
    val coroutineScope = rememberCoroutineScope()
    val alarmeeService = rememberAlarmeeMobileService(
        platformConfiguration = AlarmeeIosPlatformConfiguration,
        firebase = Firebase,
    )
    LaunchedEffect(Unit) {
        UIApplication.sharedApplication.registerForRemoteNotifications()
    }

    return remember(alarmeeService, coroutineScope) {
        AlarmeeMeetingNotificationPlatform(
            alarmeeService = alarmeeService,
            platformName = "ios",
            requestSystemPermission = { onResult ->
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                ) { granted, _ ->
                    if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                    coroutineScope.launch { onResult(granted) }
                }
            },
        )
    }
}
