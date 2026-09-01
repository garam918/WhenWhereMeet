package com.garam.whenwheremeet.platform

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CurrentDateTest {
    @Test
    fun koreaDateUsesFixedUtcPlusNineOffset() {
        assertEquals(
            LocalDate(2026, 8, 13),
            Instant.parse("2026-08-12T15:00:00Z").toKoreaLocalDate(),
        )
    }

    @Test
    fun koreaDateChangesAtKoreaMidnight() {
        assertEquals(
            LocalDate(2026, 8, 12),
            Instant.parse("2026-08-12T14:59:59Z").toKoreaLocalDate(),
        )
        assertEquals(
            LocalDate(2026, 8, 13),
            Instant.parse("2026-08-12T15:00:00Z").toKoreaLocalDate(),
        )
    }

    @Test
    fun koreaDateTimeUsesFirestoreIsoFormatWithUtcPlusNineOffset() {
        assertEquals(
            "2026-09-01T21:05:30+09:00",
            Instant.parse("2026-09-01T12:05:30Z").toKoreaIsoDateTimeString(),
        )
    }

    @Test
    fun utcFirestoreDateTimeCanBeNormalizedWithoutChangingTheInstant() {
        assertEquals(
            "2026-09-01T21:05:30+09:00",
            "2026-09-01T12:05:30Z".toKoreaIsoDateTimeStringOrNull(),
        )
    }
}
