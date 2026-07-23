package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RowBlocksWithinBlockDragTest {
    @Test
    fun `within-block drag emits a within move`() =
        runDragUiTest {
            val withinMoves = mutableListOf<RowWithinBlockMove>()
            setContent { WithinBlockTable(onWithinMove = { withinMoves += it }, onBlockMove = {}) }
            waitForIdle()

            dragHandleDown("handle-0")
            waitForIdle()

            assertThat(withinMoves.size).isEqualTo(1)
            assertThat(withinMoves.single().movedKey).isEqualTo(0)
        }

    @Test
    fun `within-block drag still emits after a whole-block move`() =
        runDragUiTest {
            val withinMoves = mutableListOf<RowWithinBlockMove>()
            val blockMoves = mutableListOf<RowBlockMove>()
            setContent { WithinBlockTable(onWithinMove = { withinMoves += it }, onBlockMove = { blockMoves += it }) }
            waitForIdle()

            // Drag the whole block past the standalone tail: it lands last, so its base view offset
            // shifts from 0 to 3 while its members keep their internal order.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(blockMoves.size).isEqualTo(1)

            // A within-block drag must still reorder among the block's members after the block moved.
            dragHandleDown("handle-0")
            waitForIdle()

            assertThat(withinMoves.size).isEqualTo(1)
        }

    @Test
    fun `a list change during the within-block settle does not lose the move`() =
        runDragUiTest {
            val withinMoves = mutableListOf<RowWithinBlockMove>()
            val itemsState =
                mutableStateOf(
                    listOf(
                        BlockRow(0, "a"),
                        BlockRow(1, "a"),
                        BlockRow(2, "a"),
                        BlockRow(3, null),
                        BlockRow(4, null),
                        BlockRow(5, null),
                    ),
                )
            setContent {
                val items by itemsState
                WithinBlockTableHoisted(items = items, onWithinMove = { withinMoves += it })
            }
            waitForIdle()

            // Park the settle at its drop animation, then land an SSE-style frame before it resolves:
            // the frame reorders the standalone tail but keeps every row and key, exactly what the
            // queue stream does mid-gesture. The move must survive the reconcile.
            mainClock.autoAdvance = false
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(6) { moveBy(Offset(0f, 50f)) }
                up()
            }
            mainClock.advanceTimeByFrame()
            // The drop animation has not resolved yet, so the frame genuinely lands mid-settle.
            assertThat(withinMoves.size).isEqualTo(0)
            itemsState.value =
                listOf(
                    BlockRow(0, "a"),
                    BlockRow(1, "a"),
                    BlockRow(2, "a"),
                    BlockRow(5, null),
                    BlockRow(4, null),
                    BlockRow(3, null),
                )
            mainClock.advanceTimeBy(2_000)
            mainClock.autoAdvance = true
            waitForIdle()

            assertThat(withinMoves.size).isEqualTo(1)
        }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.dragHandleDown(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).performTouchInput {
        down(center)
        repeat(6) { moveBy(Offset(0f, 50f)) }
        up()
    }
}

@Composable
private fun WithinBlockTable(
    onWithinMove: (RowWithinBlockMove) -> Unit,
    onBlockMove: (RowBlockMove) -> Unit,
) {
    var items by remember {
        mutableStateOf(
            listOf(
                BlockRow(0, "a"),
                BlockRow(1, "a"),
                BlockRow(2, "a"),
                BlockRow(3, null),
                BlockRow(4, null),
                BlockRow(5, null),
            ),
        )
    }
    val columns = remember { blockColumns() }
    val state =
        rememberTableState(
            columns = persistentListOf("handle", "name"),
            settings = TableSettings(rowReorderEnabled = true, selectionMode = SelectionMode.Single),
            dimensions = TableDefaults.compactDimensions(),
        )
    val blocks =
        remember {
            RowBlocks<BlockRow>(
                blockOf = { it.block },
                onCommit = { move ->
                    onBlockMove(move)
                    items =
                        items.toMutableList().also {
                            it.applyRowBlockMove(move, keyOf = { r -> r.id }, blockOf = { r -> r.block })
                        }
                },
                onRowReorderWithinBlock = { move ->
                    onWithinMove(move)
                    items =
                        items.toMutableList().also {
                            it.applyRowReorderWithinBlock(move, keyOf = { r -> r.id }, blockOf = { r -> r.block })
                        }
                },
                blockHeader = blockDragHeader,
            )
        }
    Box(Modifier.size(400.dp, 640.dp)) {
        Table(
            itemsCount = items.size,
            itemAt = { items.getOrNull(it) },
            state = state,
            columns = columns,
            rowKey = stableRowKey,
            rowBlocks = blocks,
        )
    }
}

@Composable
private fun WithinBlockTableHoisted(
    items: List<BlockRow>,
    onWithinMove: (RowWithinBlockMove) -> Unit,
) {
    val columns = remember { blockColumns() }
    val state =
        rememberTableState(
            columns = persistentListOf("handle", "name"),
            settings = TableSettings(rowReorderEnabled = true, selectionMode = SelectionMode.Single),
            dimensions = TableDefaults.compactDimensions(),
        )
    val blocks =
        remember {
            RowBlocks<BlockRow>(
                blockOf = { it.block },
                onCommit = {},
                onRowReorderWithinBlock = { onWithinMove(it) },
                blockHeader = blockDragHeader,
            )
        }
    Box(Modifier.size(400.dp, 640.dp)) {
        Table(
            itemsCount = items.size,
            itemAt = { items.getOrNull(it) },
            state = state,
            columns = columns,
            rowKey = stableRowKey,
            rowBlocks = blocks,
        )
    }
}
