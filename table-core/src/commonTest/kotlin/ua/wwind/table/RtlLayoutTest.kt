package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableSettings
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isNonMobile
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

    private fun dragScrollTest(
        direction: LayoutDirection,
        stepPx: Float,
    ) = runComposeUiTest {
        lateinit var horizontalState: ScrollState
        setContent {
            horizontalState = rememberScrollState()
            val state =
                rememberTableState(
                    columns = persistentListOf("a", "b", "c"),
                    settings = TableSettings(enableDragToScroll = true),
                )
            RtlTable(state, horizontalState, itemsCount = 5, direction = direction)
        }
        waitForIdle()
        val y = with(density) { 150.dp.toPx() }
        val x = with(density) { 125.dp.toPx() }
        onRoot().performMouseInput {
            moveTo(Offset(x, y))
            press()
            repeat(5) { moveBy(Offset(stepPx, 0f)) }
            release()
        }
        waitForIdle()
        assertThat(horizontalState.value).isGreaterThan(0)
    }

    @Test
    fun `dragging left scrolls toward the end in LTR`() = dragScrollTest(LayoutDirection.Ltr, stepPx = -20f)

    @Test
    fun `dragging right scrolls toward the end in RTL`() = dragScrollTest(LayoutDirection.Rtl, stepPx = 20f)

    /** Drags column "a" by its header handle one column toward the end: [stepPx] is physical. */
    private fun reorderTest(
        direction: LayoutDirection,
        stepPx: Float,
        switchedFromLtr: Boolean = false,
    ) = runComposeUiTest {
        if (!getPlatform().isNonMobile()) return@runComposeUiTest
        lateinit var state: TableState<String>
        var shownDirection by mutableStateOf(if (switchedFromLtr) LayoutDirection.Ltr else direction)
        setContent {
            state = rememberTableState(columns = persistentListOf("a", "b", "c"))
            CompositionLocalProvider(LocalLayoutDirection provides shownDirection) {
                Box(Modifier.size(600.dp, 300.dp)) {
                    Table(itemsCount = 1, itemAt = { "row-$it" }, state = state, columns = columns)
                }
            }
        }
        waitForIdle()
        // The table was first shown in LTR, as when an app switches direction at runtime.
        shownDirection = direction
        waitForIdle()
        onRoot().performMouseInput { moveTo(onNodeWithText("A").fetchSemanticsNode().boundsInRoot.center) }
        waitForIdle()
        val handle = onNodeWithContentDescription("Drag column").fetchSemanticsNode().boundsInRoot.center
        onRoot().performMouseInput {
            moveTo(handle)
            press()
            repeat(10) { moveBy(Offset(stepPx, 0f)) }
            release()
        }
        waitForIdle()
        assertThat(state.columns.order.toList()).isEqualTo(listOf("b", "a", "c"))
    }

    @Test
    fun `dragging a column header right moves it toward the end in LTR`() =
        reorderTest(LayoutDirection.Ltr, stepPx = 20f)

    @Test
    fun `dragging a column header left moves it toward the end in RTL`() =
        reorderTest(LayoutDirection.Rtl, stepPx = -20f)

    @Test
    fun `dragging a column header follows a switch from LTR to RTL`() =
        reorderTest(LayoutDirection.Rtl, stepPx = -20f, switchedFromLtr = true)
}
