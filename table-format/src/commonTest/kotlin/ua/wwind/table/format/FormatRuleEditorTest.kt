package ua.wwind.table.format

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

/** The rule list and editor: empty state, text buttons, Duplicate, and reordering without dragging. */
@OptIn(ExperimentalTestApi::class)
class FormatRuleEditorTest {
    private enum class TestField { A, B }

    private fun rule(
        id: Long,
        field: TestField,
    ) = TableFormatRule<TestField, Unit>(id = id, columns = listOf(field), filter = Unit)

    private fun ComposeUiTest.showDialog(
        rules: ImmutableList<TableFormatRule<TestField, Unit>>,
        changes: MutableList<ImmutableList<TableFormatRule<TestField, Unit>>>,
    ) {
        setContent {
            Box(Modifier.size(600.dp)) {
                FormatDialogContent(
                    rules = rules,
                    onRulesChange = { changes.add(it) },
                    getNewRule = { id -> TableFormatRule.new<TestField, Unit>(id, Unit) },
                    getTitle = { it.name },
                    filters = { _, _ -> emptyList() },
                    entries = TestField.entries.toImmutableList(),
                    key = 0,
                    strings = DefaultStrings,
                )
            }
        }
    }

    @Test
    fun `an empty list says how to start`() =
        runComposeUiTest {
            showDialog(persistentListOf(), mutableListOf())

            onNodeWithText("No rules yet. Add a rule to highlight rows or cells.").assertExists()
        }

    @Test
    fun `cancel leaves the editor without saving`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            showDialog(persistentListOf(rule(0, TestField.A)), changes)
            onAllNodesWithText("A").onFirst().performClick()
            waitForIdle()
            onNodeWithText("Cancel").performClick()
            waitForIdle()

            assertThat(changes).isEmpty()
            onNodeWithContentDescription("Add rule").assertExists()
        }

    @Test
    fun `duplicate opens the copy in the editor and keeps the original`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            showDialog(persistentListOf(rule(0, TestField.A)), changes)
            onAllNodesWithText("A").onFirst().performClick()
            waitForIdle()
            onNodeWithContentDescription("More actions").performClick()
            waitForIdle()
            onNodeWithText("Duplicate").performClick()
            waitForIdle()

            assertThat(changes).isEmpty()
            // The copy is a new rule: still in the editor, with nothing to delete yet.
            onNodeWithText("Save").assertExists()
            onAllNodesWithContentDescription("More actions").assertCountEquals(0)

            onNodeWithText("Save").performClick()
            waitForIdle()
            assertThat(changes.last().map { it.id }).isEqualTo(listOf(0L, 1L))
            assertThat(changes.last().map { it.columns }).isEqualTo(listOf(listOf(TestField.A), listOf(TestField.A)))
        }

    @Test
    fun `move down from the reorder handle swaps the rules`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            showDialog(persistentListOf(rule(0, TestField.A), rule(1, TestField.B)), changes)
            onAllNodesWithContentDescription("Reorder").onFirst().performClick()
            waitForIdle()
            onNodeWithText("Move up").assertIsNotEnabled()
            onNodeWithText("Move down").assertIsEnabled().performClick()
            waitForIdle()
            // Opening a rule disposes the list, which reports the order it holds.
            onAllNodesWithText("A").onFirst().performClick()
            waitForIdle()

            assertThat(changes.last().map { it.id }).isEqualTo(listOf(1L, 0L))
        }

    @Test
    fun `screen readers can move a rule up`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            showDialog(persistentListOf(rule(0, TestField.A), rule(1, TestField.B)), changes)
            val actions =
                onAllNodesWithText("B")
                    .onFirst()
                    .fetchSemanticsNode()
                    .config[SemanticsActions.CustomActions]
            assertThat(actions.map { it.label }).isEqualTo(listOf("Move up"))

            runOnIdle { actions.single().action() }
            waitForIdle()
            onAllNodesWithText("A").onFirst().performClick()
            waitForIdle()
            assertThat(changes.last().map { it.id }).isEqualTo(listOf(1L, 0L))
        }
}
