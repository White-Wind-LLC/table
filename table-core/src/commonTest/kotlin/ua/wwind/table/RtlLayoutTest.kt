package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/** The table under `LayoutDirection.Rtl` is the mirror image of the LTR one. */
@OptIn(ExperimentalTestApi::class)
class RtlLayoutTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(150.dp, 150.dp)
                cell { _, _ -> Text("pinned-cell") }
            }
            column("b", valueOf = { it }) {
                header("B")
                width(150.dp, 150.dp)
                cell { _, _ -> Text("cell-b") }
                groupHeader { value -> Text("group-$value") }
            }
            column("c", valueOf = { it }) {
                header("C")
                width(150.dp, 150.dp)
                cell { _, _ -> Text("cell-c") }
            }
        }

    @Composable
    private fun RtlTable(
        state: TableState<String>,
        horizontalState: ScrollState,
        itemsCount: Int = 1,
        direction: LayoutDirection = LayoutDirection.Rtl,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            Box(Modifier.size(250.dp, 300.dp)) {
                Table(
                    itemsCount = itemsCount,
                    itemAt = { "row-$it" },
                    state = state,
                    columns = columns,
                    horizontalState = horizontalState,
                )
            }
        }
    }

    @Test
    fun `a pinned start column holds still while the table scrolls in RTL`() =
        runComposeUiTest {
            lateinit var horizontalState: ScrollState
            setContent {
                horizontalState = rememberScrollState()
                val state =
                    rememberTableState(
                        columns = persistentListOf("a", "b", "c"),
                        settings = TableSettings(pinnedColumnsCount = 1),
                    )
                RtlTable(state, horizontalState)
            }
            waitForIdle()
            val before = onNodeWithText("pinned-cell").getBoundsInRoot()

            runOnIdle { horizontalState.dispatchRawDelta(100f) }
            waitForIdle()

            assertThat(horizontalState.value).isGreaterThan(0)
            assertThat(onNodeWithText("pinned-cell").getBoundsInRoot()).isEqualTo(before)
        }

    @Test
    fun `a group header stays in the viewport while the table scrolls in RTL`() =
        runComposeUiTest {
            lateinit var horizontalState: ScrollState
            setContent {
                horizontalState = rememberScrollState()
                val state = rememberTableState(columns = persistentListOf("a", "b", "c"))
                remember { state.groupBy("b") }
                RtlTable(state, horizontalState, itemsCount = 3)
            }
            waitForIdle()

            // The inline header of the first group and its sticky copy.
            fun headerBounds() =
                onAllNodesWithText("group-row-0", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .map { it.boundsInRoot }
            val before = headerBounds()

            runOnIdle { horizontalState.dispatchRawDelta(100f) }
            waitForIdle()

            assertThat(before).isNotEmpty()
            assertThat(headerBounds()).isEqualTo(before)
        }
}
