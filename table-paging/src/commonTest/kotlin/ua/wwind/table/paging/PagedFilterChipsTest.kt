package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import ua.wwind.table.DefaultFilterChip
import ua.wwind.table.TableFilterChipDefaults
import ua.wwind.table.TableFilterChipNode
import ua.wwind.table.TableFilterChipPlacement
import ua.wwind.table.TableFilterChipStyle
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

/** Both paged entry points take the same [TableFilterChipStyle] as the core table (#160). */
@OptIn(ExperimentalTestApi::class)
class PagedFilterChipsTest {
    /** The mixed-column group chip drawn in the row; clicking its description opens its popover. */
    private var rowGroup: TableFilterChipNode.Group<String>? = null

    @Test
    fun `paged table draws chips with the given style`() =
        runComposeUiTest {
            setTable { items, state, style ->
                val columns = remember { columns<Unit>() }
                Table(items = items, state = state, columns = columns, filterChips = style)
            }
            assertStyleApplied()
        }

    @Test
    fun `paged table with table data draws chips with the given style`() =
        runComposeUiTest {
            setTable { items, state, style ->
                val columns = remember { columns<String>() }
                Table(items = items, state = state, columns = columns, tableData = "data", filterChips = style)
            }
            assertStyleApplied()
        }

    private fun ComposeUiTest.assertStyleApplied() {
        onNodeWithText("Chip A").assertIsDisplayed()
        onNodeWithContentDescription(rowGroup!!.description).performClick()
        waitForIdle()

        onNodeWithText("Chip B").assertIsDisplayed()
        onNodeWithText("Chip Region").assertIsDisplayed()
        onNodeWithText("Chip Old name").assertIsDisplayed()
    }

    private fun ComposeUiTest.setTable(
        table: @Composable (PagingData<String>, TableState<String>, TableFilterChipStyle<String>) -> Unit,
    ) {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b"),
                    settings = TableSettings(showActiveFiltersHeader = true, autoFilterDebounce = 0),
                )
            val items =
                remember {
                    PagingData(
                        data = PagingMap(size = 1, values = persistentMapOf(0 to "row"), onGet = {}),
                        loadState = LoadState.Success,
                        retry = {},
                    )
                }
            val style =
                TableFilterChipDefaults.style<String>(
                    unknownTitle = { key -> if (key == "legacy") "Region" else null },
                    unresolvedTitle = { _, payload -> if (payload == "p") "Old name" else null },
                    chip = { node ->
                        when (node) {
                            is TableFilterChipNode.Group -> {
                                if (node.placement == TableFilterChipPlacement.Row) rowGroup = node
                                DefaultFilterChip(node)
                            }

                            is TableFilterChipNode.Condition -> {
                                Text("Chip ${node.title}")
                            }

                            is TableFilterChipNode.Unknown -> {
                                Text("Chip ${node.title}")
                            }

                            is TableFilterChipNode.Unresolved -> {
                                Text("Chip ${node.title}")
                            }
                        }
                    },
                )
            Box(Modifier.size(1200.dp, 800.dp)) { table(items, state, style) }
        }
        waitForIdle()
        state.setFilterExpression(
            and(eq("a"), Group(Operator.OR, listOf(eq("b"), eq("legacy"), TableFilterNode.Unresolved("old", "p")))),
        )
        waitForIdle()
    }

    private fun <E> columns() =
        tableColumns<String, String, E> {
            listOf("a", "b").forEach { key ->
                column(key, valueOf = { it }) {
                    header(key.uppercase())
                    filter(TableFilterType.TextTableFilter())
                    cell { item, _ -> Text(item) }
                }
            }
        }

    private fun eq(column: String): Condition<String> =
        Condition(column, TableFilterState(FilterConstraint.EQUALS, listOf(column)))

    private fun and(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.AND, children.toList())
}
