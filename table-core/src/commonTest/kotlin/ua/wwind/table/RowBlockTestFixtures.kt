package ua.wwind.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle

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
