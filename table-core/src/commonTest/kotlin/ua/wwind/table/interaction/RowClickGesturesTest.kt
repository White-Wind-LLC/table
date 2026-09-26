package ua.wwind.table.interaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.ColumnSpec
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
 * Row clicks on desktop with selection on: selection must not wait for the double-click window, and
 * the window must cover the usual OS double-click time (500 ms) even though skiko reports 300 ms.
 *
 * The test clock is driven by hand, so "immediately" means within a frame of the release. Mobile rows
 * have no double click by design, so the tests run on non-mobile platforms only.
 */
@OptIn(ExperimentalTestApi::class)
class RowClickGesturesTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("name", valueOf = { it }) {
                header("Name")
                cell { item, _ -> Text(item) }
            }
        }

    private fun ComposeUiTest.showTable(
        selectionMode: SelectionMode,
        opened: MutableList<String>,
        columns: ImmutableList<ColumnSpec<String, String, Unit>> = this@RowClickGesturesTest.columns,
    ): () -> TableState<String> {
        lateinit var state: TableState<String>
        setContent {
            state =
                rememberTableState(
                    columns = columns.map { it.key }.toPersistentList(),
                    settings = TableSettings(selectionMode = selectionMode),
                )
            Box(Modifier.size(400.dp, 400.dp)) {
                Table(
                    itemsCount = 3,
                    itemAt = { "row-$it" },
                    state = state,
                    columns = columns,
                    onRowClick = { opened += it },
                )
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        return { state }
    }

    private fun desktopOnlyTest(block: suspend ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            if (getPlatform().isNonMobile()) block()
        }

    private fun ComposeUiTest.clickRow(text: String) {
        onNodeWithText(text).performMouseInput { click() }
    }

    @Test
    fun `a single click selects the row without waiting for the double-click window`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val state = showTable(SelectionMode.Single, opened)

            clickRow("row-1")
            mainClock.advanceTimeByFrame()

            assertThat(state().selection.selectedIndex).isEqualTo(1)
            assertThat(state().selection.selectedCell?.rowIndex).isEqualTo(1)
            assertThat(opened).isEmpty()
        }

    @Test
    fun `two clicks 400 ms apart open the row`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val state = showTable(SelectionMode.Single, opened)

            clickRow("row-1")
            mainClock.advanceTimeBy(400)
            clickRow("row-1")
            mainClock.advanceTimeBy(1_000)

            assertThat(opened).isEqualTo(listOf("row-1"))
            assertThat(state().selection.selectedIndex).isEqualTo(1)
        }

    @Test
    fun `two clicks further apart than the window stay single clicks`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val state = showTable(SelectionMode.Multiple, opened)

            clickRow("row-1")
            mainClock.advanceTimeBy(700)
            clickRow("row-1")
            mainClock.advanceTimeBy(1_000)

            assertThat(opened).isEmpty()
            assertThat(state().selection.selectedIndex).isEqualTo(1)
        }

    @Test
    fun `a click on another row between two clicks is not a double click`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val state = showTable(SelectionMode.Single, opened)

            clickRow("row-1")
            mainClock.advanceTimeBy(150)
            clickRow("row-2")
            mainClock.advanceTimeBy(1_000)

            assertThat(opened).isEmpty()
            assertThat(state().selection.selectedIndex).isEqualTo(2)
        }

    @Test
    fun `without selection a single click opens the row at once`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            showTable(SelectionMode.None, opened)

            clickRow("row-1")
            mainClock.advanceTimeByFrame()

            assertThat(opened).isEqualTo(listOf("row-1"))
        }

    @Test
    fun `a double click handled by a clickable inside a cell does not open the row`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val descended = mutableListOf<String>()
            val columns =
                tableColumns<String, String, Unit> {
                    column("icon", valueOf = { it }) {
                        header("Icon")
                        // A resizable column also composes the cell off-screen to measure it.
                        resizable(false)
                        cell { item, _ ->
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .testTag("icon-$item")
                                    .combinedClickable(onClick = {}, onDoubleClick = { descended += item }),
                            )
                        }
                    }
                }
            showTable(SelectionMode.Single, opened, columns)

            onNodeWithTag("icon-row-1").performMouseInput { doubleClick() }
            mainClock.advanceTimeBy(1_000)

            assertThat(descended).isEqualTo(listOf("row-1"))
            assertThat(opened).isEmpty()
        }

    @Test
    fun `two clicks on a clickable inside a cell do not open the row`() =
        desktopOnlyTest {
            val opened = mutableListOf<String>()
            val columns =
                tableColumns<String, String, Unit> {
                    column("button", valueOf = { it }) {
                        header("Button")
                        // A resizable column also composes the cell off-screen to measure it.
                        resizable(false)
                        cell { item, _ ->
                            Box(Modifier.size(40.dp).testTag("button-$item").clickable {})
                        }
                    }
                }
            showTable(SelectionMode.Single, opened, columns)

            onNodeWithTag("button-row-1").performMouseInput { click() }
            mainClock.advanceTimeBy(150)
            onNodeWithTag("button-row-1").performMouseInput { click() }
            mainClock.advanceTimeBy(1_000)

            assertThat(opened).isEmpty()
        }
}
