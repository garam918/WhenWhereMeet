package com.garam.whenwheremeet.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberShareService(): ShareService = remember {
    object : ShareService {
        override fun share(text: String) {
            window.open(url = "mailto:?body=${text.urlEncode()}", target = "_blank")
        }
    }
}

private fun String.urlEncode(): String =
    encodeToByteArray().joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xff
        when {
            value in 'A'.code..'Z'.code -> value.toChar().toString()
            value in 'a'.code..'z'.code -> value.toChar().toString()
            value in '0'.code..'9'.code -> value.toChar().toString()
            value == '-'.code || value == '_'.code || value == '.'.code || value == '~'.code -> value.toChar().toString()
            value == ' '.code -> "%20"
            else -> "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
    }
