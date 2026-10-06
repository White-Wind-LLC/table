package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
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
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.ColumnSpec
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

    /** Six 150.dp columns in a 400.dp viewport: "c2" is cut by the right edge, "c3".."c5" are off-screen. */
    private val wideColumns =
        tableColumns<String, String, Unit> {
            (0..5).forEach { index ->
                column("c$index", valueOf = { it }) {
                    header("C$index")
                    width(150.dp, 150.dp)
                    cell { item, _ -> Text("$item $index") }
                }
            }
        }

    private fun ComposeUiTest.showTable(
        tableColumns: ImmutableList<ColumnSpec<String, String, Unit>> = columns,
        horizontalState: ScrollState = ScrollState(0),
        showColumnMenuButton: Boolean = false,
    ): () -> TableState<String> {
        lateinit var state: TableState<String>
        val before = FocusRequester()
        setContent {
            state =
                rememberTableState(
                    columns = tableColumns.map { it.key }.toPersistentList(),
                    settings =
                        TableSettings(
                            selectionMode = SelectionMode.Single,
                            showColumnMenuButton = showColumnMenuButton,
                        ),
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
                    Table(
                        itemsCount = 3,
                        itemAt = { "row-$it" },
                        state = state,
                        columns = tableColumns,
                        horizontalState = horizontalState,
                    )
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
    fun `tab visits the header then the body then leaves`() =
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
    fun `arrows home and end move the focused header`() =
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
            val state = showTable()
            press(Key.Tab)
            press(Key.Menu)
            onNodeWithText("Sort ascending").assertExists()
            press(Key.Escape)
            assertThat(state().isHeaderFocused).isTrue()

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

    /** Excludes the header's zero-size measuring copy of its content. */
    private val isLaidOut = SemanticsMatcher("has a size") { it.boundsInRoot.width > 0f }

    /** The header's role-button nodes without a content description: each column's sort, then filter button. */
    private fun ComposeUiTest.sortAndFilterButtons() =
        onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
                !SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription) and
                isLaidOut,
        )

    @Test
    fun `pointer clicks on the sort filter and menu buttons focus the header on their column`() =
        desktopOnlyTest {
            val state = showTable(showColumnMenuButton = true)
            sortAndFilterButtons().assertCountEquals(2)

            runOnIdle { state().focusedHeaderColumn = "copy" }
            sortAndFilterButtons()[0].performMouseInput { click() }
            waitForIdle()
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
            assertThat(state().isHeaderFocused).isTrue()

            runOnIdle { state().focusedHeaderColumn = "copy" }
            sortAndFilterButtons()[1].performMouseInput { click() }
            waitForIdle()
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
            onNodeWithText("Clear").performMouseInput { click() }
            waitForIdle()

            runOnIdle { state().focusedHeaderColumn = "copy" }
            onNodeWithContentDescription("Column options: Name").performMouseInput { click() }
            waitForIdle()
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
        }

    @Test
    fun `closing a filter panel opened from the keyboard menu returns focus to the header`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.F10, modifier = Key.ShiftLeft)
            onNodeWithText("Filter…").performMouseInput { click() }
            waitForIdle()

            onNodeWithText("Clear").performMouseInput { click() }
            waitForIdle()
            onNodeWithText("Clear").assertDoesNotExist()
            assertThat(state().isHeaderFocused).isTrue()
            assertThat(state().focusedHeaderColumn).isEqualTo("name")
        }

    @Test
    fun `an outside click closes a keyboard opened menu without reaching the table`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.F10, modifier = Key.ShiftLeft)
            onNodeWithText("Sort ascending").assertExists()

            // The popup layer takes the click over a body cell: it only dismisses the menu, and the
            // header, which kept focus under the popup, still has it.
            val cell = onNodeWithText("row-2 copy").fetchSemanticsNode().boundsInRoot.center
            onAllNodes(isRoot()).onLast().performMouseInput { click(cell) }
            waitForIdle()
            onNodeWithText("Sort ascending").assertDoesNotExist()
            assertThat(state().selection.selectedCell).isNull()
            assertThat(state().isFocused).isFalse()
            assertThat(state().isHeaderFocused).isTrue()
        }

    @Test
    fun `choosing an item of a keyboard opened menu returns focus to the header`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            press(Key.F10, modifier = Key.ShiftLeft)
            press(Key.Enter)

            assertThat(state().sort).isEqualTo(SortState("name", SortOrder.ASCENDING))
            onNodeWithText("Sort ascending").assertDoesNotExist()
            assertThat(state().isHeaderFocused).isTrue()
        }

    @Test
    fun `keyboard focus on the header shows the focus ring`() =
        desktopOnlyTest {
            val state = showTable()
            press(Key.Tab)
            assertThat(state().showsHeaderFocusRing("name")).isTrue()

            press(Key.DirectionRight)
            assertThat(state().showsHeaderFocusRing("name")).isFalse()
            assertThat(state().showsHeaderFocusRing("copy")).isTrue()
        }

    @Test
    fun `a click on a header focuses it without the focus ring until a key is pressed`() =
        desktopOnlyTest {
            val state = showTable()
            onAllNodesWithText("Copy").onLast().performMouseInput { click() }
            waitForIdle()
            assertThat(state().isHeaderFocused).isTrue()
            assertThat(state().focusedHeaderColumn).isEqualTo("copy")
            assertThat(state().showsHeaderFocusRing("copy")).isFalse()

            press(Key.DirectionLeft)
            assertThat(state().showsHeaderFocusRing("name")).isTrue()
        }

    @Test
    fun `left and right scroll an off-screen column into view`() =
        desktopOnlyTest {
            val scroll = ScrollState(0)
            val state = showTable(wideColumns, scroll)
            press(Key.Tab)
            repeat(5) { press(Key.DirectionRight) }
            assertThat(state().focusedHeaderColumn).isEqualTo("c5")
            assertThat(scroll.value).isGreaterThan(0)

            repeat(5) { press(Key.DirectionLeft) }
            assertThat(state().focusedHeaderColumn).isEqualTo("c0")
            assertThat(scroll.value).isEqualTo(0)
        }

    @Test
    fun `a click on a partly visible header does not scroll it into view`() =
        desktopOnlyTest {
            val scroll = ScrollState(0)
            val state = showTable(wideColumns, scroll)
            onAllNodesWithText("C2").onLast().performMouseInput { click() }
            waitForIdle()
            assertThat(state().isHeaderFocused).isTrue()
            assertThat(state().focusedHeaderColumn).isEqualTo("c2")
            assertThat(scroll.value).isEqualTo(0)
        }

    @Test
    fun `enter and down do nothing in the header while a row is edited`() =
        desktopOnlyTest {
            val state = showTable()
            runOnIdle { state().editing.start("row-1", 1, "copy") }
            waitForIdle()
            runOnIdle { state().focusHeaderFromBody("name") }
            waitForIdle()
            assertThat(state().isHeaderFocused).isTrue()

            press(Key.Enter)
            assertThat(state().sort).isNull()

            press(Key.DirectionDown)
            assertThat(state().isHeaderFocused).isTrue()
            val cell = state().selection.selectedCell
            assertThat(cell?.rowIndex to cell?.column).isEqualTo(1 to "copy")
        }
}
