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

internal fun Instant.toKoreaIsoDateTimeString(): String {
    val localDateTime = toLocalDateTime(KoreaTimeZone)
    return buildString {
        append(localDateTime.date)
        append('T')
        append(localDateTime.hour.twoDigits())
        append(':')
        append(localDateTime.minute.twoDigits())
        append(':')
        append(localDateTime.second.twoDigits())
        append("+09:00")
    }
}

internal fun String.toKoreaIsoDateTimeStringOrNull(): String? =
    runCatching { Instant.parse(this).toKoreaIsoDateTimeString() }.getOrNull()

internal fun currentKoreaDate(): LocalDate =
    Clock.System.now().toKoreaLocalDate()

expect fun currentLocalDate(): LocalDate

private fun Int.twoDigits(): String = toString().padStart(2, '0')
