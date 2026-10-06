package ua.wwind.table.interaction

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.Table
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

/** The header is the first Tab stop; it navigates columns and opens the column menu from the keyboard. */
@OptIn(ExperimentalTestApi::class)
class HeaderKeyboardFocusTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                sortable()
                headerClickToSort(false)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
            column("copy", valueOf = { it }) {
                header("Copy")
                cell { item, _ -> Text("$item copy") }
            }
        }

    private fun ComposeUiTest.showTable(): () -> TableState<String> {
        lateinit var state: TableState<String>
        val before = FocusRequester()
        setContent {
            state =
                rememberTableState(
                    columns = columns.map { it.key }.toPersistentList(),
                    settings = TableSettings(selectionMode = SelectionMode.Single),
                )
            Column {
                Box(
                    Modifier
                        .size(10.dp)
                        .testTag("before")
                        .focusRequester(before)
                        .focusable(),
                )
                Box(Modifier.size(400.dp, 300.dp)) {
                    Table(itemsCount = 3, itemAt = { "row-$it" }, state = state, columns = columns)
                }
                Box(Modifier.size(10.dp).testTag("after").focusable())
            }
        }
        runOnIdle { before.requestFocus() }
        waitForIdle()
        return { state }
    }

    /** Sends to the topmost root, so keys reach an open popup menu too. */
    private fun ComposeUiTest.press(
        key: Key,
        modifier: Key? = null,
    ) {
        onAllNodes(isRoot()).onLast().performKeyInput {
            if (modifier != null) withKeyDown(modifier) { pressKey(key) } else pressKey(key)
        }
        waitForIdle()
    }

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    @Test
    fun `tab visits the header, then the body, then leaves`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            assertThat(state().isHeaderFocused).isTrue()
            assertThat(state().isFocused).isFalse()

            press(Key.Tab)
            assertThat(state().isHeaderFocused).isFalse()
            assertThat(state().isFocused).isTrue()

            press(Key.Tab)
            onNodeWithTag("after").assertIsFocused()
        }

    @Test
    fun `sort and filter buttons are not tab stops`() =
        desktopOnlyTest {
            showTable()
            press(Key.Tab)
            press(Key.Tab)
            press(Key.Tab)
            onNodeWithTag("after").assertIsFocused()
        }

    @Test
    fun `arrows, home and end move the focused header`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
            press(Key.DirectionRight)
            assertThat(state().focusedHeaderColumn).isEqualTo("copy")
            press(Key.MoveHome)
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
            press(Key.MoveEnd)
            assertThat(state().focusedHeaderColumn).isEqualTo("copy")
        }

    @Test
    fun `enter sorts the focused column`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.Enter)
            assertThat(state().sort).isEqualTo(SortState("name", SortOrder.ASCENDING))
        }

    @Test
    fun `shift f10 opens the menu and escape returns focus to the header`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.F10, modifier = Key.ShiftLeft)
            onNodeWithText("Sort ascending").assertExists()

            press(Key.Escape)
            onNodeWithText("Sort ascending").assertDoesNotExist()
            assertThat(state().isHeaderFocused).isTrue()
        }

    @Test
    fun `the menu key and alt down open the menu`() =
        desktopOnlyTest {
            showTable()
            press(Key.Tab)
            press(Key.Menu)
            onNodeWithText("Sort ascending").assertExists()
            press(Key.Escape)

            press(Key.DirectionDown, modifier = Key.AltLeft)
            onNodeWithText("Sort ascending").assertExists()
        }

    @Test
    fun `down enters the body and up from the first row returns`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.DirectionDown)
            assertThat(state().isFocused).isTrue()

            press(Key.DirectionUp)
            assertThat(state().isHeaderFocused).isTrue()
        }

    @Test
    fun `down from the header selects row 0 of the focused column when a cell is selected`() =
        desktopOnlyTest {
            val state = showTable()
            onNodeWithText("row-2 copy").performMouseInput { click() }
            waitForIdle()

            press(Key.Tab, modifier = Key.ShiftLeft)
            assertThat(state().focusedHeaderColumn).isEqualTo("copy")
            press(Key.DirectionLeft)
            press(Key.DirectionDown)

            val cell = state().selection.selectedCell
            assertThat(cell?.rowIndex to cell?.column).isEqualTo(0 to "name")
        }

    @Test
    fun `shift f10 in the body opens the menu of the selected column`() =
        desktopOnlyTest {
            val state = showTable()
            onNodeWithText("row-1 copy").performMouseInput { click() }
            waitForIdle()

            press(Key.F10, modifier = Key.ShiftLeft)
            assertThat(state().columnMenuRequest?.column).isEqualTo("copy")
            onNodeWithText("Move left").assertExists()
        }

    @Test
    fun `shift f10 in the body falls back to the first column when the selected one is hidden`() =
        desktopOnlyTest {
            val state = showTable()
            onNodeWithText("row-1 copy").performMouseInput { click() }
            waitForIdle()
            runOnIdle { state().columns.hide("copy") }
            waitForIdle()
            assertThat(state().selection.selectedCell?.column).isEqualTo("copy")

            press(Key.F10, modifier = Key.ShiftLeft)
            assertThat(state().columnMenuRequest?.column).isEqualTo("name")
            onNodeWithText("Sort ascending").assertExists()
        }

    @Test
    fun `shift f10 in the body without a selection opens the first column menu`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.Tab)

            press(Key.F10, modifier = Key.ShiftLeft)
            assertThat(state().columnMenuRequest?.column).isEqualTo("name")
        }
}
