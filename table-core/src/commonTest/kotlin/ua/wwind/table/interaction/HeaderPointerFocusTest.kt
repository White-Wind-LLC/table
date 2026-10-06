package ua.wwind.table.interaction

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.EditableTable
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/**
 * A pointer press on a header moves the header Tab stop there, except while a row is edited and
 * when the press starts on a column's resize or drag handle.
 */
@OptIn(ExperimentalTestApi::class)
class HeaderPointerFocusTest {
    private val columns =
        editableTableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                width(150.dp, 150.dp)
                sortable()
                headerClickToSort(false)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
                editCell { item, _, _ ->
                    val focus = remember { FocusRequester() }
                    BasicTextField(
                        value = item,
                        onValueChange = {},
                        modifier = Modifier.testTag("editor").focusRequester(focus),
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                }
            }
            column("copy", valueOf = { it }) {
                header("Copy")
                width(150.dp, 150.dp)
                cell { item, _ -> Text("$item copy") }
            }
        }

    /**
     * The table above a focusable "before" box, which starts focused. While [holdFocus] answers
     * true, focus cannot leave the box, so a focus request elsewhere does nothing.
     */
    private fun ComposeUiTest.showTable(holdFocus: () -> Boolean = { false }): () -> TableState<String> {
        lateinit var state: TableState<String>
        val before = FocusRequester()
        setContent {
            state =
                rememberTableState(
                    columns = columns.map { it.key }.toPersistentList(),
                    settings = TableSettings(selectionMode = SelectionMode.Single, editingEnabled = true),
                )
            Column {
                Box(Modifier.size(400.dp, 300.dp)) {
                    EditableTable(
                        itemsCount = 3,
                        itemAt = { "row-$it" },
                        state = state,
                        columns = columns,
                        tableData = Unit,
                    )
                }
                Box(
                    Modifier
                        .focusProperties { onExit = { if (holdFocus()) cancelFocusChange() } }
                        .focusGroup(),
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .testTag("before")
                            .focusRequester(before)
                            .focusable(),
                    )
                }
            }
        }
        runOnIdle { before.requestFocus() }
        waitForIdle()
        return { state }
    }

    private fun ComposeUiTest.startEditing(state: TableState<String>) {
        runOnIdle { state.editing.start("row-1", 1, "name") }
        waitForIdle()
        onNodeWithTag("editor").assertIsFocused()
    }

    /** Excludes the header's zero-size measuring copy of its content. */
    private val isLaidOut = SemanticsMatcher("has a size") { it.boundsInRoot.width > 0f }

    /** The filter button of the "name" column: its second role-button without a content description. */
    private fun ComposeUiTest.filterButton() =
        onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                !SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription) and
                isLaidOut,
        )[1]

    /** Vertical centre of the header row, in root pixels. */
    private fun ComposeUiTest.headerCenterY(): Float =
        onAllNodesWithText("Name")
            .onLast()
            .fetchSemanticsNode()
            .boundsInRoot.center.y

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    @Test
    fun `a press on a resize handle while a row is edited keeps focus in the editor`() =
        desktopOnlyTest {
            val state = showTable()
            startEditing(state())
            val y = headerCenterY()
            val x = with(density) { 150.dp.toPx() }
            onRoot().performMouseInput {
                moveTo(Offset(x, y))
                press()
                release()
            }
            waitForIdle()
            onNodeWithTag("editor").assertIsFocused()
            assertThat(state().isHeaderFocused).isFalse()
        }

    @Test
    fun `a filter button click while a row is edited keeps focus in the editor`() =
        desktopOnlyTest {
            val state = showTable()
            startEditing(state())
            filterButton().performMouseInput { click() }
            waitForIdle()
            assertThat(state().isHeaderFocused).isFalse()
            assertThat(state().editing.rowIndex).isEqualTo(1)
        }

    @Test
    fun `a press on a resize handle does not move header focus`() =
        desktopOnlyTest {
            val state = showTable()
            val y = headerCenterY()
            val x = with(density) { 150.dp.toPx() }
            onRoot().performMouseInput {
                moveTo(Offset(x, y))
                press()
                repeat(4) { moveBy(Offset(10f, 0f)) }
                release()
            }
            waitForIdle()
            // The drag resized the column, so the press landed on its resize handle.
            assertThat(state().columns.widths["name"]).isNotNull()
            assertThat(state().isHeaderFocused).isFalse()
            onNodeWithTag("before").assertIsFocused()
        }

    @Test
    fun `a press on the drag handle does not move header focus`() =
        desktopOnlyTest {
            val state = showTable()
            val copy =
                onAllNodesWithText("Copy")
                    .onLast()
                    .fetchSemanticsNode()
                    .boundsInRoot.center
            onRoot().performMouseInput { moveTo(copy) }
            waitForIdle()
            val handle = onNodeWithContentDescription("Drag column").fetchSemanticsNode().boundsInRoot.center
            onRoot().performMouseInput {
                moveTo(handle)
                press()
                release()
            }
            waitForIdle()
            assertThat(state().isHeaderFocused).isFalse()
            onNodeWithTag("before").assertIsFocused()
        }

    @Test
    fun `a filter button click when no row is edited still focuses the header`() =
        desktopOnlyTest {
            val state = showTable()
            filterButton().performMouseInput { click() }
            waitForIdle()
            assertThat(state().isHeaderFocused).isTrue()
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
        }
}
