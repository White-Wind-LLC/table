package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ActiveFilterChipSlotTest {
    private var offlineClicks = 0

    private val columns =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
            column("b", valueOf = { it }) {
                header("B")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
                activeFilterChip { ctx ->
                    DefaultActiveFilterChip(
                        ctx,
                        onClick = { offlineClicks++ },
                        label = { Text("Offline ${ctx.label}") },
                    )
                }
            }
            column("c", valueOf = { it }) {
                header("C")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
                activeFilterChip { ctx ->
                    Row {
                        TextButton(onClick = ctx.onClear) { Text("Drop ${ctx.title}") }
                        TextButton(onClick = { ctx.onChange(TableFilterState(FilterConstraint.EQUALS, listOf("y"))) }) {
                            Text("Set ${ctx.title}")
                        }
                    }
                }
            }
            column("d", valueOf = { it }) {
                header("D")
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
                activeFilterChip { ctx -> DefaultActiveFilterChip(ctx, onClick = null) }
            }
        }

    @Test
    fun `an overridden chip gets its own label and click while other chips stay default`() =
        runComposeUiTest {
            val state = setTable("a", "b")

            onNodeWithText("A:", substring = true).assertIsDisplayed()
            onNodeWithText("Offline B:", substring = true).performClick()
            waitForIdle()

            assertThat(offlineClicks).isEqualTo(1)
            onNodeWithText("Condition").assertDoesNotExist()
            assertThat(state.filters["b"]?.isActive()).isEqualTo(true)

            onNodeWithText("A:", substring = true).performClick()
            waitForIdle()
            onNodeWithText("Condition").assertIsDisplayed()
        }

    @Test
    fun `onClear from an overridden chip resets its filter`() =
        runComposeUiTest {
            val state = setTable("a", "c")

            onNodeWithText("Drop C").performClick()
            waitForIdle()

            assertThat(state.filters["c"]?.isActive() == true).isFalse()
            assertThat(state.filters["a"]?.isActive()).isEqualTo(true)
        }

    @Test
    fun `onChange from an overridden chip replaces its filter`() =
        runComposeUiTest {
            val state = setTable("c")

            onNodeWithText("Set C").performClick()
            waitForIdle()

            assertThat(state.filters["c"]).isEqualTo(TableFilterState(FilterConstraint.EQUALS, listOf("y")))
        }

    @Test
    fun `a default chip without a click is disabled and its close icon still clears the filter`() =
        runComposeUiTest {
            val state = setTable("d")

            onNodeWithText("D:", substring = true).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            onNodeWithText("D:", substring = true).performClick()
            waitForIdle()
            onNodeWithText("Condition").assertDoesNotExist()

            onNodeWithContentDescription("Clear filter").performClick()
            waitForIdle()

            assertThat(state.filters["d"]?.isActive() == true).isFalse()
        }

    @Test
    fun `the standalone active filters row draws the column's chip`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b", "c", "d"))
                TableActiveFilters(columns = columns, state = state, tableData = Unit)
            }
            waitForIdle()
            state.setFilter("b", TableFilterState(FilterConstraint.CONTAINS, listOf("x")))
            waitForIdle()

            onNodeWithText("Offline B:", substring = true).performClick()
            waitForIdle()

            assertThat(offlineClicks).isEqualTo(1)
            onNodeWithContentDescription("Clear filter").performClick()
            waitForIdle()
            assertThat(state.filters["b"]?.isActive() == true).isFalse()
        }

    /** Shows the table with a `contains "x"` filter on each of [filtered]. */
    private fun ComposeUiTest.setTable(vararg filtered: String): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c", "d"),
                    settings = TableSettings(showActiveFiltersHeader = true),
                )
            Box(Modifier.size(1200.dp, 400.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state = state,
                    columns = columns,
                )
            }
        }
        waitForIdle()
        filtered.forEach {
            state.setFilter(it, TableFilterState(FilterConstraint.CONTAINS, listOf("x")))
        }
        waitForIdle()
        return state
    }
}
