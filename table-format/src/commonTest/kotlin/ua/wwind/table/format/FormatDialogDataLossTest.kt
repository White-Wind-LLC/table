package ua.wwind.table.format

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

/** The format dialog must not throw away rule edits, a new rule order, or a rule, without the user asking. */
@OptIn(ExperimentalTestApi::class)
class FormatDialogDataLossTest {
    private enum class TestField { A, B }

    private fun rule(
        id: Long,
        field: TestField,
    ) = TableFormatRule<TestField, Unit>(id = id, columns = listOf(field), filter = Unit)

    @Test
    fun `deleting a rule waits for confirmation`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            setContent {
                Box(Modifier.size(600.dp)) {
                    FormatDialogContent(
                        rules = persistentListOf(rule(0, TestField.A)),
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
            onAllNodesWithText("A").onFirst().performClick()
            onNodeWithContentDescription("More actions").performClick()
            waitForIdle()
            onNodeWithText("Delete").performClick()
            waitForIdle()
            assertThat(changes).isEmpty()

            onNodeWithText("Delete this rule?").assertExists()
            onNodeWithText("Delete").performClick()
            waitForIdle()
            assertThat(changes.last()).isEmpty()
        }

    @Test
    fun `a dismiss request closes the dialog when nothing is being edited`() {
        var dismissed = false
        FormatDialogState<TestField, Unit>(LazyListState()).dismissUnlessEditing { dismissed = true }

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `a dismiss request is ignored while a rule is being edited`() {
        var dismissed = false
        val state = FormatDialogState<TestField, Unit>(LazyListState())
        state.editItem = EditFormatRule(0, rule(0, TestField.A), true)
        state.dismissUnlessEditing { dismissed = true }

        assertThat(dismissed).isFalse()
    }

    @Test
    fun `a new rule order is reported even when the list leaves right away`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            var show by mutableStateOf(true)
            setContent {
                if (show) {
                    Box(Modifier.size(600.dp)) {
                        FormatDialogContent(
                            rules = persistentListOf(rule(0, TestField.A), rule(1, TestField.B)),
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
            mainClock.autoAdvance = false
            onAllNodesWithContentDescription("Reorder").onFirst().performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, height * 0.2f)) }
                up()
            }
            mainClock.advanceTimeByFrame()
            show = false
            mainClock.autoAdvance = true
            waitForIdle()

            assertThat(changes.last().map { it.id }).isEqualTo(listOf(1L, 0L))
        }
}
