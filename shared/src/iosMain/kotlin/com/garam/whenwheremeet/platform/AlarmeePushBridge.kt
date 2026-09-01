package com.garam.whenwheremeet.platform

import com.tweener.alarmee.PushNotificationServiceRegistry

class AlarmeePushBridge {
    fun onNotificationReceived(userInfo: Map<Any?, *>?) {
        val data = userInfo
            ?.mapNotNull { (key, value) ->
                val normalizedKey = key?.toString()
                val normalizedValue = value?.toString()
                if (normalizedKey != null && normalizedValue != null) normalizedKey to normalizedValue else null
            }
            ?.toMap()
            .orEmpty()
        PushNotificationServiceRegistry.notifyIncomingMessage(data)
    }
}
