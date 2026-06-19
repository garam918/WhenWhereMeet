package com.garam.whenwheremeet.data.local

interface KeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

expect fun platformKeyValueStorage(): KeyValueStorage
