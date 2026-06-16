package com.garam.whenwheremeet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform