package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/**
 * Composition-level cover for `ColumnSpec.visible` changing after the first render — the path that
 * `TableState.tableWidth` derives from, and the one the unit tests can only reach by assigning
 * `visibleColumns` by hand.
 *
 * These also stand in for a recomposition-loop check: `Table` assigns `state.visibleColumns` *during*
 * composition, and a write that kept invalidating its own readers would never let `waitForIdle`
 * return, failing here by timeout rather than by assertion.
 */
@OptIn(ExperimentalTestApi::class)
class ColumnVisibilityTest {
    private fun columnsWith(showSecond: Boolean) =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(100.dp, 100.dp)
                resizable(false)
                cell { item, _ -> Text(item) }
            }
            column("b", valueOf = { it }) {
                header("B")
                width(100.dp, 100.dp)
                resizable(false)
                visible(showSecond)
                cell { _, _ -> Text("cell-b") }
            }
        }

    @Test
    fun `table width grows when a column becomes visible after the first render`() =
        runComposeUiTest {
            // Starts hidden on purpose: the width freezes at whatever the first composition
            // computed, so a table that opens narrow is the case that breaks. Opening wide and
            // hiding freezes at the *wider* value, which still fits every cell and hides nothing.
            var showSecond by mutableStateOf(false)
            lateinit var state: TableState<String>

            setContent {
                val columns = remember(showSecond) { columnsWith(showSecond) }
                state = rememberTableState(columns = persistentListOf("a", "b"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            val divider = state.dimensions.dividerThickness
            assertThat(state.tableWidth).isEqualTo(100.dp + divider)

            showSecond = true
            waitForIdle()
            assertThat(state.tableWidth).isEqualTo(200.dp + divider * 2)

            showSecond = false
            waitForIdle()
            assertThat(state.tableWidth).isEqualTo(100.dp + divider)
        }

    @Test
    fun `a cell of a column revealed after the first render is on screen`() =
        runComposeUiTest {
            var showSecond by mutableStateOf(false)

            setContent {
                val columns = remember(showSecond) { columnsWith(showSecond) }
                val state = rememberTableState(columns = persistentListOf("a", "b"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            onNodeWithText("cell-b").assertDoesNotExist()

            // Asserts the *cell*, not the header: the header measures from its own width map and
            // rendered fine even while the bug was live. It was the row — measured with
            // `state.tableWidth` — that kept the revealed column outside its bounds and out of
            // horizontal scroll range.
            showSecond = true
            waitForIdle()
            onNodeWithText("cell-b").assertIsDisplayed()
        }

    @Test
    fun `a visibility flip settles instead of recomposing forever`() =
        runComposeUiTest {
            var showSecond by mutableStateOf(true)
            var rootCompositions = 0

            setContent {
                SideEffect { rootCompositions++ }
                val columns = remember(showSecond) { columnsWith(showSecond) }
                val state = rememberTableState(columns = persistentListOf("a", "b"))
                Box(Modifier.size(400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            val settled = rootCompositions

            showSecond = false
            waitForIdle()

            // Writing state during composition costs at most one extra pass; anything more means the
            // write is feeding its own readers.
            assertThat(rootCompositions - settled).isLessThan(3)
        }
}
