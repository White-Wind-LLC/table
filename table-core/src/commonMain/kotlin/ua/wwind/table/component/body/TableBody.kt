package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.DefaultTableItemScope
import ua.wwind.table.TableItemDragScope
import ua.wwind.table.TableItemListDragScope
import ua.wwind.table.TableItemScope
import ua.wwind.table.component.footer.TableFooter
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.isRowReorderEnabled
import ua.wwind.table.state.RowBlocksState
import ua.wwind.table.state.RowUnitIndex
import ua.wwind.table.state.TableState

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> TableBody(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    rowKey: (item: T?, index: Int) -> Any,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableData: E,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    blocks: RowBlocksState<T>?,
    onContextMenu: ((T, Offset) -> Unit)?,
    rowUnits: RowUnitIndex,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
    enableScrolling: Boolean,
    modifier: Modifier = Modifier,
) {
    val showFooter = state.settings.showFooter && !state.settings.footerPinned
    val hooks = rowDragHooks(state, blocks, onRowMove)
    val rowReorderEnabled = hooks.rowReorderEnabled
    val withinBlockEnabled = hooks.withinBlockEnabled
    val blockHeader = blocks?.config?.blockHeader
    val withinBlockRefusalCount = blocks?.refusedDropCount ?: 0
    val reorderState =
        if (rowReorderEnabled) {
            rememberReorderableLazyListState(verticalState) { from, to ->
                if (blocks != null) {
                    // Synchronous by design: the engine re-reads layout right after this call.
                    blocks.applyUnitMove(from.index, to.index)
                } else {
                    onRowMove?.invoke(rowUnits.rowsOf(from.index).first, rowUnits.rowsOf(to.index).first)
                }
            }
        } else {
            null
        }

    // The engine keeps a per-item scope longer than any composition of this body — and across a
    // TableState swap — so the scope gets stable lambdas that read the current hooks at gesture time.
    val currentOnBlockDragStarted = rememberUpdatedState(hooks.onBlockDragStarted)
    val currentOnBlockDragStopped = rememberUpdatedState(hooks.onBlockDragStopped)
    val currentOnRowMoveWithinBlock = rememberUpdatedState(hooks.onRowMoveWithinBlock)

    LazyColumn(
        modifier = modifier,
        state = verticalState,
        userScrollEnabled = enableScrolling,
    ) {
        // Everything the drag reads about the permutation — unit boundaries, item, block id, key —
        // must come from ONE snapshot read; state.rowUnits can lag it by a frame under a fast drag.
        val snap = blocks?.snapshot
        val units = snap?.units ?: rowUnits
        val unitItemAt: (Int) -> T? = if (snap != null) snap::itemAt else itemAt
        // Stable per-row key, drawn from the same snapshot as everything else the drag reads. Also
        // serves as the nested within-block column's node identity.
        val keyOf: (Int) -> Any =
            if (snap != null) snap::keyAt else { row -> rowKey(itemAt(row), row) }
        items(
            count = units.unitCount,
            key = { unit -> keyOf(units.rowsOf(unit).first) },
        ) { unit ->
            val rows = units.rowsOf(unit)
            val isGroup = units.isGroup(unit)
            // Units only; the footer is a separate lazy item and never a group, so the bound keeps
            // the lookup in range and a trailing block still draws its closing gap above the footer.
            val nextIsGroup = unit < units.unitCount - 1 && units.isGroup(unit + 1)
            val blockId = if (isGroup) snap?.blockIdAt(rows.first) else null
            val key = keyOf(rows.first)
            val currentReorderState = reorderState
            if (currentReorderState != null) {
                ReorderableItem(state = currentReorderState, key = key) {
                    val rowScope: TableItemScope =
                        remember(this) {
                            TableItemDragScope(
                                this,
                                onDragStartedHook = { currentOnBlockDragStarted.value?.invoke() },
                                onDragStoppedHook = { currentOnBlockDragStopped.value?.invoke() },
                            )
                        }
                    context(rowScope) {
                        RowUnit(
                            rows = rows,
                            isGroup = isGroup,
                            nextIsGroup = nextIsGroup,
                            blockId = blockId,
                            blockHeader = blockHeader,
                            onRowMoveWithinBlock =
                                if (withinBlockEnabled) {
                                    { fromView, toView -> currentOnRowMoveWithinBlock.value?.invoke(fromView, toView) }
                                } else {
                                    null
                                },
                            onWithinBlockDragStart = { currentOnBlockDragStarted.value?.invoke() },
                            rowKeyAt = keyOf,
                            withinBlockRefusalCount = withinBlockRefusalCount,
                            itemAt = unitItemAt,
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
            } else {
                context(DefaultTableItemScope) {
                    RowUnit(
                        rows = rows,
                        isGroup = isGroup,
                        nextIsGroup = nextIsGroup,
                        blockId = blockId,
                        blockHeader = blockHeader,
                        onRowMoveWithinBlock = null,
                        onWithinBlockDragStart = null,
                        rowKeyAt = keyOf,
                        withinBlockRefusalCount = withinBlockRefusalCount,
                        itemAt = unitItemAt,
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

        // Add footer as last item if not pinned
        if (showFooter) {
            item(key = "footer") {
                if (state.settings.showRowDividers) {
                    HorizontalDivider(modifier = Modifier.width(state.tableWidth))
                }
                TableFooter(
                    visibleColumns = visibleColumns,
                    widthResolver = { key ->
                        val spec = visibleColumns.firstOrNull { it.key == key }
                        state.resolveColumnWidth(key, spec)
                    },
                    tableData = tableData,
                    footerColor = colors.footerContainerColor,
                    footerContentColor = colors.footerContentColor,
                    dimensions = state.dimensions,
                    horizontalState = horizontalState,
                    tableWidth = state.tableWidth,
                    pinnedColumnsCount = state.settings.pinnedColumnsCount,
                    pinnedColumnsSide = state.settings.pinnedColumnsSide,
                    pinned = false,
                    showVerticalDividers = state.settings.showVerticalDividers,
                )
            }
        }
    }
    // Offscreen prefetch of the next viewport to make PgDn precise with dynamic row heights
    TableViewportPrefetcher(
        itemsCount = itemsCount,
        itemAt = itemAt,
        visibleColumns = visibleColumns,
        state = state,
        colors = colors,
        customization = customization,
        tableData = tableData,
        placeholderRow = placeholderRow,
        verticalState = verticalState,
        requestTableFocus = requestTableFocus,
        horizontalState = horizontalState,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> TableBodyEmbedded(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    rowKey: (item: T?, index: Int) -> Any,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableData: E,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    blocks: RowBlocksState<T>?,
    onContextMenu: ((T, Offset) -> Unit)?,
    rowUnits: RowUnitIndex,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    if (itemsCount <= 0 && !state.settings.showFooter) return

    val hooks = rowDragHooks(state, blocks, onRowMove)
    val rowReorderEnabled = hooks.rowReorderEnabled
    val withinBlockEnabled = hooks.withinBlockEnabled
    val blockHeader = blocks?.config?.blockHeader
    val withinBlockRefusalCount = blocks?.refusedDropCount ?: 0

    // One element per drag unit, holding that unit's leading item. `ReorderableColumn` keys its drag
    // state on this list by equality, so elements must track items (not unit indices, which never
    // change) and must never be memoized (a stale snapshot would mask rebuilds the engine needs).
    val unitList: List<T?> =
        EmbeddedUnitList(
            List(rowUnits.unitCount) { unit -> itemAt(rowUnits.rowsOf(unit).first) },
            withinBlockRefusalCount,
        )

    // `ReorderableColumn` captures `onSettle` once and never refreshes it, so route through the
    // state: equal leaders can hide a changed `rowUnits`, and a captured callback would then
    // translate units against a stale index.
    val settleUnits: (List<T?>, Int, Int) -> Unit = { engineUnits, fromUnit, toUnit ->
        settleEmbeddedUnits(engineUnits, unitList, fromUnit, toUnit, state, blocks, rowUnits, onRowMove)
    }
    val currentSettleUnits = rememberUpdatedState(settleUnits)
    val currentOnBlockDragStarted = rememberUpdatedState(hooks.onBlockDragStarted)
    val currentOnRowMoveWithinBlock = rememberUpdatedState(hooks.onRowMoveWithinBlock)

    // Stable per-row key for the nested within-block column's node identity.
    val rowKeyAt: (Int) -> Any = { i -> rowKey(itemAt(i), i) }

    Column {
        if (rowReorderEnabled) {
            ReorderableColumn(
                list = unitList,
                // `unitList` is captured alongside the callback: the guard in settleUnits needs to
                // know which list THIS engine laid out, and the engine keeps the capture for life.
                onSettle = { fromUnit, toUnit -> currentSettleUnits.value(unitList, fromUnit, toUnit) },
            ) { unitIndex, leadingItem, _ ->
                val rows = rowUnits.rowsOf(unitIndex)
                val isGroup = rowUnits.isGroup(unitIndex)
                key(rowKey(leadingItem, rows.first)) {
                    ReorderableItem {
                        val rowScope: TableItemScope =
                            remember(this) {
                                TableItemListDragScope(
                                    this,
                                    onDragStartedHook = { currentOnBlockDragStarted.value?.invoke() },
                                )
                            }
                        context(rowScope) {
                            RowUnit(
                                rows = rows,
                                isGroup = isGroup,
                                nextIsGroup =
                                    unitIndex < rowUnits.unitCount - 1 &&
                                        rowUnits.isGroup(unitIndex + 1),
                                blockId = if (isGroup) blocks?.blockIdAt(rows.first) else null,
                                blockHeader = blockHeader,
                                onRowMoveWithinBlock =
                                    if (withinBlockEnabled) {
                                        {
                                            fromView,
                                            toView,
                                            ->
                                            currentOnRowMoveWithinBlock.value?.invoke(fromView, toView)
                                        }
                                    } else {
                                        null
                                    },
                                onWithinBlockDragStart = { currentOnBlockDragStarted.value?.invoke() },
                                rowKeyAt = rowKeyAt,
                                withinBlockRefusalCount = withinBlockRefusalCount,
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
            for (unitIndex in 0 until rowUnits.unitCount) {
                val rows = rowUnits.rowsOf(unitIndex)
                val isGroup = rowUnits.isGroup(unitIndex)
                context(DefaultTableItemScope) {
                    RowUnit(
                        rows = rows,
                        isGroup = isGroup,
                        nextIsGroup =
                            unitIndex < rowUnits.unitCount - 1 &&
                                rowUnits.isGroup(unitIndex + 1),
                        blockId = if (isGroup) blocks?.blockIdAt(rows.first) else null,
                        blockHeader = blockHeader,
                        onRowMoveWithinBlock = null,
                        onWithinBlockDragStart = null,
                        rowKeyAt = rowKeyAt,
                        withinBlockRefusalCount = withinBlockRefusalCount,
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

        // Add footer for embedded tables (always non-pinned)
        if (state.settings.showFooter) {
            if (state.settings.showRowDividers) {
                HorizontalDivider(modifier = Modifier.width(state.tableWidth))
            }
            TableFooter(
                visibleColumns = visibleColumns,
                widthResolver = { key ->
                    val spec = visibleColumns.firstOrNull { it.key == key }
                    state.resolveColumnWidth(key, spec)
                },
                tableData = tableData,
                footerColor = colors.footerContainerColor,
                footerContentColor = colors.footerContentColor,
                dimensions = state.dimensions,
                horizontalState = horizontalState,
                tableWidth = state.tableWidth,
                pinnedColumnsCount = state.settings.pinnedColumnsCount,
                pinnedColumnsSide = state.settings.pinnedColumnsSide,
                pinned = false,
                showVerticalDividers = state.settings.showVerticalDividers,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
context(_: TableItemScope)
internal fun <T : Any, C, E> TableBodyRow(
    index: Int,
    /** Passed through to [TableRowItem]; derived by [RowUnit] from the unit snapshot that produced
     *  [index], so row styling stays in step with the rendered position. */
    isInRowBlock: Boolean,
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
    val density = LocalDensity.current
    val viewportWidthDp = with(density) { horizontalState.viewportSize.toDp() }

    val item = itemAt(index)
    val groupKey = state.groupBy
    val groupSpec =
        if (groupKey != null) visibleColumns.firstOrNull { it.key == groupKey } else null
    if (item != null && groupSpec != null) {
        val currentValue = groupSpec.valueOf(item)
        val previousValue =
            if (index > 0) itemAt(index - 1)?.let { groupSpec.valueOf(it) } else null
        if (index == 0 || currentValue != previousValue) {
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        translationX = horizontalState.value.toFloat()
                    },
            ) {
                Column {
                    GroupHeaderCell(
                        value = currentValue,
                        item = item,
                        tableData = tableData,
                        spec = groupSpec,
                        width = viewportWidthDp,
                        height = state.dimensions.rowHeight,
                        colors = colors,
                        customization = customization,
                    )
                    if (state.settings.showRowDividers) {
                        HorizontalDivider(
                            modifier = Modifier.width(viewportWidthDp),
                            thickness = state.dimensions.dividerThickness,
                        )
                    }
                }
            }
        }
    }

    TableRowItem(
        item = item,
        index = index,
        isInRowBlock = isInRowBlock,
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
        requestTableFocus = requestTableFocus,
        horizontalState = horizontalState,
    )

    if (state.settings.showRowDividers) {
        HorizontalDivider(
            modifier = Modifier.width(state.tableWidth),
            thickness = state.dimensions.dividerThickness,
        )
    }
}

/**
 * The embedded engine's input list. A refused drop restores the view order, so the elements alone
 * compare equal while the engine's drag offsets still show the dropped order; folding the refusal
 * count into equality is what forces the rebuild that clears them.
 */
internal class EmbeddedUnitList<E>(
    private val elements: List<E>,
    private val refusedDrops: Int,
) : List<E> by elements {
    override fun equals(other: Any?): Boolean =
        other is EmbeddedUnitList<*> && refusedDrops == other.refusedDrops && elements == other.elements

    override fun hashCode(): Int = 31 * elements.hashCode() + refusedDrops
}

/**
 * Commits an embedded whole-unit drag, if [engineUnits] is still the list this body laid out.
 *
 * Unit indices mean nothing outside the list the engine laid out: a rebuilt engine can still deliver
 * the dead gesture's settle, and applying it would commit a move nobody made. The embedded engine
 * reports the gesture's net result once, at drop, so the commit settles in this same call.
 */
@Suppress("LongParameterList")
private fun <T : Any, C> settleEmbeddedUnits(
    engineUnits: List<T?>,
    unitList: List<T?>,
    fromUnit: Int,
    toUnit: Int,
    state: TableState<C>,
    blocks: RowBlocksState<T>?,
    rowUnits: RowUnitIndex,
    onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
) {
    if (engineUnits != unitList) return
    if (blocks != null) {
        blocks.applyUnitMove(fromUnit, toUnit)
        blocks.settleAndRemap(state) { blocks.settle() }
    } else {
        onRowMove?.invoke(rowUnits.rowsOf(fromUnit).first, rowUnits.rowsOf(toUnit).first)
    }
}

/** The drag callbacks a table body wires to its blocks state, all null when reordering is off. */
private class RowDragHooks(
    val rowReorderEnabled: Boolean,
    val withinBlockEnabled: Boolean,
    val onBlockDragStarted: (() -> Unit)?,
    val onBlockDragStopped: (() -> Unit)?,
    val onRowMoveWithinBlock: ((fromView: Int, toView: Int) -> Unit)?,
)

/**
 * Resolves what the body may drag and the side effects each gesture commits.
 *
 * With blocks the drag is managed: units are blocks, so per-row [onRowMove] cannot apply. A block
 * table drags when it can move whole blocks or reorder rows within one; neither = display-only.
 *
 * The callbacks ride the drag lifecycle of the handle that owns the gesture, so each fires exactly
 * once per completed drag no matter how many items recomposed meanwhile.
 */
private fun <T : Any, C> rowDragHooks(
    state: TableState<C>,
    blocks: RowBlocksState<T>?,
    onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
): RowDragHooks {
    val rowReorderEnabled =
        state.settings.isRowReorderEnabled &&
            (
                if (blocks != null) {
                    blocks.config.onCommit != null || blocks.config.onRowReorderWithinBlock != null
                } else {
                    onRowMove != null
                }
            )
    val withinBlockEnabled =
        rowReorderEnabled && blocks != null && blocks.config.onRowReorderWithinBlock != null
    return RowDragHooks(
        rowReorderEnabled = rowReorderEnabled,
        withinBlockEnabled = withinBlockEnabled,
        // An edit in flight validates against row positions that are about to shift.
        onBlockDragStarted =
            if (blocks != null) {
                { if (!state.tryCompleteEditing()) state.cancelEditing() }
            } else {
                null
            },
        onBlockDragStopped =
            if (blocks != null) {
                { blocks.settleAndRemap(state) { blocks.settle() } }
            } else {
                null
            },
        // The nested within-block column reports its gesture once, at drop: apply the row move then
        // settle it, with the same remap and paged-refusal handling as the whole-block hook.
        onRowMoveWithinBlock =
            if (withinBlockEnabled) {
                { fromView, toView ->
                    blocks.applyRowMoveWithinBlock(fromView, toView)
                    blocks.settleAndRemap(state) { blocks.settleWithinBlock() }
                }
            } else {
                null
            },
    )
}

/**
 * Ends a finished block gesture with [commitMove] and re-syncs positional state through the
 * displacement it reports.
 *
 * The displacement must be read BEFORE settling — settling forgets the pre-gesture order. A refused
 * drop snaps back and reports no move, and remapping through the dead gesture's displacement would
 * corrupt the state it touches, so both halves have to be present before anything is rewritten.
 */
private fun <T : Any, C> RowBlocksState<T>.settleAndRemap(
    state: TableState<C>,
    commitMove: () -> Any?,
) {
    val remap = gestureRemap()
    val move = commitMove()
    if (move == null || remap == null) return
    // Cached heights describe other rows now; re-measuring is the cheapest fix.
    for (viewIndex in 0 until itemsCount) {
        if (remap(viewIndex) != viewIndex) state.rowHeightsPx.remove(viewIndex)
    }
    state.remapRowPositions(remap)
}
