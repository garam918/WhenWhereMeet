package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberShareService(): ShareService = remember {
    object : ShareService {
        override fun share(text: String) {
            val controller = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                controller,
                animated = true,
                completion = null,
            )
        }
    }
}
