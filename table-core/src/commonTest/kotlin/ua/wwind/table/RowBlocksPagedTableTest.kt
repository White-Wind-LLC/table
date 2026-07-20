package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

private data class PagedRow(val id: Int, val block: String?) {
    val name: String get() = "name-$id"
}

private fun pagedColumns() =
    tableColumns<PagedRow, String, Unit> {
        column("handle", valueOf = { it.id }) {
            header("H")
            width(48.dp, 48.dp)
            resizable(false)
            cell { item, _ ->
                Box(
                    Modifier
                        .size(24.dp)
                        .testTag("handle-${item.id}")
                        .draggableHandle(),
                )
            }
        }
        column("name", valueOf = { it.name }) {
            header("Name")
            width(120.dp, 120.dp)
            resizable(false)
            cell { item, _ -> Text(item.name) }
        }
    }

/** Block header carrying the whole-block drag handle tagged `block-handle-<blockId>`. */
private val pagedBlockDragHeader: @Composable context(RowBlockHeaderScope) (blockId: Any, rows: IntRange) -> Unit =
    { blockId, _ ->
        Box(
            Modifier
                .size(24.dp)
                .testTag("block-handle-$blockId")
                .draggableHandle(),
        ) {
            Text("band-$blockId")
        }
    }

private val pagedRowKey: (PagedRow?, Int) -> Any = { item, index -> item?.id ?: "ph-$index" }

