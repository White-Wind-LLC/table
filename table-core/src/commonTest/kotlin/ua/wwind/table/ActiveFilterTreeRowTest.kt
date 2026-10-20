package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import kotlin.test.Test

private const val HOUR_MS = 3_600_000L

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
            column("e", valueOf = { it }) {
                header("E")
                cell { item, _ -> Text(item) }
            }
        }

    private val badges = mutableListOf<String>()

    /** Default strings that record the operator badges the chips draw. */
    private val recordingStrings =
        object : StringProvider by DefaultStrings {
            @Composable
            override fun filterOperatorBadge(
                operator: Operator,
                count: Int,
            ): String = DefaultStrings.filterOperatorBadge(operator, count).also { badges += it }
        }

    private var lastGroup: TableFilterChipNode.Group<String>? = null
    private var lastUnknown: TableFilterChipNode.Unknown<String>? = null
    private val unresolvedNodes = mutableListOf<TableFilterChipNode.Unresolved<String>>()

    @Test
    fun `complex node and simple condition show two chips`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c")))

            val group = lastGroup!!
            assertThat(group.text).isEqualTo("A, B")
            assertThat(group.operator).isEqualTo(Operator.OR)
            assertThat(group.itemCount).isEqualTo(2)
            assertThat(group.description).isEqualTo("Any of: A equals \"a\", or B equals \"b\"")
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
    fun `clear in the row panel removes the key like the header`() =
        runComposeUiTest {
            val state = setTable(and(eq("a", "x")))

            onNodeWithText("A: Equals \"x\"").performClick()
            waitForIdle()
            onNodeWithText("Clear").performClick()
            waitForIdle()

            assertThat(state.filters.containsKey("a")).isFalse()
            assertThat(state.filterExpression.children).isEmpty()
            onNodeWithText("Condition").assertDoesNotExist()
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
    fun `x on a group chip moves the focus to the chip now at its place`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "a"), eq("b", "b")), eq("c", "c")))

            onNodeWithContentDescription("Remove group A, B").performClick()
            waitForIdle()

            onNodeWithText("C: Equals \"c\"").assertIsFocused()
        }

    @Test
    fun `x on the last chip moves the focus to the previous chip`() =
        runComposeUiTest {
            setTable(and(eq("c", "c"), or(eq("a", "a"), eq("b", "b"))))

            onNodeWithContentDescription("Remove group A, B").performClick()
            waitForIdle()

            onNodeWithText("C: Equals \"c\"").assertIsFocused()
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

    @Test
    fun `a condition on a column without a filter type shows its title and only resets`() =
        runComposeUiTest {
            val state = setTable(and(eq("e", "v")))

            // The header has an "E" too; the chip is the selected one.
            onNode(hasText("E") and isSelected()).assertIsDisplayed().assertIsNotEnabled()
            onNodeWithContentDescription("Clear filter").performClick()
            waitForIdle()

            assertThat(
                state.filterExpression.children
                    .single()
                    .isActive(),
            ).isFalse()
            onNode(hasText("E") and isSelected()).assertDoesNotExist()
        }

    @Test
    fun `a negated condition has no operator badge`() =
        runComposeUiTest {
            setTable(and(TableFilterNode.Not(eq("a", "a"))))

            val group = lastGroup!!
            assertThat(group.isGroup).isFalse()
            assertThat(group.negated).isTrue()
            assertThat(badges).isEmpty()
        }

    @Test
    fun `a group shows its operator badge`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "a"), eq("b", "b"))))

            assertThat(lastGroup!!.isGroup).isTrue()
            assertThat(badges.distinct()).containsExactly("Any 2")
        }

    @Test
    fun `a group kept open with nothing active reads as its columns`() =
        runComposeUiTest {
            val state = setTable(and(or(eq("a", "a"), empty("b"))))
            onNodeWithContentDescription(lastGroup!!.description).performClick()
            waitForIdle()
            onNodeWithText("A: Equals \"a\"").performClick()
            waitForIdle()

            onNodeWithText("Value").performTextReplacement("")
            waitUntil { !state.filterExpression.isActive() }
            waitForIdle()

            assertThat(lastGroup!!.description).isEqualTo("A, B")
            assertThat(lastGroup!!.text).isEqualTo("A, B")
            assertThat(lastGroup!!.columnTitles).containsExactly("A", "B")
        }

    @Test
    fun `clear all leaves the conditions it does not show`() =
        runComposeUiTest {
            val state = setTable(and(eq("c", "c"), empty("legacy"), empty("a"), eq("gone", "v")))

            onNodeWithText("Clear all").performClick()
            waitForIdle()

            val children = state.filterExpression.children.map { it as Condition }
            assertThat(children.map { it.column }).containsExactly("c", "legacy", "a")
            assertThat(children.none { it.state.isActive() }).isTrue()
            assertThat(children[1]).isEqualTo(empty("legacy"))
        }

    @Test
    fun `the app replacing the condition under an open panel closes it`() =
        runComposeUiTest {
            val state = setTable(and(eq("c", "c")))
            onNodeWithText("C: Equals \"c\"").performClick()
            waitForIdle()
            onNodeWithText("Value").assertIsDisplayed()

            state.setFilterExpression(and(eq("d", "d")))
            waitForIdle()

            onNodeWithText("Value").assertDoesNotExist()
            onNodeWithText("D: Equals \"d\"").assertIsDisplayed()
        }

    @Test
    fun `removing another chip closes the open panel`() =
        runComposeUiTest {
            val state = setTable(and(eq("c", "c"), eq("legacy", "v")))
            onNodeWithText("C: Equals \"c\"").performClick()
            waitForIdle()
            onNodeWithText("Value").assertIsDisplayed()

            onNodeWithContentDescription("Remove filter legacy").performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(eq("c", "c")))
            onNodeWithText("Value").assertDoesNotExist()
        }

    @Test
    fun `an edit waiting on the debounce lands when the row panel closes`() =
        runComposeUiTest {
            val state = setTable(and(eq("c", "c")), debounce = HOUR_MS)
            onNodeWithText("C: Equals \"c\"").performClick()
            waitForIdle()

            onNodeWithText("Value").performTextReplacement("z")
            waitForIdle()
            assertThat(state.filterExpression).isEqualTo(and(eq("c", "c")))
            tapOutside()

            assertThat(state.filterExpression).isEqualTo(and(eq("c", "z")))
            onNodeWithText("Value").assertDoesNotExist()
        }

    private fun ComposeUiTest.setTable(
        expression: Group<String>,
        unknownTitle: ((String) -> String?)? = null,
        debounce: Long = 0,
        unresolvedTitle: ((key: String, payload: Any?) -> String?)? = null,
    ): TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c", "d", "e"),
                    settings = TableSettings(showActiveFiltersHeader = true, autoFilterDebounce = debounce),
                )
            Box(Modifier.size(1200.dp, 400.dp)) {
                Table(
                    itemsCount = 1,
                    itemAt = { "row" },
                    state = state,
                    columns = columns,
                    strings = recordingStrings,
                    filterChips =
                        TableFilterChipDefaults.style(
                            unknownTitle = unknownTitle,
                            unresolvedTitle = unresolvedTitle,
                            chip = { node ->
                                when (node) {
                                    is TableFilterChipNode.Group -> lastGroup = node
                                    is TableFilterChipNode.Unknown -> lastUnknown = node
                                    is TableFilterChipNode.Unresolved -> unresolvedNodes += node
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

    private fun empty(column: String): Condition<String> = Condition(column, TableFilterState<String>(null, null))

    /** A click on the popup layer below the table, which a popover or panel takes as a tap outside. */
    private fun ComposeUiTest.tapOutside() {
        val outside =
            onAllNodes(isRoot())
                .onFirst()
                .fetchSemanticsNode()
                .boundsInRoot.bottomCenter - Offset(0f, 8f)
        onAllNodes(isRoot()).onLast().performMouseInput { click(outside) }
        waitForIdle()
    }

    private fun and(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.AND, children.toList())

    private fun or(vararg children: TableFilterNode<String>): Group<String> = Group(Operator.OR, children.toList())

    @Test
    fun `unresolved leaf at top level shows a warning chip with x only`() =
        runComposeUiTest {
            val state = setTable(and(TableFilterNode.Unresolved("old"), eq("c", "c")))

            onNodeWithText("old").assertIsDisplayed().assertIsNotEnabled()
            assertThat(onAllNodesWithContentDescription("Unknown field").fetchSemanticsNodes()).hasSize(1)
            onNodeWithContentDescription("Remove filter old").performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(eq("c", "c")))
        }

    @Test
    fun `group with an unresolved leaf names it and is marked`() =
        runComposeUiTest {
            setTable(and(or(eq("a", "a"), eq("b", "b"), TableFilterNode.Unresolved("old")), eq("c", "c")))

            val group = lastGroup!!
            assertThat(group.hasUnresolved).isTrue()
            assertThat(group.text).isEqualTo("A, B, old")
            assertThat(group.columnTitles).containsExactly("A", "B", "old")
            assertThat(group.description)
                .isEqualTo("Any of: A equals \"a\", or B equals \"b\", or Unknown field: old")
            onNodeWithText("C: Equals \"c\"").assertIsDisplayed()
        }

    @Test
    fun `top-level negated unresolved leaf is a NOT chip naming it`() =
        runComposeUiTest {
            val state = setTable(and(Not(TableFilterNode.Unresolved("old"))))

            val group = lastGroup!!
            assertThat(group.negated).isTrue()
            assertThat(group.hasUnresolved).isTrue()
            assertThat(group.text).isEqualTo("old")
            onNodeWithContentDescription("Remove group old").performClick()
            waitForIdle()

            assertThat(state.filterExpression.children).isEmpty()
        }

    @Test
    fun `x on one of two equal unresolved chips removes only that one`() =
        runComposeUiTest {
            val state = setTable(and(TableFilterNode.Unresolved("old"), TableFilterNode.Unresolved("old")))

            onAllNodesWithContentDescription("Remove filter old").onFirst().performClick()
            waitForIdle()

            assertThat(state.filterExpression).isEqualTo(and(TableFilterNode.Unresolved("old")))
        }

    @Test
    fun `unresolvedTitle titles the chip from key and payload`() =
        runComposeUiTest {
            setTable(
                and(TableFilterNode.Unresolved("x", "known"), TableFilterNode.Unresolved("y")),
                unresolvedTitle = { _, payload -> if (payload == "known") "Known name" else null },
            )

            onNodeWithText("Known name").assertIsDisplayed()
            onNodeWithText("y").assertIsDisplayed()
            onNodeWithText("x").assertDoesNotExist()
            assertThat(unresolvedNodes.map { it.key to it.title }).containsExactly("x" to "Known name", "y" to "y")
        }
}
