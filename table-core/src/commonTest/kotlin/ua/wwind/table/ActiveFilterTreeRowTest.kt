package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ActiveFilterTreeRowTest {
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
    private var lastUnknown: TableFilterChipNode.Unknown<String>? = null

    @Test
    fun `complex node and simple condition show two chips`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c")))

            val group = lastGroup!!
            assertThat(group.text).isEqualTo("A, B")
            assertThat(group.operator).isEqualTo(Operator.OR)
            assertThat(group.itemCount).isEqualTo(2)
            assertThat(group.description).isEqualTo("Any of: A Equals \"a\", or B Equals \"b\"")
            onNodeWithContentDescription(group.description).assertIsDisplayed()
            onNodeWithText("C: Equals \"c\"").assertIsDisplayed()
            assertThat(onAllNodesWithContentDescription("Clear filter").fetchSemanticsNodes().size).isEqualTo(1)
            assertThat(onAllNodesWithContentDescription("Remove group A, B").fetchSemanticsNodes().size).isEqualTo(1)
        }

    @Test
    fun `editing C in the row keeps the group`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c")))

            onNodeWithText("C: Equals \"c\"").performClick()
            waitForIdle()
            onNodeWithText("Value").performTextReplacement("z")
            waitUntil { (state.filterExpression.children[1] as Condition).state.values == listOf("z") }

            assertThat(state.filterExpression.children[0]).isEqualTo(or(eq("a", "a"), eq("b", "b")))
            assertThat(state.filterExpression.children.size).isEqualTo(2)
        }

    @Test
    fun `x on the group chip removes the whole node`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c")))

            onNodeWithContentDescription("Remove group A, B").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(eq("c", "c")))
        }

    @Test
    fun `same-column OR reads as one chip`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "x"), eq("a", "y"))))

            val group = lastGroup!!
            assertThat(group.sharedColumn).isEqualTo("a")
            assertThat(group.text).isEqualTo("A: Equals \"x\" or Equals \"y\"")
            assertThat(group.columnTitles).containsExactly("A")
        }

    @Test
    fun `unknown key shows a chip and x removes it`() =
        runComposeUiTest {
            val state = setTable(and(eq("legacy", "v")))

            val unknown = lastUnknown!!
            assertThat(unknown.title).isEqualTo("legacy")
            onNodeWithText("legacy").assertIsDisplayed()
            onNodeWithContentDescription("Unknown field").assertIsDisplayed()

            onNodeWithContentDescription("Remove filter legacy").performClick()
            waitForIdle()

            assertThat(state.filterExpression.children).isEmpty()
        }

    @Test
    fun `unknown key uses the app's title`() =
        runComposeUiTest {
            setTable(and(eq("legacy", "v")), unknownTitle = { if (it == "legacy") "Region" else null })

            onNodeWithText("Region").assertIsDisplayed()
            onNodeWithText("legacy").assertDoesNotExist()
        }

    @Test
    fun `two conditions on one column show two chips`() =
        runComposeUiTest {
            val state = setTable(and(eq("a", "x"), eq("a", "y")))

            onNodeWithText("A: Equals \"x\"").assertIsDisplayed()
            onNodeWithText("A: Equals \"y\"").performClick()
            waitForIdle()
            onNodeWithText("Value").performTextReplacement("z")
            waitUntil { (state.filterExpression.children[1] as Condition).state.values == listOf("z") }

            assertThat(state.filterExpression.children[0]).isEqualTo(eq("a", "x"))
            assertThat(onAllNodesWithText("A:", substring = true).fetchSemanticsNodes().size).isEqualTo(2)
        }

    @Test
    fun `clear all resets simple conditions and removes complex ones`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c"), eq("legacy", "v")))

            onNodeWithText("Clear all").performClick()
            waitForIdle()

            val c = state.filters["c"]
            assertThat(c).isNotNull()
            assertThat(c!!.isActive()).isFalse()
            assertThat(state.filterExpression.children.single()).isInstanceOf(Condition::class)
            onNodeWithText("Clear all").assertDoesNotExist()
        }

    private fun ComposeUiTest.setTable(
        expression: Group<String>,
        unknownTitle: ((String) -> String?)? = null,
    ): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c", "d"),
                    settings = TableSettings(showActiveFiltersHeader = true, autoFilterDebounce = 0),
                )
            Box(Modifier.size(1200.dp, 400.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state = state,
                    columns = columns,
                    filterChips =
                        TableFilterChipDefaults.style(
                            unknownTitle = unknownTitle,
                            chip = { node ->
                                when (node) {
                                    is TableFilterChipNode.Group -> lastGroup = node
                                    is TableFilterChipNode.Unknown -> lastUnknown = node
                                    is TableFilterChipNode.Condition -> Unit
                                }
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

    private fun eq(
        column: String,
        value: String,
    ): Condition<String> = Condition(column, TableFilterState(FilterConstraint.EQUALS, listOf(value)))

    private fun and(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.AND, children.toList())

    private fun or(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.OR, children.toList())
}
