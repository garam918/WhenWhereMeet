package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cocoapods.FirebaseMessaging.FIRMessaging
import cocoapods.FirebaseMessaging.FIRMessagingDelegateProtocol
import com.tweener.alarmee.PushNotificationServiceRegistry
import com.tweener.alarmee.configuration.AlarmeeIosPlatformConfiguration
import com.tweener.alarmee.rememberAlarmeeMobileService
import dev.gitlive.firebase.Firebase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMeetingNotificationPlatform(): MeetingNotificationPlatform {
    val coroutineScope = rememberCoroutineScope()
    val alarmeeService = rememberAlarmeeMobileService(
        platformConfiguration = AlarmeeIosPlatformConfiguration,
        firebase = Firebase,
    )
    val messagingDelegate = remember { IosFirebaseMessagingDelegate() }
    LaunchedEffect(alarmeeService, messagingDelegate) {
        FIRMessaging.messaging().delegate = messagingDelegate
        UIApplication.sharedApplication.registerForRemoteNotifications()
    }

    return remember(alarmeeService, coroutineScope) {
        AlarmeeMeetingNotificationPlatform(
            pushService = alarmeeService.push,
            platformName = "ios",
            allowDeferredTokenRegistration = true,
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

@OptIn(ExperimentalForeignApi::class)
private class IosFirebaseMessagingDelegate : NSObject(), FIRMessagingDelegateProtocol {
    override fun messaging(messaging: FIRMessaging, didReceiveRegistrationToken: String?) {
        didReceiveRegistrationToken
            ?.takeIf(String::isNotBlank)
            ?.let(PushNotificationServiceRegistry::notifyTokenUpdated)
    }
}
