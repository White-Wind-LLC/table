package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import assertk.assertions.isTrue
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.config.resolveRowBlockContainerColor
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RowBlocksTableTest {
    @Test
    fun `bands render from blockOf derivation without onCommit`() =
        runComposeUiTest {
            val items =
                listOf(
                    BlockRow(0, "alpha"),
                    BlockRow(1, "alpha"),
                    BlockRow(2, null),
                    BlockRow(3, "beta"),
                    BlockRow(4, "beta"),
                )
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
                    )
                }
            }
            waitForIdle()
            onNodeWithText("band-alpha").assertIsDisplayed()
            onNodeWithText("band-beta").assertIsDisplayed()
        }

    @Test
    fun `every row carries a drag handle`() =
        runComposeUiTest {
            // Handles are per-row now — block members included, not just a leader — because a block
            // row drives the nested within-block engine.
            val items =
                listOf(
                    BlockRow(0, "a"),
                    BlockRow(1, "a"),
                    BlockRow(2, null),
                    BlockRow(3, "a"),
                )
            setContent {
                val columns = remember { blockColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks = remember { RowBlocks<BlockRow>(blockOf = { it.block }) }
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
            waitForIdle()
            onNodeWithTag("handle-0", useUnmergedTree = true).assertExists()
            onNodeWithTag("handle-1", useUnmergedTree = true).assertExists()
            onNodeWithTag("handle-2", useUnmergedTree = true).assertExists()
            onNodeWithTag("handle-3", useUnmergedTree = true).assertExists()
        }

    @Test
    fun `isInRowBlock flag reaches customization true for block members false for standalone`() =
        runComposeUiTest {
            // The adjacent isRowBlockLeader flag is proven by the handle/member tags, but the
            // isInRowBlock boolean fed into resolveRowStyle is customization's only handle on block
            // membership; a regression to constant false/true would leave every other block test
            // green. Observe it directly through a recording customization.
            val inBlockById = mutableMapOf<Int, Boolean>()
            val items =
                listOf(
                    BlockRow(0, "alpha"),
                    BlockRow(1, "alpha"),
                    BlockRow(2, null),
                    BlockRow(3, "beta"),
                    BlockRow(4, "beta"),
                )
            setContent {
                val columns = remember { blockColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks = remember { RowBlocks<BlockRow>(blockOf = { it.block }) }
                val customization = remember { RecordingCustomization(inBlockById) }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                        customization = customization,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()
            // Members of the two derived blocks are marked; the standalone row between them is not.
            // isEqualTo(true/false) also rejects a missing (null) entry, so every row is observed.
            assertThat(inBlockById[0]).isEqualTo(true)
            assertThat(inBlockById[1]).isEqualTo(true)
            assertThat(inBlockById[2]).isEqualTo(false)
            assertThat(inBlockById[3]).isEqualTo(true)
            assertThat(inBlockById[4]).isEqualTo(true)
        }

    @Test
    fun `drop commits one move and remaps positional state`() =
        runComposeUiTest {
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
            // the overshoot makes the landing position deterministic (last), however many
            // intermediate swaps fire.
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
    fun `drag start completes or cancels an active cell edit`() =
        runComposeUiTest {
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
    fun `groupBy suppresses blocks and their commits and is surfaced`() =
        runComposeUiTest {
            val moves = mutableListOf<RowBlockMove>()
            val rowMoves = mutableListOf<Pair<Int, Int>>()
            val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null))
            lateinit var state: TableState<String>
            setContent {
                val columns = remember { blockColumns() }
                state =
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
                        // Superseded for good: suppression must not resurrect the per-row
                        // fallback, or the same gesture would silently change move semantics.
                        onRowMove = { from, to -> rowMoves += from to to },
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()
            onNodeWithText("band-a").assertExists()
            assertThat(state.rowBlocksSuppressedByGroupBy).isFalse()

            runOnIdle { state.groupBy("name") }
            waitForIdle()
            assertThat(state.rowBlocksSuppressedByGroupBy).isTrue()
            onNodeWithText("band-a").assertDoesNotExist()

            // Handles are inert while suppressed: a full drag gesture must commit nothing —
            // neither a block commit nor a per-row move.
            onNodeWithTag("handle-0", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(5) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()
            assertThat(moves).isEqualTo(emptyList<RowBlockMove>())
            assertThat(rowMoves).isEqualTo(emptyList<Pair<Int, Int>>())

            runOnIdle { state.groupBy(null) }
            waitForIdle()
            assertThat(state.rowBlocksSuppressedByGroupBy).isFalse()
            onNodeWithText("band-a").assertExists()
        }

    @Test
    fun `column menu group-by item is disabled while blocks are non-empty`() =
        runComposeUiTest {
            val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null))
            setContent {
                val columns = remember { blockColumns() }
                // Reorder stays disabled: the header menu locks entirely under reorder, and the
                // group-by conflict matters exactly for display-only blocks.
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks = remember { RowBlocks<BlockRow>(blockOf = { it.block }) }
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
            waitForIdle()
            onNodeWithText("Name").performTouchInput { longClick() }
            waitForIdle()
            onNodeWithText("Group by").assertIsNotEnabled()
        }

    @Test
    fun `column menu group-by item stays enabled without blocks`() =
        runComposeUiTest {
            val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null))
            setContent {
                val columns = remember { blockColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { items.getOrNull(it) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                    )
                }
            }
            waitForIdle()
            onNodeWithText("Name").performTouchInput { longClick() }
            waitForIdle()
            onNodeWithText("Group by").assertIsEnabled()
        }

    @Test
    fun `unspecified row block color resolves to the theme band color at draw`() =
        runComposeUiTest {
            val themed = Color(0xFF123456)
            var resolvedDefault: Color? = null
            var resolvedExplicit: Color? = null
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(surfaceContainerHighest = themed)) {
                    resolvedDefault = resolveRowBlockContainerColor(TableDefaults.colors())
                    resolvedExplicit =
                        resolveRowBlockContainerColor(
                            TableDefaults.colors(rowBlockContainerColor = Color.Red),
                        )
                }
            }
            waitForIdle()
            assertThat(resolvedDefault).isEqualTo(themed)
            assertThat(resolvedExplicit).isEqualTo(Color.Red)
        }

    @Test
    fun `an items change settles instead of recomposing forever`() =
        runComposeUiTest {
            var rootCompositions = 0
            var items by mutableStateOf(listOf(BlockRow(0, "a"), BlockRow(1, "a"), BlockRow(2, null)))
            setContent {
                SideEffect { rootCompositions++ }
                val columns = remember { blockColumns() }
                val state = rememberTableState(columns = persistentListOf("handle", "name"))
                val blocks = remember { RowBlocks<BlockRow>(blockOf = { it.block }) }
                Box(Modifier.size(400.dp, 640.dp)) {
                    Table(
                        itemsCount = items.size,
                        itemAt = { index -> items.getOrNull(index) },
                        state = state,
                        columns = columns,
                        rowKey = stableRowKey,
                        rowBlocks = blocks,
                    )
                }
            }
            waitForIdle()
            val settled = rootCompositions

            items = items + BlockRow(3, "b")
            waitForIdle()

            // reconcile() writes snapshot state during composition, which costs at most one extra
            // pass; anything more means the write keeps feeding its own readers.
            assertThat(rootCompositions - settled).isLessThan(4)
        }

    @Test
    fun `drag hooks land on a recreated TableState`() =
        runComposeUiTest {
            // rememberTableState swaps instances when dimensions change, while the reorder engine
            // keeps its per-item scopes alive across the swap. The commit side effects must reach
            // the live state, not the one captured when the scope was first built.
            val moves = mutableListOf<RowBlockMove>()
            val items =
                listOf(
                    BlockRow(0, "a"),
                    BlockRow(1, "a"),
                    BlockRow(2, null),
                    BlockRow(3, null),
                    BlockRow(4, null),
                )
            var dimensions by mutableStateOf(TableDefaults.compactDimensions())
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
                        dimensions = dimensions,
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
                    )
                }
            }
            waitForIdle()
            val firstState = state

            dimensions = TableDefaults.standardDimensions()
            waitForIdle()
            assertThat(state !== firstState).isTrue()
            runOnIdle {
                state.selection.toggleRow(3)
                state.selection.selectCell(3, "name")
            }
            waitForIdle()

            onNodeWithTag("block-handle-a", useUnmergedTree = true).performTouchInput {
                down(center)
                repeat(10) { moveBy(Offset(0f, 60f)) }
                up()
            }
            waitForIdle()

            assertThat(moves.size).isEqualTo(1)
            // id-3 sat at view position 3; the two-row block moved from above it to the end, so it
            // renders one unit higher — on the LIVE state.
            assertThat(state.selection.selectedIndex).isEqualTo(1)
            assertThat(state.selection.selectedCell?.rowIndex).isEqualTo(1)
        }

    @Test
    fun `default positional row key with blocks logs a single warning`() =
        runComposeUiTest {
            val warnings = mutableListOf<String>()
            val writer =
                object : LogWriter() {
                    override fun log(
                        severity: Severity,
                        message: String,
                        tag: String,
                        throwable: Throwable?,
                    ) {
                        if (severity == Severity.Warn) warnings += message
                    }
                }
            Logger.addLogWriter(writer)
            try {
                val items = listOf(BlockRow(0, "a"), BlockRow(1, "a"))
                var tick by mutableIntStateOf(0)
                setContent {
                    val columns = remember { blockColumns() }
                    val state = rememberTableState(columns = persistentListOf("handle", "name"))
                    val blocks = remember { RowBlocks<BlockRow>(blockOf = { it.block }) }
                    Text("tick-$tick")
                    Box(Modifier.size(400.dp, 640.dp)) {
                        Table(
                            itemsCount = items.size,
                            itemAt = { items.getOrNull(it) },
                            state = state,
                            columns = columns,
                            // rowKey deliberately omitted: the default positional key cannot
                            // anchor a RowBlockMove, which is exactly what must warn.
                            rowBlocks = blocks,
                        )
                    }
                }
                waitForIdle()
                tick = 1
                waitForIdle()
                assertThat(warnings.count { it.contains("stable rowKey") }).isEqualTo(1)
            } finally {
                Logger.setLogWriters(platformLogWriter())
            }
        }
}
