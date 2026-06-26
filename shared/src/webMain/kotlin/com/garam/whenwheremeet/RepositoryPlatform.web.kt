package com.garam.whenwheremeet

import com.garam.whenwheremeet.data.local.platformKeyValueStorage
import com.garam.whenwheremeet.data.repository.LocalMeetingRepository
import com.garam.whenwheremeet.data.repository.WebFirebaseAuth
import com.garam.whenwheremeet.data.repository.WebFirestoreMeetingRepository
import com.garam.whenwheremeet.data.repository.webFirebaseConfig
import com.garam.whenwheremeet.domain.repository.MeetingRepository
import com.garam.whenwheremeet.platform.AuthSession
import kotlinx.browser.window

actual fun createMeetingRepository(localRepository: LocalMeetingRepository): MeetingRepository =
    webFirebaseConfig().let { config ->
        if (config.isConfigured) {
            WebFirestoreMeetingRepository(localRepository, config, WebFirebaseAuth(config))
        } else {
            localRepository
        }
    }

actual fun currentAuthSession(): AuthSession? =
    WebFirebaseAuth(webFirebaseConfig()).currentSession()

actual fun initialRoomCodeFromLaunch(): String? {
    val query = window.location.search.removePrefix("?")
    return query.split("&")
        .asSequence()
        .mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            if (pieces.size == 2) pieces[0] to pieces[1] else null
        }
        .firstOrNull { (key, _) -> key == "room" || key == "code" }
        ?.second
        ?.decodeUrlComponent()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
}

actual fun buildRoomJoinLink(roomId: String): String {
    val origin = window.location.origin
    val path = window.location.pathname.ifBlank { "/" }
    return "$origin$path?room=${roomId.urlEncode()}"
}

private fun String.decodeUrlComponent(): String =
    replace("+", " ").split("%").let { parts ->
        if (parts.size == 1) return@let this
        buildString {
            append(parts.first())
            parts.drop(1).forEach { part ->
                val hex = part.take(2)
                val rest = part.drop(2)
                val value = hex.toIntOrNull(radix = 16)
                if (value == null) {
                    append('%').append(part)
                } else {
                    append(value.toChar()).append(rest)
                }
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
            else -> "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
    }
