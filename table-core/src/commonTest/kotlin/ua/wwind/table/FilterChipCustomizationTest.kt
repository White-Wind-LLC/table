package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FilterChipCustomizationTest {
    /** The last mixed-column group chip drawn in the row; clicking its description opens its popover. */
    private var rowGroup: TableFilterChipNode.Group<String>? = null

    private val placements = mutableSetOf<TableFilterChipPlacement>()

    private fun columns(configure: ReadonlyColumnBuilder<String, String, Unit>.(String) -> Unit) =
        tableColumns<String, String, Unit> {
            listOf("a", "b", "c").forEach { key ->
                column(key, valueOf = { it }) {
                    header(key.uppercase())
                    filter(TableFilterType.TextTableFilter())
                    cell { item, _ -> Text(item) }
                    configure(key)
                }
            }
        }

    @Test
    fun `column content draws in the row and in the popover`() =
        runComposeUiTest {
            val columns =
                columns { key ->
                    if (key == "a") {
                        filterChipContent { node ->
                            placements += node.placement
                            Text("Custom ${node.title}")
                        }
                    }
                }
            setTable(columns, and(eq("a"), or(eq("a", "x"), eq("b"))))

            onNodeWithText("Custom A").assertIsDisplayed()
            assertThat(placements).containsExactlyInAnyOrder(TableFilterChipPlacement.Row)
            openGroup()

            assertThat(onAllNodesWithText("Custom A").fetchSemanticsNodes().size).isEqualTo(2)
            onNodeWithText("B: Equals \"b\"").assertIsDisplayed()
            assertThat(placements)
                .containsExactlyInAnyOrder(TableFilterChipPlacement.Row, TableFilterChipPlacement.Tree)
        }

    @Test
    fun `interactive column content changes only its node`() =
        runComposeUiTest {
            val columns =
                columns { key ->
                    if (key == "b") {
                        filterChipContent { node ->
                            TextButton(onClick = { node.onChange(filterState("y")) }) { Text("Set ${node.title}") }
                        }
                    }
                }
            val state = setTable(columns, and(or(eq("a"), eq("b"), eq("c"))))
            openGroup()

            onNodeWithText("Set B").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(or(eq("a"), eq("b", "y"), eq("c"))))
        }

    @Test
    fun `full renderer draws every kind and can delegate`() =
        runComposeUiTest {
            setTable(
                columns {},
                and(eq("a"), or(eq("b"), eq("legacy")), eq("old")),
                chip = { node ->
                    if (node is TableFilterChipNode.Unknown) Text("U:${node.title}") else DefaultFilterChip(node)
                },
            )

            onNodeWithText("A: Equals \"a\"").assertIsDisplayed()
            onNodeWithText("U:old").assertIsDisplayed()
            openGroup()

            onNodeWithText("B: Equals \"b\"").assertIsDisplayed()
            onNodeWithText("U:legacy").assertIsDisplayed()
        }

    @Test
    fun `popover still navigates with a custom renderer`() =
        runComposeUiTest {
            setTable(
                columns {},
                and(or(eq("a"), eq("b"))),
                chip = { node ->
                    if (node is TableFilterChipNode.Condition) {
                        TextButton(onClick = node.onOpen) { Text("Go ${node.title}") }
                    } else {
                        DefaultFilterChip(node)
                    }
                },
            )
            openGroup()

            onNodeWithText("Go B").performClick()
            waitForIdle()

            onNodeWithText("Value").assertIsDisplayed()
            onNodeWithContentDescription("Back").performClick()
            waitForIdle()
            onNodeWithText("Value").assertDoesNotExist()
            onNodeWithText("Go A").assertIsDisplayed()
        }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated activeFilterChip still draws its column's chips`() =
        runComposeUiTest {
            val columns =
                columns { key ->
                    if (key == "a") activeFilterChip { ctx -> Text("Legacy ${ctx.title}") }
                }
            setTable(columns, and(eq("a"), or(eq("a", "x"), eq("b")), or(eq("a", "p"), eq("a", "q"))))

            assertThat(onAllNodesWithText("Legacy A").fetchSemanticsNodes().size).isEqualTo(1)
            // The merged same-column chip is a group and keeps the library's text.
            onNodeWithText("A: Equals \"p\" or Equals \"q\"", useUnmergedTree = true).assertIsDisplayed()
            openGroup()

            assertThat(onAllNodesWithText("Legacy A").fetchSemanticsNodes().size).isEqualTo(2)
            onNodeWithText("B: Equals \"b\"").assertIsDisplayed()
        }

    private fun ComposeUiTest.openGroup() {
        onNodeWithContentDescription(rowGroup!!.description).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.setTable(
        columns: ImmutableList<ColumnSpec<String, String, Unit>>,
        expression: Group<String>,
        chip: (@Composable (TableFilterChipNode<String>) -> Unit)? = null,
    ): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c"),
                    settings = TableSettings(showActiveFiltersHeader = true, autoFilterDebounce = 0),
                )
            Box(Modifier.size(1200.dp, 800.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state = state,
                    columns = columns,
                    filterChips =
                        TableFilterChipDefaults.style(
                            chip = { node ->
                                if (node is TableFilterChipNode.Group &&
                                    node.placement == TableFilterChipPlacement.Row &&
                                    node.sharedColumn == null
                                ) {
                                    rowGroup = node
                                }
                                if (chip != null) chip(node) else DefaultFilterChip(node)
                            },
                        ),
                )
            }
        }
        waitForIdle()
        state.setFilterExpression(expression)
        waitForIdle()
        return state
    }

    private fun filterState(value: String): TableFilterState<String> =
        TableFilterState(FilterConstraint.EQUALS, listOf(value))

    private fun eq(
        column: String,
        value: String = column,
    ): Condition<String> = Condition(column, filterState(value))

    private fun and(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.AND, children.toList())

    private fun or(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.OR, children.toList())
}
