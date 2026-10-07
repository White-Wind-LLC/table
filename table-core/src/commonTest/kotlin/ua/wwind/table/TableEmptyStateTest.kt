package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private const val NO_DATA = "No data"
private const val NO_RESULTS = "No results match the current filters"
private const val CLEAR_FILTERS = "Clear filters"

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
}
