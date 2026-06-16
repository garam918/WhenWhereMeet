package com.garam.whenwheremeet.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareService(): ShareService {
    val context = LocalContext.current
    return remember(context) {
        object : ShareService {
            override fun share(text: String) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "약속 공유"))
            }
        }
    }
}
