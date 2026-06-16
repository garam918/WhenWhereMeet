package com.garam.whenwheremeet.data.local

import platform.Foundation.NSUserDefaults

actual fun platformKeyValueStorage(): KeyValueStorage {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : KeyValueStorage {
        override fun getString(key: String): String? = defaults.stringForKey(key)

        override fun putString(key: String, value: String) {
            defaults.setObject(value, forKey = key)
        }
    }
}
