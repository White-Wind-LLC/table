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
    fun `years below 1000 have four digits`() {
        assertThat(formatLocalizedDate(LocalDate(5, 1, 2), "en-US")).isEqualTo("01/02/0005")
        assertThat(formatLocalizedDate(LocalDate(5, 1, 2), "de-DE")).isEqualTo("02.01.0005")
    }

    @Test
    fun `dates before 1582 are proleptic gregorian`() {
        assertThat(formatLocalizedDate(LocalDate(1500, 1, 1), "en-US")).isEqualTo("01/01/1500")
        assertThat(formatLocalizedDate(LocalDate(1500, 1, 1), "uk-UA")).isEqualTo("01.01.1500")
    }

    @Test
    fun `arabic dates keep the locale separators with ascii digits`() {
        // ar-EG puts a right-to-left mark (U+200F) before each slash.
        assertThat(formatLocalizedDate(LocalDate(2026, 10, 8), "ar-EG")).isEqualTo("08‏/10‏/2026")
    }

    @Test
    fun `arabic numbers use ascii digits and separators`() {
        assertThat(formatLocalizedNumber(1234.5, "ar-EG")).isEqualTo("1,234.5")
        assertThat(formatLocalizedNumber(1234, "fa-IR")).isEqualTo("1,234")
    }

    @Test
    fun `longs beyond double precision keep every digit`() {
        assertThat(formatLocalizedNumber(9007199254740993L, "en-US")).isEqualTo("9,007,199,254,740,993")
    }
}
