package com.garam.whenwheremeet.data.local

import android.content.Context

private var applicationContext: Context? = null

fun initializePlatformStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun platformKeyValueStorage(): KeyValueStorage {
    val context = checkNotNull(applicationContext) { "Platform storage must be initialized before App()" }
    val preferences = context.getSharedPreferences("when_where_meet", Context.MODE_PRIVATE)
    return object : KeyValueStorage {
        override fun getString(key: String): String? = preferences.getString(key, null)

        override fun putString(key: String, value: String) {
            preferences.edit().putString(key, value).apply()
        }

        override fun remove(key: String) {
            preferences.edit().remove(key).apply()
        }
    }
}
