package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.component.header.resizeScrollPullback
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ColumnResizeTest {
    private val columns =
        tableColumns<String, String, Unit> {
            column("a", valueOf = { it }) {
                header("A")
                width(100.dp, 500.dp)
                cell { item, _ -> Text(item) }
            }
            column("b", valueOf = { it }) {
                header("B")
                width(100.dp, 500.dp)
                cell { _, _ -> Text("cell-b") }
            }
        }

    @Test
    fun `dragging the last boundary right widens the column and scrolls after it`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            lateinit var horizontalState: ScrollState

            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b"))
                horizontalState = rememberScrollState()
                Box(Modifier.size(600.dp, 400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                        horizontalState = horizontalState,
                    )
                }
            }

            waitForIdle()
            horizontalState.dispatchRawDelta(10_000f)
            waitForIdle()

            val scrollBeforeResize = horizontalState.value
            val divider = state.dimensions.dividerThickness
            val boundaryPx = with(density) { (state.tableWidth - divider).toPx() }
            val grabX = boundaryPx - scrollBeforeResize - with(density) { 2.dp.toPx() }
            val grabY = with(density) { (state.dimensions.headerHeight / 2).toPx() }

            onRoot().performMouseInput {
                moveTo(Offset(grabX, grabY))
                press()
                repeat(4) { moveBy(Offset(20f, 0f)) }
                release()
            }
            waitForIdle()

            val resized = state.columns.widths["b"]
            assertThat(resized).isNotNull()
            assertThat(resized!!.value).isGreaterThan(500f)
            assertThat(horizontalState.value).isGreaterThan(scrollBeforeResize)

            val boundaryAfter = with(density) { (state.tableWidth - divider).toPx() }
            assertThat(
                resizeScrollPullback(boundaryAfter, horizontalState.value, horizontalState.viewportSize),
            ).isEqualTo(0f)
        }

    @Test
    fun `a small drag left at the right edge narrows the last column`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            lateinit var horizontalState: ScrollState

            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b"))
                horizontalState = rememberScrollState()
                Box(Modifier.size(600.dp, 400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                        horizontalState = horizontalState,
                    )
                }
            }

            waitForIdle()
            horizontalState.dispatchRawDelta(10_000f)
            waitForIdle()

            val divider = state.dimensions.dividerThickness
            val boundaryPx = with(density) { (state.tableWidth - divider).toPx() }
            val grabX = boundaryPx - horizontalState.value - with(density) { 2.dp.toPx() }
            val grabY = with(density) { (state.dimensions.headerHeight / 2).toPx() }

            onRoot().performMouseInput {
                moveTo(Offset(grabX, grabY))
                press()
                repeat(4) { moveBy(Offset(-20f, 0f)) }
                release()
            }
            waitForIdle()

            val resized = state.columns.widths["b"]
            assertThat(resized).isNotNull()
            assertThat(resized!!.value).isLessThan(500f)
        }

    @Test
    fun `a second drag continues from the width the first one left`() =
        runComposeUiTest {
            lateinit var state: TableState<String>

            setContent {
                state = rememberTableState(columns = persistentListOf("a", "b"))
                Box(Modifier.size(1200.dp, 400.dp)) {
                    Table(
                        itemsCount = 1,
                        itemAt = { "row" },
                        state = state,
                        columns = columns,
                    )
                }
            }

            waitForIdle()
            val grabY = with(density) { (state.dimensions.headerHeight / 2).toPx() }

            fun dragBoundaryRight() {
                val boundaryPx = with(density) { (state.columns.widths["a"] ?: 500.dp).toPx() }
                onRoot().performMouseInput {
                    moveTo(Offset(boundaryPx - with(density) { 1.dp.toPx() }, grabY))
                    press()
                    repeat(4) { moveBy(Offset(10f, 0f)) }
                    release()
                }
                waitForIdle()
            }

            dragBoundaryRight()
            val afterFirst = state.columns.widths["a"]
            assertThat(afterFirst).isNotNull()
            assertThat(afterFirst!!.value).isGreaterThan(500f)

            dragBoundaryRight()
            assertThat(state.columns.widths["a"]!!.value).isGreaterThan(afterFirst.value)
        }
}
