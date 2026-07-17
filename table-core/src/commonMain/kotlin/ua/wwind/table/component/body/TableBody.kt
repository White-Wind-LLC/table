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
    // With blocks the drag is managed: units are blocks, so per-row onRowMove semantics cannot
    // apply and display-only blocks (no onCommit) render without any drag at all.
    val rowReorderEnabled =
        state.settings.isRowReorderEnabled &&
            (if (blocks != null) blocks.config.onCommit != null else onRowMove != null)
    val reorderState =
        if (rowReorderEnabled) {
            rememberReorderableLazyListState(verticalState) { from, to ->
                if (blocks != null) {
                    // Synchronous by design: the engine re-reads layout right after this call and
                    // expects the swap already applied — the library's own permutation satisfies
                    // that, the consumer's data stays untouched until the commit.
                    blocks.applyUnitMove(from.index, to.index)
                } else {
                    onRowMove?.invoke(rowUnits.rowsOf(from.index).first, rowUnits.rowsOf(to.index).first)
                }
            }
        } else {
            null
        }

    // Commit side effects ride the drag lifecycle of the handle that owns the gesture, so the
    // event fires exactly once per completed drag no matter how many items recomposed meanwhile.
    val onBlockDragStarted: (() -> Unit)? =
        if (blocks != null) {
            {
                // An edit in flight validates against row positions that are about to shift.
                if (!state.tryCompleteEditing()) state.cancelEditing()
            }
        } else {
            null
        }
    val onBlockDragStopped: (() -> Unit)? =
        if (blocks != null) {
            {
                // Read the displacement before settle() — settling forgets the pre-gesture order.
                val remap = blocks.gestureRemap()
                val move = blocks.settle()
                // A settle can refuse the drop (paged policy: the anchor is still a placeholder)
                // and snap the view back; positional state then describes the restored order and
                // remapping it through the dead gesture's displacement would corrupt it.
                if (move != null && remap != null) {
                    // Positions whose content changed: cached heights describe other rows now, and
                    // re-measuring is the cheapest correct option.
                    for (viewIndex in 0 until blocks.itemsCount) {
                        if (remap(viewIndex) != viewIndex) state.rowHeightsPx.remove(viewIndex)
                    }
                    state.remapRowPositions(remap)
                }
            }
        } else {
            null
        }
    // The engine remembers its per-item scope for as long as the item's key survives — longer than
    // any one composition of this body, and across a TableState recreation (rememberTableState
    // swaps instances on settings/dimensions changes). Hooks frozen into that scope would keep
    // mutating the dead state — cancelling its edits, remapping its selection — while the live one
    // never hears about the drop. Same trampoline as the embedded settle path: the scope gets
    // stable lambdas that read the current hooks at gesture time.
    val currentOnBlockDragStarted = rememberUpdatedState(onBlockDragStarted)
    val currentOnBlockDragStopped = rememberUpdatedState(onBlockDragStopped)

    LazyColumn(
        modifier = modifier,
        state = verticalState,
        userScrollEnabled = enableScrolling,
    ) {
        // Everything the drag reads about the permutation — unit boundaries, item, block id, key AND
        // the is-leader test that gates the handle — must come from ONE snapshot read. [snapshot] is
        // that read: [units] below is its own [RowUnitIndex], and [RowUnit] derives each row's
        // is-leader flag from the same [rows] it hands down, so no reader falls back on the separately
        // published state.rowUnits, which can lag this snapshot by a frame under a fast drag. Without
        // blocks the units are identity and itemAt is the consumer's own, so the plain params already
        // form one source.
        val snap = blocks?.snapshot
        val units = snap?.units ?: rowUnits
        val unitItemAt: (Int) -> T? = if (snap != null) snap::itemAt else itemAt
        items(
            count = units.unitCount,
            key = { unit ->
                val leader = units.rowsOf(unit).first
                if (snap != null) snap.keyAt(leader) else rowKey(itemAt(leader), leader)
            },
        ) { unit ->
            val rows = units.rowsOf(unit)
            val isGroup = units.isGroup(unit)
            // Units only; the footer is a separate lazy item and never a group, so the bound keeps
            // the lookup in range and a trailing block still draws its closing gap above the footer.
            val nextIsGroup = unit < units.unitCount - 1 && units.isGroup(unit + 1)
            val blockId = if (isGroup) snap?.blockIdAt(rows.first) else null
            val key = if (snap != null) snap.keyAt(rows.first) else rowKey(itemAt(rows.first), rows.first)
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
                            blockHeader = blocks?.config?.blockHeader,
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
                        blockHeader = blocks?.config?.blockHeader,
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

    // With blocks the drag is managed: units are blocks, so per-row onRowMove semantics cannot
    // apply and display-only blocks (no onCommit) render without any drag at all.
    val rowReorderEnabled =
        state.settings.isRowReorderEnabled &&
            (if (blocks != null) blocks.config.onCommit != null else onRowMove != null)

    // One element per drag unit, holding that unit's leading item. `ReorderableColumn` keys its
    // internal drag state on this list *by equality*, and only rebuilds it — clearing the drag
    // offsets left over from a drop — when the contents change. Elements must therefore track the
    // items, not the unit indices: `0..unitCount-1` never changes and would strand those offsets.
    // An eager snapshot, not a view over `itemAt`: a lazy view reads live data, so consecutive
    // instances compare equal whenever unit geometry survives an upstream change — masking rebuilds
    // the engine needs — and the settle guard below would compare a list against itself.
    // Rebuilt on every composition, never memoized: without blocks `rowUnits` keys on the count
    // alone and a consumer's `itemAt` routinely keeps one identity over live data, so any remember
    // over them would survive an in-place list change — handing the engine, the row keys and the
    // settle guard a stale snapshot. The engine dedupes equal contents itself, and the embedded
    // path is sized for lists where an O(n) rebuild is noise. Wrapped with the refusal count
    // because a refused drop is the one gesture that nets to zero over the leaders — see
    // [EmbeddedUnitList].
    val unitList: List<T?> =
        EmbeddedUnitList(
            List(rowUnits.unitCount) { unit -> itemAt(rowUnits.rowsOf(unit).first) },
            blocks?.refusedDropCount ?: 0,
        )

    // `ReorderableColumn` captures `onSettle` once, when it builds that state, and never refreshes it
    // — unlike the lazy path, whose `rememberReorderableLazyListState` wraps the callback in
    // `rememberUpdatedState`. Leaders do not distinguish every layout this callback reads: inserting a
    // row *into* a group changes `rowUnits` while leaving the leaders — and so the list — equal, so a
    // captured callback would keep translating units against a stale index and report rows that no
    // longer match the data. Route through the state, so the callback that runs is the current one.
    val settleUnits: (List<T?>, Int, Int) -> Unit = settle@{ engineUnits, fromUnit, toUnit ->
        // Unit indices mean nothing outside the list the engine laid out. When the list changes
        // under an in-flight drag the engine rebuilds, and nothing in its contract stops the OLD
        // state's disposal from delivering the dead gesture's settle here — through the trampoline
        // above, which no snapshot latch in RowBlocksState can catch because embedded swaps arrive
        // only at drop. Applying those indices to the new geometry could commit a move the user
        // never made, so a settle is honored only when the engine's list still matches the rendered
        // one; dropping the rest is the documented cancel-on-external-change policy. An engine that
        // survived the change (equal leaders) keeps its indices meaningful and settles normally.
        if (engineUnits != unitList) return@settle
        if (blocks != null) {
            // The embedded engine reports the gesture's net result once, at drop, so one unit move
            // reproduces the whole drag and the commit settles in the same callback — the same
            // exactly-one-event contract the lazy path keeps via its drag-stop wrapper.
            blocks.applyUnitMove(fromUnit, toUnit)
            // Read the displacement before settle() — settling forgets the pre-gesture order.
            val remap = blocks.gestureRemap()
            val move = blocks.settle()
            // A settle can refuse the drop (paged policy: the anchor is still a placeholder). The
            // model snaps back, and the refusal count bumped inside settle() rebuilds the engine —
            // whose leftover drag offsets would otherwise keep showing the dropped order over the
            // restored one. Positional state then describes the restored order, and remapping it
            // through the dead gesture's displacement would corrupt it.
            if (move != null && remap != null) {
                // Positions whose content changed: cached heights describe other rows now, and
                // re-measuring is the cheapest correct option.
                for (viewIndex in 0 until blocks.itemsCount) {
                    if (remap(viewIndex) != viewIndex) state.rowHeightsPx.remove(viewIndex)
                }
                state.remapRowPositions(remap)
            }
        } else {
            onRowMove?.invoke(rowUnits.rowsOf(fromUnit).first, rowUnits.rowsOf(toUnit).first)
        }
    }
    val currentSettleUnits = rememberUpdatedState(settleUnits)

    // An edit in flight validates against row positions that are about to shift. Same trampoline
    // as the settle path: the per-item scope outlives any one composition of this body, so it gets
    // a stable lambda that reads the current hook at gesture time.
    val onBlockDragStarted: (() -> Unit)? =
        if (blocks != null) {
            {
                if (!state.tryCompleteEditing()) state.cancelEditing()
            }
        } else {
            null
        }
    val currentOnBlockDragStarted = rememberUpdatedState(onBlockDragStarted)

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
                                blockHeader = blocks?.config?.blockHeader,
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
                        blockHeader = blocks?.config?.blockHeader,
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
    /** Passed through to [TableRowItem]; both are derived by [RowUnit] from the unit snapshot that
     *  produced [index], keeping the leader test in step with the rendered position. */
    isInRowBlock: Boolean,
    isRowBlockLeader: Boolean,
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
        isRowBlockLeader = isRowBlockLeader,
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
 * The embedded engine's input list. `ReorderableColumn` clears the drag offsets left over from a
 * drop only by rebuilding its remembered state, and it rebuilds only when this list compares
 * unequal — but a REFUSED drop (paged policy) restores the view order, so the leaders alone
 * compare equal while the offsets still show the dropped order. Folding the refusal count into
 * equality makes every refusal a rebuild; the elements themselves stay untouched, so row identity
 * and the content lambdas are unaffected.
 */
private class EmbeddedUnitList<E>(
    private val elements: List<E>,
    private val refusedDrops: Int,
) : List<E> by elements {
    override fun equals(other: Any?): Boolean =
        other is EmbeddedUnitList<*> && refusedDrops == other.refusedDrops && elements == other.elements

    override fun hashCode(): Int = 31 * elements.hashCode() + refusedDrops
}
