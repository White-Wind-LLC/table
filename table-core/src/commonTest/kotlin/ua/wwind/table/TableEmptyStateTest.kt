package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private const val NO_DATA = "No data"
private const val NO_RESULTS = "No results match the current filters"
private const val CLEAR_FILTERS = "Clear filters"
private const val UNRESOLVED = "Filter contains unknown fields — remove them to apply"

/** The body's empty state: what an empty table says, and that it keeps the header and footer. */
@OptIn(ExperimentalTestApi::class)
class TableEmptyStateTest {
    private fun columns() =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                width(120.dp, 120.dp)
                resizable(false)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
                footer { Text("total") }
            }
        }

    private fun TableState<String>.filterByName() =
        setFilter("name", TableFilterState(FilterConstraint.CONTAINS, listOf("zzz")))

    @Test
    fun `an empty unfiltered table says there is no data`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns)
                }
            }
            onNodeWithText(NO_DATA).assertIsDisplayed()
            onNodeWithText(NO_RESULTS).assertDoesNotExist()
        }

    @Test
    fun `an empty filtered table offers to clear the filters`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { columns() }
                state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns)
                }
            }
            runOnIdle { state.filterByName() }
            onNodeWithText(NO_RESULTS).assertIsDisplayed()
            onNodeWithText(NO_DATA).assertDoesNotExist()

            onNodeWithText(CLEAR_FILTERS).performClick()
            waitForIdle()
            assertThat(state.filters).isEmpty()
            onNodeWithText(NO_DATA).assertIsDisplayed()
        }

    private fun complexTree() =
        TableFilterNode.Group(
            TableFilterNode.Operator.AND,
            listOf(
                TableFilterNode.Group(
                    TableFilterNode.Operator.OR,
                    listOf(
                        TableFilterNode.Condition("name", TableFilterState(FilterConstraint.CONTAINS, listOf("x"))),
                        TableFilterNode.Condition("name", TableFilterState(FilterConstraint.CONTAINS, listOf("y"))),
                    ),
                ),
            ),
        )

    /** An empty table whose filter tree is [expression]. */
    private fun ComposeUiTest.setEmptyTable(expression: TableFilterNode.Group<String>): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            val columns = remember { columns() }
            state = rememberTableState(columns = persistentListOf("name"))
            Box(Modifier.size(400.dp)) {
                Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns)
            }
        }
        runOnIdle { state.setFilterExpression(expression) }
        return state
    }

    @Test
    fun `only a complex node counts as filtered`() =
        runComposeUiTest {
            setEmptyTable(complexTree())
            onNodeWithText(NO_RESULTS).assertIsDisplayed()
            onNodeWithText(NO_DATA).assertDoesNotExist()
        }

    @Test
    fun `clear filters removes complex nodes`() =
        runComposeUiTest {
            val state = setEmptyTable(complexTree())
            onNodeWithText(CLEAR_FILTERS).performClick()
            waitForIdle()
            assertThat(state.filterExpression.children).isEmpty()
            onNodeWithText(NO_DATA).assertIsDisplayed()
        }

    @Test
    fun `clear filters removes simple and complex filters together`() =
        runComposeUiTest {
            val nameFilter =
                TableFilterNode.Condition("name", TableFilterState(FilterConstraint.CONTAINS, listOf("zzz")))
            val state = setEmptyTable(complexTree().let { it.copy(children = listOf(nameFilter) + it.children) })
            onNodeWithText(CLEAR_FILTERS).performClick()
            waitForIdle()
            assertThat(state.filters).isEmpty()
            assertThat(state.filterExpression.children).isEmpty()
            onNodeWithText(NO_DATA).assertIsDisplayed()
        }

    @Test
    fun `the header and footer stay visible in the empty state`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("name"),
                        settings = TableSettings(showFooter = true, footerPinned = false),
                    )
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns)
                }
            }
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText(NO_DATA).assertIsDisplayed()
            onNodeWithText("total").assertIsDisplayed()
        }

    @Test
    fun `a table with rows shows no empty state`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = columns)
                }
            }
            onNodeWithText("row").assertIsDisplayed()
            onNodeWithText(NO_DATA).assertDoesNotExist()
        }

    @Test
    fun `a custom slot replaces the default content and sees the filter state`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { columns() }
                state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 0,
                        itemAt = { null },
                        state = state,
                        columns = columns,
                        emptyContent = { Text(if (isFiltered) "custom-filtered" else "custom-empty") },
                    )
                }
            }
            onNodeWithText("custom-empty").assertIsDisplayed()
            onNodeWithText(NO_DATA).assertDoesNotExist()
            runOnIdle { state.filterByName() }
            onNodeWithText("custom-filtered").assertIsDisplayed()
        }

    @Test
    fun `an empty slot restores the blank body`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 0,
                        itemAt = { null },
                        state = state,
                        columns = columns,
                        emptyContent = {},
                    )
                }
            }
            onNodeWithText("Name").assertIsDisplayed()
            onNodeWithText(NO_DATA).assertDoesNotExist()
        }

    @Test
    fun `an empty embedded table says there is no data`() =
        runComposeUiTest {
            setContent {
                val columns = remember { columns() }
                val state = rememberTableState(columns = persistentListOf("name"))
                Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns, embedded = true)
            }
            onNodeWithText(NO_DATA).assertIsDisplayed()
        }

    @Test
    fun `the empty state is centred in the visible body of a narrow table`() =
        assertEmptyStateCentred(columnWidth = 120.dp)

    @Test
    fun `the empty state is centred in the visible body of a table wider than the viewport`() =
        assertEmptyStateCentred(columnWidth = 900.dp)

    /** The body is everything below the header; the slot's centre must sit at the body's centre. */
    private fun assertEmptyStateCentred(columnWidth: Dp) =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns =
                    remember {
                        tableColumns<String, String, Unit> {
                            column("name", valueOf = { it }) {
                                header("Name")
                                width(columnWidth, columnWidth)
                                resizable(false)
                                cell { item, _ -> Text(item) }
                            }
                        }
                    }
                state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 0,
                        itemAt = { null },
                        state = state,
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        emptyContent = { Text("slot") },
                    )
                }
            }
            val bounds = onNodeWithText("slot").getBoundsInRoot()
            val headerBottom = state.dimensions.headerHeight
            val centreX = (bounds.left + bounds.right) / 2
            val centreY = (bounds.top + bounds.bottom) / 2
            assertThat(centreX).isBetween(196.dp, 204.dp)
            assertThat(centreY).isBetween((headerBottom + 400.dp) / 2 - 6.dp, (headerBottom + 400.dp) / 2 + 6.dp)
        }

    @Test
    fun `an unresolved filter says so and clearFilters removes it`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { columns() }
                state = rememberTableState(columns = persistentListOf("name"))
                Box(Modifier.size(400.dp)) {
                    Table(itemsCount = 0, itemAt = { null }, state = state, columns = columns)
                }
            }
            runOnIdle { state.setFilterExpression(TableFilterNode.Unresolved("old")) }
            onNodeWithText(UNRESOLVED).assertIsDisplayed()
            onNodeWithText(NO_RESULTS).assertDoesNotExist()
            onNodeWithText(CLEAR_FILTERS).performClick()
            waitForIdle()

            assertThat(state.filterExpression.children).isEmpty()
            onNodeWithText(NO_DATA).assertIsDisplayed()
        }
}
