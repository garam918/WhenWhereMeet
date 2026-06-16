package com.garam.whenwheremeet.platform

import kotlinx.datetime.LocalDate

actual fun currentLocalDate(): LocalDate = LocalDate.parse(java.time.LocalDate.now().toString())
