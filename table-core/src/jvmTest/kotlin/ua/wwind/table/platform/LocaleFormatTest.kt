package ua.wwind.table.platform

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.Test

// JDK CLDR data; uk-UA groups with a no-break space (U+00A0).
class LocaleFormatTest {
    private val date = LocalDate(2026, 10, 8)

    @Test
    fun `date uses the locale order with a full year`() {
        assertThat(formatLocalizedDate(date, "en-US")).isEqualTo("10/08/2026")
        assertThat(formatLocalizedDate(date, "uk-UA")).isEqualTo("08.10.2026")
        assertThat(formatLocalizedDate(date, "de-DE")).isEqualTo("08.10.2026")
    }

    @Test
    fun `date before 1970 keeps its day`() {
        assertThat(formatLocalizedDate(LocalDate(1969, 12, 31), "en-US")).isEqualTo("12/31/1969")
    }

    @Test
    fun `empty and und tags use the default locale`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"))
            assertThat(formatLocalizedDate(date, "")).isEqualTo("08.10.2026")
            assertThat(formatLocalizedDate(date, "und")).isEqualTo("08.10.2026")
            assertThat(formatLocalizedNumber(1234.5, "")).isEqualTo("1 234,5")
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `numbers group and use the locale decimal separator`() {
        assertThat(formatLocalizedNumber(1234.5, "en-US")).isEqualTo("1,234.5")
        assertThat(formatLocalizedNumber(1234.5, "uk-UA")).isEqualTo("1 234,5")
        assertThat(formatLocalizedNumber(1234.5, "de-DE")).isEqualTo("1.234,5")
        assertThat(formatLocalizedNumber(1234, "en-US")).isEqualTo("1,234")
        assertThat(formatLocalizedNumber(-1234.5, "en-US")).isEqualTo("-1,234.5")
    }

    @Test
    fun `fraction digits are not cut and longs keep every digit`() {
        assertThat(formatLocalizedNumber(0.1234, "en-US")).isEqualTo("0.1234")
        assertThat(formatLocalizedNumber(12345678901L, "en-US")).isEqualTo("12,345,678,901")
    }

    @Test
    fun `float shows its decimal text, not binary noise`() {
        assertThat(formatLocalizedNumber(0.1f, "en-US")).isEqualTo("0.1")
    }

    @Test
    fun `non-finite numbers use toString`() {
        assertThat(formatLocalizedNumber(Double.NaN, "en-US")).isEqualTo("NaN")
        assertThat(formatLocalizedNumber(Double.POSITIVE_INFINITY, "en-US")).isEqualTo("Infinity")
        assertThat(formatLocalizedNumber(Float.NEGATIVE_INFINITY, "en-US")).isEqualTo("-Infinity")
    }
}
