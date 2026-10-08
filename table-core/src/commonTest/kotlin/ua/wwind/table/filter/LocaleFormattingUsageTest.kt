package ua.wwind.table.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isNotNull
import kotlinx.datetime.LocalDate
import ua.wwind.table.buildFilterChipTextUnsafe
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.test.Test

/** The date field and the chips format through [StringProvider] (issue #106). */
@OptIn(ExperimentalTestApi::class)
class LocaleFormattingUsageTest {
    private val marked =
        object : StringProvider {
            @Composable
            override fun get(key: UiString): String = DefaultStrings.get(key)

            @Composable
            override fun formatDate(date: LocalDate): String = "D:$date"

            @Composable
            override fun formatNumber(value: Number): String = "N:$value"
        }

    private val date = LocalDate(2026, 10, 8)

    @Test
    fun `date field shows the provider's date format`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.DateTableFilter(),
                    state = TableFilterState(FilterConstraint.EQUALS, listOf(date)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = marked,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("D:2026-10-08").assertIsDisplayed()
        }

    @Test
    fun `chips format numbers and dates through the provider`() =
        runComposeUiTest {
            var single: String? = null
            var range: String? = null
            var dateChip: String? = null
            var dateRange: String? = null
            setContent {
                val numberType = TableFilterType.NumberTableFilter(delegate = IntDelegate)
                single = buildFilterChipTextUnsafe(numberType, TableFilterState(FilterConstraint.GT, listOf(5)), marked)
                range =
                    buildFilterChipTextUnsafe(
                        numberType,
                        TableFilterState(FilterConstraint.BETWEEN, listOf(1, 9)),
                        marked,
                    )
                dateChip =
                    buildFilterChipTextUnsafe(
                        TableFilterType.DateTableFilter(),
                        TableFilterState(FilterConstraint.EQUALS, listOf(date)),
                        marked,
                    )
                dateRange =
                    buildFilterChipTextUnsafe(
                        TableFilterType.DateTableFilter(),
                        TableFilterState(FilterConstraint.BETWEEN, listOf(date, LocalDate(2026, 10, 20))),
                        marked,
                    )
            }
            assertThat(single).isNotNull().endsWith(" N:5")
            assertThat(range).isNotNull().endsWith(" N:1 – N:9")
            assertThat(dateChip).isNotNull().endsWith(" D:2026-10-08")
            assertThat(dateRange).isNotNull().endsWith(" D:2026-10-08 – D:2026-10-20")
        }
}
