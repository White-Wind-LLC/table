package ua.wwind.table.platform

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/** Runs on every target, so each platform's formatter is held to the same output. */
class LocaleFormatCommonTest {
    @Test
    fun `dates stay gregorian where the locale calendar is not`() {
        // The date picker is always Gregorian; th-TH defaults to the Buddhist calendar (year 2569).
        assertThat(formatLocalizedDate(LocalDate(2026, 10, 8), "th-TH")).isEqualTo("08/10/2026")
    }

    @Test
    fun `longs beyond double precision keep every digit`() {
        assertThat(formatLocalizedNumber(9007199254740993L, "en-US")).isEqualTo("9,007,199,254,740,993")
    }
}
