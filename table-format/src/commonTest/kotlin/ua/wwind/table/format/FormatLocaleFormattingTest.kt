package ua.wwind.table.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isNotNull
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.test.Test

/** The condition tab formats dates and numbers through [StringProvider], like the table's filters (issue #106). */
@OptIn(ExperimentalTestApi::class)
class FormatLocaleFormattingTest {
    private enum class Field { A }

    private val marked =
        object : StringProvider {
            @Composable
            override fun get(key: UiString): String = DefaultStrings.get(key)

            @Composable
            override fun formatDate(date: LocalDate): String = "D:$date"

            @Composable
            override fun formatNumber(value: Number): String = "N:$value"
        }

    private val from = LocalDate(2026, 10, 8)
    private val to = LocalDate(2026, 10, 9)

    private fun data(
        type: TableFilterType<*>,
        state: TableFilterState<*>,
    ) = FormatFilterData(Field.A, type, state, onChange = {})

    @Test
    fun `header title formats numbers and dates through the provider`() =
        runComposeUiTest {
            var number: String? = null
            var numberRange: String? = null
            var date: String? = null
            var dateRange: String? = null
            setContent {
                val numberType = TableFilterType.NumberTableFilter(delegate = IntDelegate)
                val dateType = TableFilterType.DateTableFilter()
                number =
                    buildFilterHeaderTitle(data(numberType, TableFilterState(FilterConstraint.GT, listOf(5))), marked)
                numberRange =
                    buildFilterHeaderTitle(
                        data(numberType, TableFilterState(FilterConstraint.BETWEEN, listOf(1, 9))),
                        marked,
                    )
                date =
                    buildFilterHeaderTitle(
                        data(dateType, TableFilterState(FilterConstraint.EQUALS, listOf(from))),
                        marked,
                    )
                dateRange =
                    buildFilterHeaderTitle(
                        data(dateType, TableFilterState(FilterConstraint.BETWEEN, listOf(from, to))),
                        marked,
                    )
            }
            assertThat(number).isNotNull().endsWith(" N:5")
            assertThat(numberRange).isNotNull().endsWith(" N:1 - N:9")
            assertThat(date).isNotNull().endsWith(" D:2026-10-08")
            assertThat(dateRange).isNotNull().endsWith(" D:2026-10-08 - D:2026-10-09")
        }

    @Test
    fun `date field shows the provider's date format`() =
        runComposeUiTest {
            setContent {
                FormatDateFilter(
                    filter = TableFilterType.DateTableFilter(),
                    state = TableFilterState(FilterConstraint.EQUALS, listOf(from)),
                    onChange = {},
                    strings = marked,
                )
            }
            onNodeWithText("D:2026-10-08").assertIsDisplayed()
        }
}
