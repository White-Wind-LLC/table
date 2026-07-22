package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle

/**
 * `runComposeUiTest` for the tests that drive a drag through the reorderable engine.
 *
 * The v2 test API composes on a `StandardTestDispatcher`, which queues coroutines instead of
 * running them inline. The reorderable engine both starts a drag and applies every swap from a
 * `scope.launch` inside its pointer callbacks, so under the queued dispatcher an injected gesture
 * reorders nothing: a block dragged past the end of a non-scrolling list never crosses a single
 * row, and the drop that should land last lands nowhere. Pumping the clock between the injected
 * move events does not recover it. Composing on an `UnconfinedTestDispatcher` keeps those launches
 * inline, which is the timing a real drag gets from its frames.
 *
 * Tests that only compose and assert use the v2 default instead.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal fun runDragUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    runComposeUiTest(effectContext = UnconfinedTestDispatcher(), block = block)

internal data class BlockRow(
    val id: Int,
    val block: String?,
) {
    val name: String get() = "name-$id"
}

/**
 * A handle column plus a plain content column. Every row carries a drag handle tagged `handle-<id>`:
 * a standalone row's handle drives the outer (unit) engine, a block row's handle drives the nested
 * within-block engine. The whole block is dragged from its header handle (see [blockDragHeader]).
 */
internal fun blockColumns() =
    tableColumns<BlockRow, String, Unit> {
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

/**
 * A block header carrying the whole-block drag handle tagged `block-handle-<blockId>`, plus a
 * `band-<blockId>` label. Dragging the handle drags the entire block through the outer engine.
 */
internal val blockDragHeader: @Composable context(RowBlockHeaderScope)
(blockId: Any, rows: IntRange) -> Unit =
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

internal val stableRowKey: (BlockRow?, Int) -> Any = { item, index -> item?.id ?: "ph-$index" }

/**
 * Records the `isInRowBlock` flag handed to row styling per row id. Proves the boolean the
 * derivation feeds into `resolveRowStyle` — the only channel by which customization can style block
 * members — carries the real membership, not a constant that would pass every other block test.
 */
internal class RecordingCustomization(
    val inBlockById: MutableMap<Int, Boolean>,
) : TableCustomization<BlockRow, String> {
    @Composable
    override fun resolveRowStyle(ctx: TableRowContext<BlockRow, String>): TableRowStyle {
        inBlockById[ctx.item.id] = ctx.isInRowBlock
        return TableRowStyle()
    }
}
