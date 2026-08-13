package com.garam.whenwheremeet.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

internal val KoreaTimeZone = UtcOffset(hours = 9).asTimeZone()

internal fun Instant.toKoreaLocalDate(): LocalDate =
    toLocalDateTime(KoreaTimeZone).date

internal fun currentKoreaDate(): LocalDate =
    Clock.System.now().toKoreaLocalDate()

expect fun currentLocalDate(): LocalDate