/**
 * The paged surface of row blocks, driven through a fake paged source: a fixed [itemsCount] whose
 * `itemAt` answers null until the "page" holding an index loads. This is exactly what the paging
 * adapter hands the core table, so the policies proven here — bands over loaded runs, the paged
 * drop policy, gestures surviving a mid-drag page load — are the adapter's behavior.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalTableApi::class)
class RowBlocksPagedTableTest {
    @Test
    fun `bands extend as pages load`() =
        runComposeUiTest {
            // Rows 0..3 are one block, but only the first page is in: the band must cover the
            // loaded fragment and grow when the rest of the block arrives — never claim unloaded
            // rows whose membership is not yet observable.
            var loaded by mutableStateOf(mapOf(0 to PagedRow(0, "a"), 1 to PagedRow(1, "a")))
            setContent {
                val columns = remember { pagedColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks =
                    remember {
                        RowBlocks<PagedRow>(
                            blockOf = { it.block },
                            blockHeader = { blockId, rows ->
                                Text("band-$blockId-${rows.first}-${rows.last}")
                            },
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = 6,
                        itemAt = { loaded[it] },
                        state = state,
                        columns = columns,
                        rowKey = pagedRowKey,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()
            onNodeWithText("band-a-0-1").assertIsDisplayed()

            loaded = loaded +
                mapOf(
                    2 to PagedRow(2, "a"),
                    3 to PagedRow(3, "a"),
                    4 to PagedRow(4, null),
                    5 to PagedRow(5, null),
                )
            waitForIdle()
            onNodeWithText("band-a-0-1").assertDoesNotExist()
            onNodeWithText("band-a-0-3").assertIsDisplayed()
        }

    @Test
    fun `drop onto a placeholder cancels and a later drop onto the loaded page commits`() =
        runComposeUiTest {
            val moves = mutableListOf<RowBlockMove>()
            var loaded by mutableStateOf(
                mapOf(
                    0 to PagedRow(0, "a"),
                    1 to PagedRow(1, "a"),
                    2 to PagedRow(2, null),
                ),
            )
            setContent {
                val columns = remember { pagedColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<PagedRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
                            blockHeader = pagedBlockDragHeader,
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { loaded[it] },
                        state = state,
                        columns = columns,
                        rowKey = pagedRowKey,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()

            // Overshoot past the end: the block lands last, directly after the unloaded row 3 —
            // an anchor whose key does not exist yet. The gesture must snap back and emit nothing.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(moves).isEqualTo(emptyList<RowBlockMove>())
            val blockTop = onNodeWithText("name-0").getBoundsInRoot().top
            val standaloneTop = onNodeWithText("name-2").getBoundsInRoot().top
            assertThat(blockTop).isLessThan(standaloneTop)

            // The page arrives; the same drop now has a loaded anchor and commits normally.
            loaded = loaded + mapOf(3 to PagedRow(3, null))
            waitForIdle()
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
            assertThat(move.afterKey).isEqualTo(3)
            assertThat(move.beforeKey).isNull()
        }

    @Test
    fun `embedded refused drop snaps the view back and a later drop onto the loaded page commits`() =
        runComposeUiTest {
            // Embedded twist on the paged drop policy: the embedded engine keys its drag offsets
            // on list equality alone, and a refusal is net-neutral over that list — without a
            // rebuild signal the view would keep showing the dropped order over a model that
            // snapped back, until some unrelated list change made the rows visibly jump.
            val moves = mutableListOf<RowBlockMove>()
            var loaded by mutableStateOf(
                mapOf(
                    0 to PagedRow(0, "a"),
                    1 to PagedRow(1, "a"),
                    2 to PagedRow(2, null),
                ),
            )
            setContent {
                val columns = remember { pagedColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<PagedRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
                            blockHeader = pagedBlockDragHeader,
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { loaded[it] },
                        state = state,
                        columns = columns,
                        rowKey = pagedRowKey,
                        rowBlocks = blocks,
                        embedded = true,
                    )
                }
            }
            waitForIdle()

            // Overshoot past the end: the block lands after the unloaded row 3, whose key does
            // not exist yet. Nothing may be emitted — and the VIEW, not just the model, must
            // show the restored order.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(moves).isEqualTo(emptyList<RowBlockMove>())
            val blockTop = onNodeWithText("name-0").getBoundsInRoot().top
            val standaloneTop = onNodeWithText("name-2").getBoundsInRoot().top
            assertThat(blockTop).isLessThan(standaloneTop)

            // The page arrives; the same drop now has a loaded anchor, and the engine rebuilt by
            // the refusal must deliver it as one normal commit.
            loaded = loaded + mapOf(3 to PagedRow(3, null))
            waitForIdle()
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
            assertThat(move.afterKey).isEqualTo(3)
            assertThat(move.beforeKey).isNull()
        }

    @Test
    fun `drop onto a loaded anchor commits despite a placeholder crossed en route`() =
        runComposeUiTest {
            val moves = mutableListOf<RowBlockMove>()
            val loaded =
                mapOf(
                    0 to PagedRow(0, "a"),
                    1 to PagedRow(1, "a"),
                    3 to PagedRow(3, null),
                )
            setContent {
                val columns = remember { pagedColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<PagedRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
                            blockHeader = pagedBlockDragHeader,
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = 4,
                        itemAt = { loaded[it] },
                        state = state,
                        columns = columns,
                        rowKey = pagedRowKey,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()

            // The drag swaps past the placeholder unit at row 2 and lands last, after loaded
            // row 3: crossing a placeholder is fine, only the landing anchors must be loaded.
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
            assertThat(move.afterKey).isEqualTo(3)
            assertThat(move.beforeKey).isNull()
        }

    @Test
    fun `page loading mid gesture keeps the drag alive and the drop commits`() =
        runComposeUiTest {
            // The go/no-go check for paged drag: a page arriving under a held gesture replaces
            // placeholder index-keys with item keys beneath the engine. The dragged unit's own key
            // is stable, so the drag must survive the neighbour churn and settle into one commit
            // whose anchors come from the freshly loaded page.
            val moves = mutableListOf<RowBlockMove>()
            var loaded by mutableStateOf(
                mapOf(
                    0 to PagedRow(0, "a"),
                    1 to PagedRow(1, "a"),
                    2 to PagedRow(2, null),
                ),
            )
            setContent {
                val columns = remember { pagedColumns() }
                val state =
                    rememberTableState(
                        columns = persistentListOf("handle", "name"),
                        settings = TableSettings(rowReorderEnabled = true),
                        dimensions = TableDefaults.compactDimensions(),
                    )
                val blocks =
                    remember {
                        RowBlocks<PagedRow>(
                            blockOf = { it.block },
                            onCommit = { moves += it },
                            blockHeader = pagedBlockDragHeader,
                        )
                    }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = 6,
                        itemAt = { loaded[it] },
                        state = state,
                        columns = columns,
                        rowKey = pagedRowKey,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()

            // Start the drag and hold it mid-list — no release yet. Steps stay under one row
            // height (36dp compact): the engine swaps with the item whose CENTER the dragged
            // rect covers, one per event, so a coarser step can pass two centers and drop a swap
            // — that would blur what the landing assertion below is allowed to prove.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(2) { moveBy(Offset(0f, 40f)) }
            }
            waitForIdle()

            // The page under the pointer loads: rows 3..5 appear, their lazy keys flip from
            // placeholder index-keys to item keys.
            loaded = loaded +
                mapOf(
                    3 to PagedRow(3, null),
                    4 to PagedRow(4, null),
                    5 to PagedRow(5, null),
                )
            waitForIdle()

            // The same gesture continues over the fresh rows to the end of the list. Landing
            // last requires swaps that can only happen AFTER the load, so the final anchor is
            // itself the proof that the churn did not kill or corrupt the gesture.
            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                repeat(2) { moveBy(Offset(0f, 40f)) }
                repeat(4) { moveBy(Offset(0f, 10f)) }
                up()
            }
            waitForIdle()

            assertThat(moves.size).isEqualTo(1)
            val move = moves.single()
            assertThat(move.blockId).isEqualTo("a")
            assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
            assertThat(move.afterKey).isEqualTo(5)
            assertThat(move.beforeKey).isNull()
        }
}
