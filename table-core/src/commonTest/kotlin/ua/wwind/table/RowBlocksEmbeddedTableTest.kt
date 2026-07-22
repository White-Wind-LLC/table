package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

/**
 * Every test here drives the table with `embedded = true`, which lays the rows out in place instead
 * of a lazy list and runs a reorder engine of its own. The lazy path's counterparts live in
 * [RowBlocksTableTest].
 */
@OptIn(ExperimentalTestApi::class)
class RowBlocksEmbeddedTableTest {
    @Test
    fun `drop commits one move and remaps positional state`() =
        runDragUiTest {
            // The embedded engine reports the gesture's net result once, at drop — the commit and
            // its side effects must match the lazy path's exactly-one-event contract.
            val moves = mutableListOf<RowBlockMove>()
            val items =
                listOf(
                    BlockRow(0, "a"),
                    BlockRow(1, "a"),
                    BlockRow(2, null),
                    BlockRow(3, null),
                    BlockRow(4, null),
                )
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { blockColumns() }
                state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings =
                            TableSettings(
                                rowReorderEnabled = true,
                                selectionMode = SelectionMode.Single,
                            ),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<BlockRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
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
                        embedded = true,
                    )
                }
            }
            waitForIdle()
            runOnIdle {
                state.selection.toggleRow(3)
                state.selection.selectCell(3, "name")
            }
            waitForIdle()

            // Drag block "a" by its header handle from the top far past the end of the short list:
            // the overshoot makes the landing position deterministic (last), whatever the
            // intermediate geometry.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()

            assertThat(moves.size).isEqualTo(1)
            val move = moves.single()
            assertThat(move.blockId).isEqualTo("a")
            assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
            assertThat(move.afterKey).isEqualTo(4)
            assertThat(move.beforeKey).isNull()

            // id-3 sat at view position 3; the block moved from above it to the end, so it now
            // renders one unit higher.
            assertThat(state.selection.selectedIndex).isEqualTo(1)
            assertThat(state.selection.selectedCell?.rowIndex).isEqualTo(1)

            // The permuted order is held optimistically after the drop: block rows render last.
            val blockTop = onNodeWithText("name-0").getBoundsInRoot().top
            val standaloneTop = onNodeWithText("name-4").getBoundsInRoot().top
            assertThat(blockTop).isGreaterThan(standaloneTop)
        }

    @Test
    fun `upstream change mid-gesture cancels the drag and commits nothing`() =
        runDragUiTest {
            // Changing the rendered list mid-drag rebuilds the embedded engine, and the OLD
            // engine's disposal still delivers the dead gesture's settle — with unit indices
            // computed against the old geometry. Applying them to the new list would commit a
            // move the user never made (regression for the stale-settle phantom commit); the
            // documented policy is to cancel, and the next gesture must work unimpeded.
            val moves = mutableListOf<RowBlockMove>()
            var items by mutableStateOf(
                listOf(
                    BlockRow(0, "a"),
                    BlockRow(1, "a"),
                    BlockRow(2, null),
                    BlockRow(3, null),
                    BlockRow(4, null),
                ),
            )
            setContent {
                val columns = remember { blockColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<BlockRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
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
                        embedded = true,
                    )
                }
            }
            waitForIdle()

            // Drag block "a" by its header handle well past other units and hold it there — no
            // release yet.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
            }
            waitForIdle()

            // External update mid-gesture: a new standalone leader on top shifts every unit index.
            items = listOf(BlockRow(9, null)) + items
            waitForIdle()
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput { up() }
            waitForIdle()

            assertThat(moves).isEqualTo(emptyList<RowBlockMove>())

            // The dead gesture must not poison the next one.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(12) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()

            assertThat(moves.size).isEqualTo(1)
            val move = moves.single()
            assertThat(move.blockId).isEqualTo("a")
            assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
            assertThat(move.afterKey).isEqualTo(4)
            assertThat(move.beforeKey).isNull()
        }

    @Test
    fun `reorder without blocks still delivers per-row moves`() =
        runDragUiTest {
            // Regression guard for the pre-blocks embedded contract: no blocks means per-row
            // onRowMove semantics, one (from, to) pair per completed gesture.
            val moves = mutableListOf<Pair<Int, Int>>()
            val items =
                listOf(
                    BlockRow(0, null),
                    BlockRow(1, null),
                    BlockRow(2, null),
                    BlockRow(3, null),
                )
            setContent {
                val columns = remember { blockColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                        onRowMove = { from, to -> moves += from to to },
                        embedded = true,
                    )
                }
            }
            waitForIdle()
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(moves).isEqualTo(listOf(0 to 3))
        }

    @Test
    fun `same-count list change mid-gesture drops the stale settle`() =
        runDragUiTest {
            // itemsCount is unchanged and the itemAt lambda captures the state delegate, so both
            // keep one identity across the change: only the snapshot's CONTENT can tell the settle
            // guard the engine laid out a different list. Regression for the memoized stale
            // snapshot that compared the old list against itself and let a phantom move through.
            val moves = mutableListOf<Pair<Int, Int>>()
            var items by mutableStateOf(
                listOf(
                    BlockRow(0, null),
                    BlockRow(1, null),
                    BlockRow(2, null),
                    BlockRow(3, null),
                ),
            )
            setContent {
                val columns = remember { blockColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                        onRowMove = { from, to -> moves += from to to },
                        embedded = true,
                    )
                }
            }
            waitForIdle()

            // Drag row 0 well past other rows and hold it there — no release yet.
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
            }
            waitForIdle()

            // External update mid-gesture that keeps the size: replace a row the gesture never
            // touched, so the dragged handle survives while the rendered list changes.
            items = items.dropLast(1) + BlockRow(7, null)
            waitForIdle()
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput { up() }
            waitForIdle()

            assertThat(moves).isEqualTo(emptyList<Pair<Int, Int>>())

            // The dead gesture must not poison the next one.
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(moves).isEqualTo(listOf(0 to 3))
        }

    @Test
    fun `reorder without blocks renders cleanly after an applied in-place move`() =
        runDragUiTest {
            // The applied move changes neither itemsCount nor the itemAt identity, so only the
            // snapshot's content can rebuild the engine state; a stale snapshot would keep the
            // settled drag offsets forever and render every row doubly displaced — once by the
            // leftover offsets, once by the reordered data.
            var items by mutableStateOf(
                listOf(
                    BlockRow(0, null),
                    BlockRow(1, null),
                    BlockRow(2, null),
                    BlockRow(3, null),
                ),
            )
            setContent {
                val columns = remember { blockColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                        onRowMove = { from, to ->
                            items = items.toMutableList().also { it.add(to, it.removeAt(from)) }
                        },
                        embedded = true,
                    )
                }
            }
            waitForIdle()

            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()

            assertThat(items.map { it.id }).isEqualTo(listOf(1, 2, 3, 0))
            // The rendered vertical order follows the applied list, with no leftover offsets.
            val tops = items.map { onNodeWithText(it.name).getBoundsInRoot().top }
            assertThat(tops).isEqualTo(tops.sorted())
        }

    @Test
    fun `drag start completes or cancels an active cell edit`() =
        runDragUiTest {
            val cancelled = mutableListOf<Int>()
            val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null))
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { blockColumns() }
                state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings =
                            TableSettings(rowReorderEnabled = true, editingEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<BlockRow>(
                            blockOf = { it.block },
                            onCommit = {},
                            blockHeader = blockDragHeader,
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    EditableTable(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        tableData = Unit,
                        rowKey = stableRowKey,
                        rowBlocks = blocks,
                        embedded = true,
                        // Completion is blocked, so the drag must fall back to cancelling.
                        onRowEditComplete = { false },
                        onEditCancel = { cancelled += it },
                    )
                }
            }
            waitForIdle()
            runOnIdle { state.editing.start(items[2], 2, "name") }
            waitForIdle()
            assertThat(state.editing.rowIndex).isEqualTo(2)

            // Whole-block drag starts from the header handle; its start hook cancels the edit.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                moveBy(Offset(0f, 60f))
                up()
            }
            waitForIdle()

            assertThat(state.editing.rowIndex).isNull()
            assertThat(cancelled).isEqualTo(listOf(2))
        }

    @Test
    fun `band header renders with non-zero width`() =
        runComposeUiTest {
            // The embedded path attaches no horizontal scroll node, so viewportSize reports 0;
            // the band must fall back to the table width (regression for the 0-px band defect).
            val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null))
            setContent {
                val columns = remember { blockColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks =
                    remember {
                        RowBlocks<BlockRow>(
                            blockOf = { it.block },
                            blockHeader = { blockId, _ -> Text("band-$blockId") },
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
                        embedded = true,
                    )
                }
            }
            waitForIdle()
            val band = onNodeWithText("band-a").getBoundsInRoot()
            assertThat(band.width).isGreaterThan(0.dp)
        }
}
