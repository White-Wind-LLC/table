package ua.wwind.table.strings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.Test

/**
 * The defaults follow the platform's format locale, not the UI language: macOS with English (US)
 * and the Ukraine region formats dates as 08.10.2026.
 */
@OptIn(ExperimentalTestApi::class)
class LocaleDefaultsTest {
    @Test
    fun `defaults use the format locale rather than the display language`() =
        runComposeUiTest {
            val savedDefault = Locale.getDefault()
            val savedFormat = Locale.getDefault(Locale.Category.FORMAT)
            try {
                Locale.setDefault(Locale.forLanguageTag("en-US"))
                Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("uk-UA"))
                var date = ""
                var number = ""
                setContent {
                    date = defaultFormatDate(LocalDate(2026, 10, 8))
                    number = defaultFormatNumber(1234.5)
                }
                waitForIdle()
                assertThat(date).isEqualTo("08.10.2026")
                assertThat(number).isEqualTo("1 234,5")
            } finally {
                Locale.setDefault(savedDefault)
                Locale.setDefault(Locale.Category.FORMAT, savedFormat)
            }
        }
}
