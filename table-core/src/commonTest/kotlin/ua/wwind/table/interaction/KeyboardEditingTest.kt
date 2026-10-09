package ua.wwind.table.interaction

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.EditableTable
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.component.syncEditCellFocus
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.editableTableColumns
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/**
 * Keyboard bindings that start, commit and leave an edit, activate a row and toggle its selection.
 *
 * Hardware-keyboard concern, so the tests run on non-mobile platforms only.
 */
@OptIn(ExperimentalTestApi::class)
class KeyboardEditingTest {
    private val names = mutableStateMapOf<Int, String>()
    private var singleLineName = false

    private val columns =
        editableTableColumns<String, String, Unit> {
            column("id", valueOf = { it }) {
                header("Id")
                cell { item, _ -> Text(item) }
            }
            column("name", valueOf = { it }) {
                header("Name")
                cell { item, _ -> Text("$item name") }
                editCell { item, _, onComplete ->
                    val row = item.removePrefix("row-").toInt()
                    TableCellTextField(
                        value = names[row] ?: item,
                        onValueChange = { names[row] = it },
                        modifier = Modifier.testTag("name-editor"),
                        singleLine = singleLineName,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onComplete() }),
                    )
                }
            }
            column("flag", valueOf = { it }) {
                header("Flag")
                cell { item, _ -> Text("$item flag") }
                // A non-text editor: it takes focus but leaves Enter to the table.
                editCell(canEdit = { _, row -> row != 2 }) { _, _, _ ->
                    Box(
                        Modifier
                            .syncEditCellFocus()
                            .size(20.dp)
                            .focusable()
                            .testTag("flag-editor"),
                    )
                }
            }
        }

    private class Fixture(
        val state: () -> TableState<String>,
        val clicked: List<String>,
        val completeAttempts: () -> Int,
    ) {
        fun selectedCell(): Pair<Int, String>? = state().selection.selectedCell?.let { it.rowIndex to it.column }

        fun editing(): Pair<Int, String>? = state().editing.rowIndex?.let { row -> row to state().editing.column!! }
    }

    private fun ComposeUiTest.showTable(
        editingEnabled: Boolean = true,
        selectionMode: SelectionMode = SelectionMode.Single,
        allowComplete: () -> Boolean = { true },
    ): Fixture {
        lateinit var state: TableState<String>
        val clicked = mutableListOf<String>()
        var completeAttempts = 0
        val before = FocusRequester()
        setContent {
            state =
                rememberTableState(
                    columns = columns.map { it.key }.toPersistentList(),
                    settings = TableSettings(selectionMode = selectionMode, editingEnabled = editingEnabled),
                )
            Column {
                Box(Modifier.size(10.dp).focusRequester(before).focusable())
                Box(Modifier.size(500.dp, 300.dp)) {
                    EditableTable(
                        itemsCount = 4,
                        itemAt = { "row-$it" },
                        state = state,
                        columns = columns,
                        tableData = Unit,
                        onRowClick = { clicked += it },
                        onRowEditComplete = {
                            completeAttempts++
                            allowComplete()
                        },
                    )
                }
            }
        }
        runOnIdle { before.requestFocus() }
        waitForIdle()
        // The header, then the body.
        press(Key.Tab)
        press(Key.Tab)
        return Fixture({ state }, clicked, { completeAttempts })
    }

    private fun ComposeUiTest.selectCell(
        fixture: Fixture,
        row: Int,
        column: String,
    ) {
        runOnIdle {
            fixture.state().selection.selectCell(row, column)
            fixture.state().selection.focusRow(row)
        }
        waitForIdle()
    }

    private fun ComposeUiTest.press(key: Key) {
        onRoot().performKeyInput { pressKey(key) }
        waitForIdle()
    }

    private fun ComposeUiTest.pressWith(
        modifier: Key,
        key: Key,
    ) {
        onRoot().performKeyInput { withKeyDown(modifier) { pressKey(key) } }
        waitForIdle()
    }

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    @Test
    fun `enter starts editing the selected editable cell`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "name")

            press(Key.Enter)

            assertThat(fixture.editing()).isEqualTo(1 to "name")
            onNodeWithTag("name-editor").assertIsFocused()
        }

    @Test
    fun `f2 starts editing the selected editable cell`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 0, "flag")

            press(Key.F2)

            assertThat(fixture.editing()).isEqualTo(0 to "flag")
        }

    @Test
    fun `enter on a cell refused by canEdit opens the row instead`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 2, "flag")

            press(Key.Enter)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.clicked).containsExactly("row-2")
        }

    @Test
    fun `enter on a read-only column opens the row`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "id")

            press(Key.Enter)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.clicked).containsExactly("row-1")
        }

    @Test
    fun `enter opens the row when editing is disabled`() =
        desktopOnlyTest {
            val fixture = showTable(editingEnabled = false)
            selectCell(fixture, 3, "name")

            press(Key.Enter)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.clicked).containsExactly("row-3")
        }

    @Test
    fun `f2 on a cell that cannot be edited does nothing`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "id")

            press(Key.F2)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.clicked).isEmpty()
        }

    @Test
    fun `space toggles the focused row in single selection`() =
        desktopOnlyTest {
            val fixture = showTable(selectionMode = SelectionMode.Single)
            selectCell(fixture, 1, "id")
            assertThat(fixture.state().selection.selectedIndex).isEqualTo(1)

            press(Key.Spacebar)
            assertThat(fixture.state().selection.selectedIndex).isNull()

            press(Key.Spacebar)
            assertThat(fixture.state().selection.selectedIndex).isEqualTo(1)
        }

    @Test
    fun `space toggles the row checkmark in multiple selection`() =
        desktopOnlyTest {
            val fixture = showTable(selectionMode = SelectionMode.Multiple)
            selectCell(fixture, 2, "id")

            press(Key.Spacebar)
            assertThat(
                fixture
                    .state()
                    .selection.checkedIndices
                    .toList(),
            ).containsExactly(2)

            press(Key.Spacebar)
            assertThat(
                fixture
                    .state()
                    .selection.checkedIndices
                    .toList(),
            ).isEmpty()
        }

    @Test
    fun `space does nothing without a selection mode`() =
        desktopOnlyTest {
            val fixture = showTable(selectionMode = SelectionMode.None)
            selectCell(fixture, 2, "id")

            press(Key.Spacebar)

            assertThat(fixture.state().selection.selectedIndex).isNull()
            assertThat(
                fixture
                    .state()
                    .selection.checkedIndices
                    .toList(),
            ).isEmpty()
            assertThat(fixture.selectedCell()).isEqualTo(2 to "id")
        }

    @Test
    fun `shift tab moves the edit to the previous editable cell`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "flag")
            press(Key.F2)

            pressWith(Key.ShiftLeft, Key.Tab)

            assertThat(fixture.editing()).isEqualTo(1 to "name")
            assertThat(fixture.selectedCell()).isEqualTo(1 to "name")
            onNodeWithTag("name-editor").assertIsFocused()
        }

    @Test
    fun `shift tab on the first editable cell keeps the edit there`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "name")
            press(Key.F2)

            pressWith(Key.ShiftLeft, Key.Tab)

            assertThat(fixture.editing()).isEqualTo(1 to "name")
            assertThat(fixture.completeAttempts()).isEqualTo(0)
        }

    @Test
    fun `enter the editor leaves alone commits the row and moves down`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "flag")
            press(Key.F2)
            onNodeWithTag("flag-editor", useUnmergedTree = true).assertIsFocused()

            press(Key.Enter)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.completeAttempts()).isEqualTo(1)
            assertThat(fixture.selectedCell()).isEqualTo(2 to "flag")
            assertThat(fixture.clicked).isEmpty()
            // The table holds focus again, so navigation goes on.
            assertThat(fixture.state().isFocused).isTrue()
            press(Key.DirectionDown)
            assertThat(fixture.selectedCell()).isEqualTo(3 to "flag")
        }

    @Test
    fun `enter keeps the row in edit mode when completion is refused`() =
        desktopOnlyTest {
            val fixture = showTable(allowComplete = { false })
            selectCell(fixture, 1, "flag")
            press(Key.F2)

            press(Key.Enter)

            assertThat(fixture.completeAttempts()).isEqualTo(1)
            assertThat(fixture.editing()).isEqualTo(1 to "flag")
            assertThat(fixture.selectedCell()).isEqualTo(1 to "flag")
        }

    @Test
    fun `enter in a multi-line editor types a line break`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "name")
            press(Key.F2)

            press(Key.Enter)

            assertThat(fixture.editing()).isEqualTo(1 to "name")
            assertThat(fixture.completeAttempts()).isEqualTo(0)
            assertThat(names[1].orEmpty().contains('\n')).isTrue()
        }

    @Test
    fun `ctrl enter commits a multi-line editor and moves down`() =
        desktopOnlyTest {
            val fixture = showTable()
            selectCell(fixture, 1, "name")
            press(Key.F2)

            pressWith(Key.CtrlLeft, Key.Enter)

            assertThat(fixture.editing()).isNull()
            assertThat(fixture.selectedCell()).isEqualTo(2 to "name")
            assertThat(names[1].orEmpty().contains('\n')).isEqualTo(false)
            assertThat(fixture.state().isFocused).isTrue()
        }

    @Test
    fun `enter in a single-line editor with done still moves to the next editable cell`() =
        desktopOnlyTest {
            singleLineName = true
            val fixture = showTable()
            selectCell(fixture, 1, "name")
            press(Key.F2)

            press(Key.Enter)

            assertThat(fixture.editing()).isEqualTo(1 to "flag")
            assertThat(fixture.completeAttempts()).isEqualTo(0)
        }
}
