package ua.wwind.table.format

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.DefaultStrings
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FormatDialogContentTest {
    private enum class TestField { A, B }

    private fun emptyRules(): ImmutableList<TableFormatRule<TestField, Unit>> = persistentListOf()

    @Test
    fun `saving a new rule emits it through onRulesChange`() =
        runComposeUiTest {
            val changes = mutableListOf<ImmutableList<TableFormatRule<TestField, Unit>>>()
            setContent {
                FormatDialogContent(
                    rules = emptyRules(),
                    onRulesChange = { changes.add(it) },
                    getNewRule = { id -> TableFormatRule.new<TestField, Unit>(id, Unit) },
                    getTitle = { it.name },
                    filters = { _, _ -> emptyList() },
                    entries = TestField.entries.toImmutableList(),
                    key = 0,
                    strings = DefaultStrings,
                    onDismissRequest = {},
                )
            }
            onNodeWithContentDescription("Add").performClick()
            waitForIdle()
            onNodeWithContentDescription("Save").performClick()
            waitForIdle()

            assertThat(changes).hasSize(1)
            assertThat(changes.single().size).isEqualTo(1)
        }

    @Test
    fun `close affordance is hidden when onDismissRequest is null`() =
        runComposeUiTest {
            setContent {
                FormatDialogContent(
                    rules = emptyRules(),
                    onRulesChange = {},
                    getNewRule = { id -> TableFormatRule.new<TestField, Unit>(id, Unit) },
                    getTitle = { it.name },
                    filters = { _, _ -> emptyList() },
                    entries = TestField.entries.toImmutableList(),
                    key = 0,
                    strings = DefaultStrings,
                    onDismissRequest = null,
                )
            }
            waitForIdle()
            onAllNodesWithContentDescription("Add").assertCountEquals(1)
            onAllNodesWithContentDescription("Close").assertCountEquals(0)
        }
}
