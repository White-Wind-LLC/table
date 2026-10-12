package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.state.rememberTableState
import kotlin.math.roundToInt
import kotlin.test.Test

private data class OffsetRow(
    val id: Int,
    val group: String,
)

/** Enough rows after the ones under test for the list to scroll row 0 off the top. */
private val fillerRows = List(10) { OffsetRow(100 + it, "z") }

/** Where the sticky header and the inline group headers sit, read from the `groupHeader` slot. */
private class HeaderProbe {
    var sticky: LayoutCoordinates? = null
    var stickyValue: Any? = null
    val inline = mutableListOf<Pair<Any?, LayoutCoordinates>>()

    val stickyTop: Int get() = sticky!!.positionInRoot().y.roundToInt()

    /** Top of the topmost inline header showing [value]. */
    fun inlineTop(value: Any?): Int =
        inline
            .filter { it.first == value && it.second.isAttached }
            .minOf { it.second.positionInRoot().y }
            .roundToInt()
}

private fun probedColumns(
    probe: HeaderProbe,
    valueOf: (OffsetRow) -> Any?,
): ImmutableList<ColumnSpec<OffsetRow, String, Unit>> =
    tableColumns {
        column("group", valueOf = valueOf) {
            header("Group")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item.group) }
            groupHeader { value ->
                // Lazy items provide a pinnable container; the overlay above the list does not.
                val isSticky = LocalPinnableContainer.current == null
                var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                DisposableEffect(coordinates, value) {
                    val entry = coordinates?.let { value to it }
                    if (isSticky) {
                        probe.sticky = coordinates
                        probe.stickyValue = value
                    } else if (entry != null) {
                        probe.inline += entry
                    }
                    onDispose { if (entry != null) probe.inline -= entry }
                }
                Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates = it }) { Text("group-$value") }
            }
        }
    }

@Composable
private fun ProbedTable(
    itemsCount: Int,
    itemAt: (Int) -> OffsetRow?,
    columns: ImmutableList<ColumnSpec<OffsetRow, String, Unit>>,
    listState: LazyListState,
    rowHeight: Dp = TableDefaults.standardDimensions().rowHeight,
) {
    val state =
        rememberTableState(
            columns = persistentListOf("group"),
            dimensions = TableDefaults.standardDimensions().copy(rowHeight = rowHeight),
        )
    remember(state) { state.groupBy("group") }
    Box(Modifier.size(400.dp, 320.dp)) {
        Table(
            itemsCount = itemsCount,
            itemAt = itemAt,
            state = state,
            columns = columns,
            verticalState = listState,
        )
    }
}

/**
 * The sticky header is pushed up by the next group's first row: its cell's bottom edge meets that
 * row's inline header. Each case changes something the push-up depends on without changing the loader,
 * the row count or the first row's height (#146).
 */
@OptIn(ExperimentalTestApi::class)
class GroupStickyOverlayOffsetTest {
    private fun ComposeUiTest.px(dp: Dp): Int = with(density) { dp.roundToPx() }

    /** Scrolls row 0 up until only half a row of it is left on screen. */
    private fun ComposeUiTest.scrollFirstRowToHalf(
        listState: LazyListState,
        rowHeight: Dp,
    ) {
        runOnIdle { listState.requestScrollToItem(0) }
        waitForIdle()
        val size =
            runOnIdle {
                listState.layoutInfo.visibleItemsInfo
                    .first()
                    .size
            }
        runOnIdle { listState.requestScrollToItem(0, size - px(rowHeight) / 2) }
        waitForIdle()
    }

    /** The top of the list viewport, where an unpushed sticky header sits. */
    private fun ComposeUiTest.viewportTop(
        probe: HeaderProbe,
        listState: LazyListState,
        firstValue: Any?,
    ): Int =
        runOnIdle {
            probe.inlineTop(firstValue) -
                listState.layoutInfo.visibleItemsInfo
                    .first()
                    .offset
        }

    /** Bottom of the sticky header cell; its divider below overlaps the next row. */
    private fun ComposeUiTest.stickyBottom(
        probe: HeaderProbe,
        rowHeight: Dp,
    ): Int = runOnIdle { probe.stickyTop + px(rowHeight) }

