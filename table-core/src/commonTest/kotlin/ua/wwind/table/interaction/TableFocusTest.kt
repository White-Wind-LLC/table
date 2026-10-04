package ua.wwind.table.interaction

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns
import kotlin.test.Test

/**
 * Keyboard focus: the table body is a single Tab stop, and the arrow keys move the selection inside it.
 *
 * Tab traversal is a hardware-keyboard concern, so the tests run on non-mobile platforms only.
 */
@OptIn(ExperimentalTestApi::class)
class TableFocusTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                cell { item, _ -> Text(item) }
            }
            column("copy", valueOf = { it }) {
                header("Copy")
                cell { item, _ -> Text("$item copy") }
            }
        }

    private class Fixture(
        val state: () -> TableState<String>,
        val tableHasFocus: () -> Boolean,
    ) {
        fun selectedCell(): Pair<Int, String>? = state().selection.selectedCell?.let { it.rowIndex to it.column }
    }

    private fun ComposeUiTest.showTableBetweenFocusables(): Fixture {
        lateinit var state: TableState<String>
        var tableHasFocus = false
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
                Box(Modifier.size(400.dp, 300.dp).onFocusChanged { tableHasFocus = it.hasFocus }) {
                    Table(
                        itemsCount = 3,
                        itemAt = { "row-$it" },
                        state = state,
                        columns = columns,
                    )
                }
                Box(Modifier.size(10.dp).testTag("after").focusable())
            }
        }
        runOnIdle { before.requestFocus() }
        waitForIdle()
        return Fixture({ state }, { tableHasFocus })
    }

    private fun ComposeUiTest.press(key: Key) {
        onRoot().performKeyInput { pressKey(key) }
        waitForIdle()
    }

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    @Test
    fun `tab enters the table once and the next tab leaves it`() =
        desktopOnlyTest {
            val fixture = showTableBetweenFocusables()

            press(Key.Tab)
            assertThat(fixture.tableHasFocus()).isTrue()

            press(Key.Tab)
            onNodeWithTag("after").assertIsFocused()
            assertThat(fixture.tableHasFocus()).isFalse()
        }

    @Test
    fun `the table reports focus only while it holds it`() =
        desktopOnlyTest {
            val fixture = showTableBetweenFocusables()
            assertThat(fixture.state().isFocused).isFalse()

            press(Key.Tab)
            assertThat(fixture.state().isFocused).isTrue()

            press(Key.Tab)
            assertThat(fixture.state().isFocused).isFalse()
        }

    @Test
    fun `shift tab from after the table enters it once`() =
        desktopOnlyTest {
            val fixture = showTableBetweenFocusables()
            press(Key.Tab)
            press(Key.Tab)
            onNodeWithTag("after").assertIsFocused()

            onRoot().performKeyInput {
                withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
            }
            waitForIdle()
            assertThat(fixture.tableHasFocus()).isTrue()

            onRoot().performKeyInput {
                withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }
            }
            waitForIdle()
            onNodeWithTag("before").assertIsFocused()
        }

    @Test
    fun `arrow keys move the selected cell while the table has focus`() =
        desktopOnlyTest {
            val fixture = showTableBetweenFocusables()
            press(Key.Tab)

            press(Key.DirectionDown)
            press(Key.DirectionRight)

            assertThat(fixture.selectedCell()).isEqualTo(1 to "copy")
        }

    @Test
    fun `a click on a cell selects it and focuses the table`() =
        desktopOnlyTest {
            val fixture = showTableBetweenFocusables()

            onNodeWithText("row-2 copy").performMouseInput { click() }
            waitForIdle()

            assertThat(fixture.tableHasFocus()).isTrue()
            assertThat(fixture.selectedCell()).isEqualTo(2 to "copy")

            press(Key.Tab)
            onNodeWithTag("after").assertIsFocused()
        }
}
