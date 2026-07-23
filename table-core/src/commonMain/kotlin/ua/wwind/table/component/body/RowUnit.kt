package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import ua.wwind.table.ColumnSpec
import ua.wwind.table.DefaultTableItemScope
import ua.wwind.table.RowBlockHeaderScope
import ua.wwind.table.RowBlockHeaderScopeImpl
import ua.wwind.table.TableItemListDragScope
import ua.wwind.table.TableItemScope
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.resolveRowBlockContainerColor
import ua.wwind.table.state.TableState

/**
 * Renders one drag unit: a single row, or a block drawn as one card over a
 * [TableColors.rowBlockContainerColor] background that also spans the gap around its rows.
 *
 * A block opens with a band — the [blockHeader] or a `rowBlockSpacing` gap — and closes with a
 * bottom gap only when [nextIsGroup] is false, so two adjacent blocks never stack two tinted
 * regions into one slab. The header is floored at `rowBlockSpacing` for the same reason: one that
 * measures zero would butt two blocks together. No horizontal insets — they would shift the rows
 * relative to the header and break column alignment.
 *
 * Drag has two levels: the header renders under the ambient outer scope (its handle drags the whole
 * block), while a non-null [onRowMoveWithinBlock] wraps the rows in a nested [ReorderableColumn]
 * holding only this block's rows — so a row handle reorders within the block and can never leave it.
 */
@Composable
@Suppress("LongParameterList")
context(itemScope: TableItemScope)
internal fun <T : Any, C, E> RowUnit(
    rows: IntRange,
    isGroup: Boolean,
    /** True when the following unit is a block, which opens with a band of its own. */
    nextIsGroup: Boolean,
    /** Identity of the block this unit renders; null for single-row units. */
    blockId: Any?,
    /** Optional content for the band above a block; replaces the block's top gap. Its
     *  [RowBlockHeaderScope] receiver carries the whole-block drag handle. */
    blockHeader: (
        @Composable context(RowBlockHeaderScope)
        (blockId: Any, rows: IntRange) -> Unit
    )?,
    /** View-space within-block move `(fromView, toView)`. Null disables within-block reorder — the
     *  block's rows render without an inner drag engine. */
    onRowMoveWithinBlock: ((fromView: Int, toView: Int) -> Unit)?,
    /** Fires once when a within-block gesture starts (edit cancellation). */
    onWithinBlockDragStart: (() -> Unit)?,
    /** Stable key for the row at a view index — feeds the nested column's Compose node identity. */
    rowKeyAt: (Int) -> Any,
    /** Bumped on every refused drop; folded into the nested column's list equality so a refusal
     *  rebuilds the inner engine and clears its leftover drag offsets. */
    withinBlockRefusalCount: Int,
    itemAt: (Int) -> T?,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableData: E,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    if (!isGroup) {
        // A standalone unit is a single row rendered under the ambient outer scope; its cell handle
        // drives the outer (unit) engine.
        TableBodyRow(
            index = rows.first,
            isInRowBlock = false,
            itemAt = itemAt,
            visibleColumns = visibleColumns,
            state = state,
            colors = colors,
            customization = customization,
            tableData = tableData,
            rowEmbedded = rowEmbedded,
            placeholderRow = placeholderRow,
            onRowClick = onRowClick,
            onRowLongClick = onRowLongClick,
            onContextMenu = onContextMenu,
            horizontalState = horizontalState,
            requestTableFocus = requestTableFocus,
        )
        return
    }

    // A header cannot render without its block id: runs only form over loaded rows, so a block
    // unit always has one — the null check is for the type system, not a reachable state.
    val header = blockHeader?.takeIf { blockId != null }
    Column(
        modifier =
            Modifier
                .width(state.tableWidth)
                .background(resolveRowBlockContainerColor(colors))
                .padding(
                    top =
                        if (header != null) {
                            0.dp
                        } else {
                            state.dimensions.rowBlockSpacing
                        },
                    bottom =
                        if (nextIsGroup) {
                            0.dp
                        } else {
                            state.dimensions.rowBlockSpacing
                        },
                ),
    ) {
        if (header != null && blockId != null) {
            // Only the lazy path attaches the horizontal scroll node; on the embedded path the
            // viewport reports 0 and the whole table is the viewport.
            val viewportWidth =
                with(LocalDensity.current) { horizontalState.viewportSize.takeIf { it > 0 }?.toDp() }
                    ?: state.tableWidth
            // Rendered under the ambient outer scope so the header's drag handle drives the whole
            // block through the outer engine.
            val headerScope = remember(itemScope) { RowBlockHeaderScopeImpl(itemScope) }
            Box(
                modifier =
                    Modifier
                        .heightIn(min = state.dimensions.rowBlockSpacing)
                        .graphicsLayer {
                            translationX = horizontalState.value.toFloat()
                        },
            ) {
                Box(modifier = Modifier.width(viewportWidth)) {
                    context(headerScope) { header(blockId, rows) }
                }
            }
        }

        if (onRowMoveWithinBlock != null) {
            // Embedded-style: onSettle reports the net move at drop, so the settle applies once.
            val blockItems = EmbeddedUnitList(rows.map { itemAt(it) }, withinBlockRefusalCount)
            // Read live: ReorderableColumn freezes onSettle, but a whole-block move shifts rows.first.
            val blockBaseOffset = rememberUpdatedState(rows.first)
            ReorderableColumn(
                list = blockItems,
                onSettle = { fromLocal, toLocal ->
                    onRowMoveWithinBlock(blockBaseOffset.value + fromLocal, blockBaseOffset.value + toLocal)
                },
            ) { localIndex, _, _ ->
                val rowIndex = rows.first + localIndex
                key(rowKeyAt(rowIndex)) {
                    ReorderableItem {
                        val innerScope: TableItemScope =
                            remember(this) {
                                TableItemListDragScope(
                                    this,
                                    onDragStartedHook = { onWithinBlockDragStart?.invoke() },
                                )
                            }
                        context(innerScope) {
                            TableBodyRow(
                                index = rowIndex,
                                isInRowBlock = true,
                                itemAt = itemAt,
                                visibleColumns = visibleColumns,
                                state = state,
                                colors = colors,
                                customization = customization,
                                tableData = tableData,
                                rowEmbedded = rowEmbedded,
                                placeholderRow = placeholderRow,
                                onRowClick = onRowClick,
                                onRowLongClick = onRowLongClick,
                                onContextMenu = onContextMenu,
                                horizontalState = horizontalState,
                                requestTableFocus = requestTableFocus,
                            )
                        }
                    }
                }
            }
        } else {
            // Within-block reorder disabled: rows are inert (no handle binds anywhere), the block
            // still drags whole from its header.
            rows.forEach { row ->
                context(DefaultTableItemScope) {
                    TableBodyRow(
                        index = row,
                        isInRowBlock = true,
                        itemAt = itemAt,
                        visibleColumns = visibleColumns,
                        state = state,
                        colors = colors,
                        customization = customization,
                        tableData = tableData,
                        rowEmbedded = rowEmbedded,
                        placeholderRow = placeholderRow,
                        onRowClick = onRowClick,
                        onRowLongClick = onRowLongClick,
                        onContextMenu = onContextMenu,
                        horizontalState = horizontalState,
                        requestTableFocus = requestTableFocus,
                    )
                }
            }
        }
    }
}
