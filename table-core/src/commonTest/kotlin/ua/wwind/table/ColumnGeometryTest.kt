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
import assertk.assertions.isCloseTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ColumnGeometryTest {
    private val keys = (0 until 8).map { "c$it" }

    private val columns =
        tableColumns<String, String, Unit> {
            keys.forEach { key ->
                column(key, valueOf = { it }) {
                    header(key)
                    width(30.dp, 100.dp)
                    cell { _, _ -> Text(key) }
                }
            }
        }

    @Test
    fun `scrolling a column into view stops at its real edge when dividers are hidden`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            lateinit var horizontalState: ScrollState
            setContent {
                state =
                    rememberTableState(
                        columns = keys.toImmutableList(),
                        settings = TableSettings(showVerticalDividers = false),
                    )
                horizontalState = rememberScrollState()
                Box(Modifier.size(300.dp, 400.dp)) {
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

            state.selection.selectCell(rowIndex = 0, column = "c5")
            waitForIdle()

            val sixColumnsPastTheViewport = with(density) { (600.dp - 300.dp).toPx() }
            assertThat(horizontalState.value.toFloat()).isCloseTo(sixColumnsPastTheViewport, 1f)
        }

    @Test
    fun `the last column still resizes when vertical dividers are hidden`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            lateinit var horizontalState: ScrollState
            setContent {
                state =
                    rememberTableState(
                        columns = keys.toImmutableList(),
                        settings = TableSettings(showVerticalDividers = false),
                    )
                horizontalState = rememberScrollState()
                Box(Modifier.size(300.dp, 400.dp)) {
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

            val boundaryPx = with(density) { (100.dp * 8).toPx() }
            val grabX = boundaryPx - horizontalState.value - with(density) { 2.dp.toPx() }
            val grabY = with(density) { (state.dimensions.headerHeight / 2).toPx() }

            onRoot().performMouseInput {
                moveTo(Offset(grabX, grabY))
                press()
                repeat(4) { moveBy(Offset(15f, 0f)) }
                release()
            }
            waitForIdle()

            val resized = state.columns.widths["c7"]
            assertThat(resized).isNotNull()
            assertThat(resized!!.value).isGreaterThan(100f)
        }

    @Test
    fun `a grab strip sits on the boundary when vertical dividers are hidden`() =
        runComposeUiTest {
            lateinit var state: TableState<String>
            setContent {
                state =
                    rememberTableState(
                        columns = keys.toImmutableList(),
                        settings = TableSettings(showVerticalDividers = false),
                    )
                Box(Modifier.size(900.dp, 400.dp)) {
                    Table(itemsCount = 1, itemAt = { "row" }, state = state, columns = columns)
                }
            }
            waitForIdle()

            val boundaryAfterSevenColumns = with(density) { (100.dp * 7).toPx() }
            val grabY = with(density) { (state.dimensions.headerHeight / 2).toPx() }

            onRoot().performMouseInput {
                moveTo(Offset(boundaryAfterSevenColumns - with(density) { 1.dp.toPx() }, grabY))
                press()
                repeat(4) { moveBy(Offset(15f, 0f)) }
                release()
            }
            waitForIdle()

            val resized = state.columns.widths["c6"]
            assertThat(resized).isNotNull()
            assertThat(resized!!.value).isGreaterThan(100f)
        }
}
