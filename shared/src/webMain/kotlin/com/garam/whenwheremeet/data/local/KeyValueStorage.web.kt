package com.garam.whenwheremeet.data.local

import kotlinx.browser.window

actual fun platformKeyValueStorage(): KeyValueStorage {
    val localStorage = window.localStorage
    return object : KeyValueStorage {
        override fun getString(key: String): String? = localStorage.getItem(key)

        override fun putString(key: String, value: String) {
            localStorage.setItem(key, value)
        }

        override fun remove(key: String) {
            localStorage.removeItem(key)
        }
    }
}
