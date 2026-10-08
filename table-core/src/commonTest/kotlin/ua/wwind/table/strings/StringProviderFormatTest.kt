package ua.wwind.table.strings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import kotlin.test.Test

/** Format hooks on [StringProvider] (issue #106). */
@OptIn(ExperimentalTestApi::class)
class StringProviderFormatTest {
    private val numbersOnly =
        object : StringProvider {
            @Composable
            override fun get(key: UiString): String = DefaultStrings.get(key)

            @Composable
            override fun formatNumber(value: Number): String = "N:$value"
        }

    @Test
    fun `overriding one format keeps the locale default for the other`() =
        runComposeUiTest {
            val date = LocalDate(2026, 10, 8)
            var actualDate = ""
            var expectedDate = ""
            var number = ""
            setContent {
                actualDate = numbersOnly.formatDate(date)
                expectedDate = defaultFormatDate(date)
                number = numbersOnly.formatNumber(5)
            }
            assertThat(actualDate).isEqualTo(expectedDate)
            assertThat(number).isEqualTo("N:5")
        }

    @Test
    fun `default strings format through the locale defaults`() =
        runComposeUiTest {
            var actual = ""
            var expected = ""
            setContent {
                actual = DefaultStrings.formatNumber(1234.5)
                expected = defaultFormatNumber(1234.5)
            }
            assertThat(actual).isEqualTo(expected)
        }
}
