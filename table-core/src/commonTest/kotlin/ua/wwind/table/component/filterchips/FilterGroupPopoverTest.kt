package ua.wwind.table.component.filterchips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.DefaultFilterChip
import ua.wwind.table.Table
import ua.wwind.table.TableFilterChipDefaults
import ua.wwind.table.TableFilterChipNode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FilterGroupPopoverTest {
    private val columns =
        tableColumns<String, String, Unit> {
            listOf("a", "b", "c", "d").forEach { key ->
                column(key, valueOf = { it }) {
                    header(key.uppercase())
                    filter(TableFilterType.TextTableFilter())
                    cell { item, _ -> Text(item) }
                }
            }
        }

    private var lastGroup: TableFilterChipNode.Group<String>? = null

    @Test
    fun `popover shows the subtree and removes a leaf without flattening`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"), eq("c"))))
            openGroup()

            onNodeWithText(leaf("a")).assertIsDisplayed()
            onNodeWithText(leaf("b")).assertIsDisplayed()
            onNodeWithText(leaf("c")).assertIsDisplayed()
            onNodeWithContentDescription("Remove filter B").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(or(eq("a"), eq("c"))))
            onNodeWithText("Any").assertIsDisplayed()
        }

    @Test
    fun `removing down to one leaf dissolves and closes`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            onNodeWithContentDescription("Remove filter B").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(eq("a")))
            onNodeWithText("Any").assertDoesNotExist()
            onNodeWithText("A: Equals \"a\"").assertIsDisplayed()
        }

    @Test
    fun `popover closes when its group dissolves to a plain condition`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            onNodeWithContentDescription("Remove filter B").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(eq("a")))
            onNodeWithText("Any").assertDoesNotExist()
            onNodeWithText("All").assertDoesNotExist()
            // The plain chip at the group's place does not open its panel and takes the focus back.
            onNodeWithText("Value").assertDoesNotExist()
            onNodeWithText("A: Equals \"a\"").assertIsFocused()
        }

    @Test
    fun `switching Any to All emits the tree`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            onNodeWithText("All").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(and(eq("a"), eq("b"))))
            onNodeWithText(leaf("a")).assertIsDisplayed()
        }

    @Test
    fun `toggling NOT wraps and unwraps the group`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            onNodeWithText("NOT").performClick()
            waitForIdle()
            assertThat(state.filterExpression).isEqualTo(and(Not(or(eq("a"), eq("b")))))
            onNodeWithText("Any").assertIsDisplayed()

            onNodeWithText("NOT").performClick()
            waitForIdle()
            assertThat(state.filterExpression).isEqualTo(and(or(eq("a"), eq("b"))))
        }

    @Test
    fun `nested group line has its own switch and x`() =
        runComposeUiTest {
            val state = setTable(and(or(and(eq("a"), eq("b")), eq("c"), eq("d"))))
            openGroup()

            // Lines in visual order: the top OR line, then the inner AND line.
            onAllNodesWithText("Any")[1].performClick()
            waitForIdle()
            assertThat(state.filterExpression).isEqualTo(and(or(or(eq("a"), eq("b")), eq("c"), eq("d"))))

            onNodeWithContentDescription("Remove group A, B").performClick()
            waitForIdle()
            assertThat(state.filterExpression).isEqualTo(and(or(eq("c"), eq("d"))))
            onNodeWithText(leaf("c")).assertIsDisplayed()
            assertThat(onAllNodesWithText("Any").fetchSemanticsNodes().size).isEqualTo(1)
        }

    @Test
    fun `unknown leaf inside a group shows warning chip with x only`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("legacy"), eq("b"))))
            openGroup()

            onNodeWithText("legacy").assertIsDisplayed().assertIsNotEnabled()
            onAllNodesWithContentDescription("Unknown field").fetchSemanticsNodes().let {
                assertThat(it.size).isEqualTo(1)
            }
            onNodeWithContentDescription("Remove filter legacy").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(or(eq("a"), eq("b"))))
        }

    @Test
    fun `escape closes the popover`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            onNodeWithText("Any").requestFocus()
            onNodeWithText("Any").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            onNodeWithText("Any").assertDoesNotExist()
            assertThat(state.filterExpression).isEqualTo(and(or(eq("a"), eq("b"))))
            onNodeWithContentDescription(lastGroup!!.description).assertIsFocused()
        }

    @Test
    fun `app setting a condition at the open path closes the popover`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a"), eq("b"))))
            openGroup()

            state.setFilterExpression(and(eq("c")))
            waitForIdle()

            onNodeWithText("Any").assertDoesNotExist()
            onNodeWithText("Value").assertDoesNotExist()
            onNodeWithText("C: Equals \"c\"").assertIsDisplayed()
        }

    private fun ComposeUiTest.openGroup() {
        onNodeWithContentDescription(lastGroup!!.description).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.setTable(expression: Group<String>): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c", "d"),
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
                                if (node is TableFilterChipNode.Group) lastGroup = node
                                DefaultFilterChip(node)
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

    /** A leaf's chip label in the popover. */
    private fun leaf(column: String): String = "${column.uppercase()}: Equals \"$column\""

    private fun eq(column: String): Condition<String> =
        Condition(column, TableFilterState(FilterConstraint.EQUALS, listOf(column)))

    private fun and(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.AND, children.toList())

    private fun or(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.OR, children.toList())
}