    @Test
    fun `a sort that makes the first row the last of its group pushes the header up`() =
        runComposeUiTest {
            val probe = HeaderProbe()
            val listState = LazyListState()
            val rows =
                mutableStateListOf(OffsetRow(0, "a"), OffsetRow(2, "a"), OffsetRow(1, "b"), *fillerRows.toTypedArray())
            val rowHeight = TableDefaults.standardDimensions().rowHeight
            setContent {
                val columns = remember { probedColumns(probe) { it.group } }
                val itemAt = remember { { index: Int -> rows[index] } }
                ProbedTable(rows.size, itemAt, columns, listState)
            }
            scrollFirstRowToHalf(listState, rowHeight)
            assertThat(runOnIdle { probe.stickyTop }).isEqualTo(viewportTop(probe, listState, "a"))

            runOnIdle { rows.sortBy { it.id } }
            waitForIdle()

            assertThat(stickyBottom(probe, rowHeight)).isEqualTo(runOnIdle { probe.inlineTop("b") })
        }

    @Test
    fun `a taller row height moves the push-up with it`() =
        runComposeUiTest {
            val probe = HeaderProbe()
            val listState = LazyListState()
            val rows = listOf(OffsetRow(0, "a"), OffsetRow(1, "b"), OffsetRow(2, "c")) + fillerRows
            var rowHeight by mutableStateOf(40.dp)
            setContent {
                val columns = remember { probedColumns(probe) { it.group } }
                val itemAt = remember { { index: Int -> rows[index] } }
                ProbedTable(rows.size, itemAt, columns, listState, rowHeight)
            }
            scrollFirstRowToHalf(listState, rowHeight)
            assertThat(stickyBottom(probe, rowHeight)).isEqualTo(runOnIdle { probe.inlineTop("b") })

            rowHeight = 72.dp
            scrollFirstRowToHalf(listState, rowHeight)

            assertThat(stickyBottom(probe, rowHeight)).isEqualTo(runOnIdle { probe.inlineTop("b") })
        }

    @Test
    fun `a finer valueOf turns the next row into a new group and pushes the header up`() =
        runComposeUiTest {
            val probe = HeaderProbe()
            val listState = LazyListState()
            val rows = listOf(OffsetRow(0, "a1"), OffsetRow(1, "a2"), OffsetRow(2, "b1")) + fillerRows
            val rowHeight = TableDefaults.standardDimensions().rowHeight
            var byDay by mutableStateOf(false)
            setContent {
                val columns =
                    remember(byDay) {
                        if (byDay) probedColumns(probe) { it.group } else probedColumns(probe) { it.group.take(1) }
                    }
                val itemAt = remember { { index: Int -> rows[index] } }
                ProbedTable(rows.size, itemAt, columns, listState)
            }
            scrollFirstRowToHalf(listState, rowHeight)
            assertThat(runOnIdle { probe.stickyTop }).isEqualTo(viewportTop(probe, listState, "a"))

            byDay = true
            waitForIdle()

            assertThat(runOnIdle { probe.stickyValue }).isEqualTo("a1")
            assertThat(stickyBottom(probe, rowHeight)).isEqualTo(runOnIdle { probe.inlineTop("a2") })
        }

    @Test
    fun `a next row that loads into the same group releases the push-up`() =
        runComposeUiTest {
            val probe = HeaderProbe()
            val listState = LazyListState()
            val rows = listOf(OffsetRow(0, "a"), OffsetRow(1, "a"), OffsetRow(2, "b")) + fillerRows
            val rowHeight = TableDefaults.standardDimensions().rowHeight
            var loaded by mutableStateOf(false)
            setContent {
                val columns = remember { probedColumns(probe) { it.group } }
                val itemAt = remember { { index: Int -> if (index == 1 && !loaded) null else rows[index] } }
                ProbedTable(rows.size, itemAt, columns, listState)
            }
            scrollFirstRowToHalf(listState, rowHeight)
            val top = viewportTop(probe, listState, "a")
            assertThat(runOnIdle { probe.stickyTop }).isEqualTo(top + px(rowHeight) / 2 - px(rowHeight))

            loaded = true
            waitForIdle()

            assertThat(runOnIdle { probe.stickyTop }).isEqualTo(top)
        }
}
