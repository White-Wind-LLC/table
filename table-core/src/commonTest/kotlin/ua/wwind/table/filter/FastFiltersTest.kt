package ua.wwind.table.filter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import ua.wwind.table.Table
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

private enum class Fruit { Apple, Pear }

/** Clear buttons, operator hints and the labelled boolean of the fast filter row (issue #105). */
@OptIn(ExperimentalTestApi::class)
class FastFiltersTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("text", valueOf = { it }) {
                header("Text")
                width(200.dp, 200.dp)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
            column("number", valueOf = { it }) {
                header("Number")
                width(200.dp, 200.dp)
                filter(TableFilterType.NumberTableFilter(delegate = IntDelegate))
                cell { _, _ -> }
            }
            column("enum", valueOf = { it }) {
                header("Enum")
                width(200.dp, 200.dp)
                filter(TableFilterType.EnumTableFilter(options = persistentListOf(Fruit.Apple, Fruit.Pear)) { it.name })
                cell { _, _ -> }
            }
            column("bool", valueOf = { it }) {
                header("Bool")
                width(100.dp, 100.dp)
                filter(TableFilterType.BooleanTableFilter())
                cell { _, _ -> }
            }
            column("date", valueOf = { it }) {
                header("Date")
                width(200.dp, 200.dp)
                filter(TableFilterType.DateTableFilter())
                cell { _, _ -> }
            }
        }

    private fun ComposeUiTest.showTable(): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("text", "number", "enum", "bool", "date"),
                    settings = TableSettings(showFastFilters = true, autoFilterDebounce = 0),
                )
            Box(Modifier.size(1200.dp, 400.dp)) {
                Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = columns)
            }
        }
        waitForIdle()
        return state
    }

    private fun ComposeUiTest.setFilter(
        state: TableState<String>,
        column: String,
        filter: TableFilterState<*>,
    ) {
        state.setFilter(column, filter)
        waitForIdle()
    }

    @Test
    fun `an empty fast row has no clear buttons`() =
        runComposeUiTest {
            showTable()
            onNodeWithContentDescription("Clear").assertDoesNotExist()
        }

    @Test
    fun `clear button empties the text filter`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "text", TableFilterState(FilterConstraint.CONTAINS, listOf("abc")))

            onNodeWithContentDescription("Clear").performClick()
            waitForIdle()

            assertThat(state.filters["text"]).isNull()
            onNodeWithText("abc").assertDoesNotExist()
        }

    @Test
    fun `clear button empties the enum filter`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "enum", TableFilterState(FilterConstraint.EQUALS, listOf(Fruit.Pear)))

            onNodeWithContentDescription("Clear").performClick()
            waitForIdle()

            assertThat(state.filters["enum"]).isNull()
        }

    @Test
    fun `panel operator is hinted and kept while editing`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "number", TableFilterState(FilterConstraint.GTE, listOf(5)))

            onNodeWithText("≥", substring = true).assertIsDisplayed()
            onNodeWithText("5").performTextReplacement("7")
            waitForIdle()

            assertThat(state.filters["number"]).isEqualTo(TableFilterState(FilterConstraint.GTE, listOf(7)))
        }

    @Test
    fun `text operator is hinted by name`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "text", TableFilterState(FilterConstraint.STARTS_WITH, listOf("ab")))

            onNodeWithText("Starts with", substring = true).assertIsDisplayed()
        }

    @Test
    fun `date operator is hinted`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "date", TableFilterState(FilterConstraint.LT, listOf(LocalDate(2026, 1, 1))))

            onNodeWithText("<", substring = true).assertIsDisplayed()
        }

    @Test
    fun `between filter is shown read only and clears`() =
        runComposeUiTest {
            val state = showTable()
            setFilter(state, "number", TableFilterState(FilterConstraint.BETWEEN, listOf(1, 10)))

            onNodeWithText("Between 1 - 10").assertIsDisplayed()
            onNodeWithContentDescription("Clear").performClick()
            waitForIdle()

            assertThat(state.filters["number"]).isNull()
        }

    @Test
    fun `boolean checkbox names its state`() =
        runComposeUiTest {
            showTable()
            val any = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Any")
            val yes = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Yes")

            onNode(any).performClick()
            waitForIdle()

            onNode(yes).assertIsDisplayed()
        }
}
