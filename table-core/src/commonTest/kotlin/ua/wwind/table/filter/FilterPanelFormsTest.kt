package ua.wwind.table.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.test.Test

/** Labels and inline error text in the filter panel forms (issue #103). */
@OptIn(ExperimentalTestApi::class)
class FilterPanelFormsTest {
    @Test
    fun `text panel labels the condition and the value`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.TextTableFilter(),
                    state = null,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Condition").assertIsDisplayed()
            onNodeWithText("Value").assertIsDisplayed()
        }

    @Test
    fun `single date is labelled date whatever the operator`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.DateTableFilter(),
                    state = TableFilterState<LocalDate>(FilterConstraint.LT, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Date").assertIsDisplayed()
            onNodeWithText("From").assertDoesNotExist()
        }

    @Test
    fun `date range is labelled from and to`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.DateTableFilter(),
                    state = TableFilterState<LocalDate>(FilterConstraint.BETWEEN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("From").assertIsDisplayed()
            onNodeWithText("To").assertIsDisplayed()
        }

    @Test
    fun `inverted number range explains the error`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.NumberTableFilter(delegate = IntDelegate),
                    state = TableFilterState(FilterConstraint.BETWEEN, listOf(9, 5)),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("From must not be greater than To").assertIsDisplayed()
        }

    @Test
    fun `half filled number range asks for both values`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.NumberTableFilter(delegate = IntDelegate),
                    state = TableFilterState<Int>(FilterConstraint.BETWEEN, null),
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            onNodeWithText("Enter both values").assertDoesNotExist()
            onNodeWithText("From").performTextInput("3")
            onNodeWithText("Enter both values").assertIsDisplayed()
        }

    @Test
    fun `panel keeps its minimum width for short strings`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.TextTableFilter(),
                    state = null,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            // The fields fill the panel minus its 16 dp side padding.
            onNodeWithText("Value").assertWidthIsEqualTo(248.dp)
        }

    @Test
    fun `panel grows for a long translation up to its maximum width`() =
        runComposeUiTest {
            val longStrings =
                object : StringProvider {
                    @Composable
                    override fun get(key: UiString): String =
                        when (key) {
                            UiString.FilterClear -> "A very long translation of the clear action"
                            UiString.FilterApply -> "An equally long translation of apply"
                            else -> DefaultStrings.get(key)
                        }
                }
            setContent {
                FilterPanel(
                    type = TableFilterType.TextTableFilter(),
                    state = null,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = longStrings,
                    autoApplyFilters = false,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            val width = onNodeWithText("Value").getBoundsInRoot().let { it.right - it.left }
            assertThat(width).isBetween(249.dp, 328.dp)
        }

    @Test
    fun `single number is labelled value`() =
        runComposeUiTest {
            setContent {
                FilterPanel(
                    type = TableFilterType.NumberTableFilter(delegate = IntDelegate),
                    state = null,
                    tableData = Unit,
                    expanded = true,
                    onDismissRequest = {},
                    strings = DefaultStrings,
                    autoApplyFilters = true,
                    autoFilterDebounce = 0,
                    onChange = {},
                )
            }
            assertThat(onAllNodesWithText("Value").fetchSemanticsNodes().size).isEqualTo(1)
        }
}
