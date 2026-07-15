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
    onRowsMove: ((from: IntRange, to: IntRange) -> Unit)?,
    rowGroupHeader: (@Composable (rows: IntRange) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    rowUnits: RowUnitIndex,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
    enableScrolling: Boolean,
    modifier: Modifier = Modifier,
) {
    val showFooter = state.settings.showFooter && !state.settings.footerPinned
    val rowReorderEnabled = state.settings.isRowReorderEnabled && (onRowsMove != null || onRowMove != null)
    val reorderState =
        if (rowReorderEnabled) {
            rememberReorderableLazyListState(verticalState) { from, to ->
                val fromRows = rowUnits.rowsOf(from.index)
                val toRows = rowUnits.rowsOf(to.index)
                if (onRowsMove != null) {
                    onRowsMove.invoke(fromRows, toRows)
                } else {
                    onRowMove?.invoke(fromRows.first, toRows.first)
                }
            }
        } else {
            null
        }

    LazyColumn(
        modifier = modifier,
        state = verticalState,
        userScrollEnabled = enableScrolling,
    ) {
        items(
            count = rowUnits.unitCount,
            key = { unit ->
                val leader = rowUnits.rowsOf(unit).first
                rowKey(itemAt(leader), leader)
            },
        ) { unit ->
            val rows = rowUnits.rowsOf(unit)
            val isGroup = rowUnits.isGroup(unit)
            // Units only; the footer is a separate lazy item and never a group, so the bound keeps
            // the lookup in range and a trailing block still draws its closing gap above the footer.
            val nextIsGroup = unit < rowUnits.unitCount - 1 && rowUnits.isGroup(unit + 1)
            val key = rowKey(itemAt(rows.first), rows.first)
            val currentReorderState = reorderState
            if (currentReorderState != null) {
                ReorderableItem(state = currentReorderState, key = key) {
                    val rowScope: TableItemScope =
                        remember(this) {
                            TableItemDragScope(this)
                        }
                    context(rowScope) {
                        RowUnit(
                            rows = rows,
                            isGroup = isGroup,
                            nextIsGroup = nextIsGroup,
                            rowGroupHeader = rowGroupHeader,
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
            } else {
                context(DefaultTableItemScope) {
                    RowUnit(
                        rows = rows,
                        isGroup = isGroup,
                        nextIsGroup = nextIsGroup,
                        rowGroupHeader = rowGroupHeader,
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
    onRowsMove: ((from: IntRange, to: IntRange) -> Unit)?,
    rowGroupHeader: (@Composable (rows: IntRange) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    rowUnits: RowUnitIndex,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    if (itemsCount <= 0 && !state.settings.showFooter) return

    val rowReorderEnabled =
        state.settings.isRowReorderEnabled && (onRowsMove != null || onRowMove != null)

    // One element per drag unit, holding that unit's leading item. `ReorderableColumn` keys its
    // internal drag state on this list *by equality*, and only rebuilds it — clearing the drag
    // offsets left over from a drop — when the contents change. Elements must therefore track the
    // items, not the unit indices: `0..unitCount-1` never changes and would strand those offsets.
    val unitList =
        remember(rowUnits, itemAt) {
            IndexedListAdapter(rowUnits.unitCount) { unit -> itemAt(rowUnits.rowsOf(unit).first) }
        }

    // `ReorderableColumn` captures `onSettle` once, when it builds that state, and never refreshes it
    // — unlike the lazy path, whose `rememberReorderableLazyListState` wraps the callback in
    // `rememberUpdatedState`. Leaders do not distinguish every layout this callback reads: inserting a
    // row *into* a group changes `rowUnits` while leaving the leaders — and so the list — equal, so a
    // captured callback would keep translating units against a stale index and report rows that no
    // longer match the data. Route through the state, so the callback that runs is the current one.
    val settleUnits: (Int, Int) -> Unit = { fromUnit, toUnit ->
        val fromRows = rowUnits.rowsOf(fromUnit)
        val toRows = rowUnits.rowsOf(toUnit)
        if (onRowsMove != null) {
            onRowsMove.invoke(fromRows, toRows)
        } else {
            onRowMove?.invoke(fromRows.first, toRows.first)
        }
    }
    val currentSettleUnits = rememberUpdatedState(settleUnits)

    Column {
        if (rowReorderEnabled) {
            ReorderableColumn(
                list = unitList,
                onSettle = { fromUnit, toUnit -> currentSettleUnits.value(fromUnit, toUnit) },
            ) { unitIndex, leadingItem, _ ->
                val rows = rowUnits.rowsOf(unitIndex)
                key(rowKey(leadingItem, rows.first)) {
                    ReorderableItem {
                        val rowScope: TableItemScope =
                            remember(this) {
                                TableItemListDragScope(this)
                            }
                        context(rowScope) {
                            RowUnit(
                                rows = rows,
                                isGroup = rowUnits.isGroup(unitIndex),
                                nextIsGroup =
                                    unitIndex < rowUnits.unitCount - 1 &&
                                        rowUnits.isGroup(unitIndex + 1),
                                rowGroupHeader = rowGroupHeader,
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
                context(DefaultTableItemScope) {
                    RowUnit(
                        rows = rowUnits.rowsOf(unitIndex),
                        isGroup = rowUnits.isGroup(unitIndex),
                        nextIsGroup =
                            unitIndex < rowUnits.unitCount - 1 &&
                                rowUnits.isGroup(unitIndex + 1),
                        rowGroupHeader = rowGroupHeader,
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

private class IndexedListAdapter<T>(
    private val itemsCount: Int,
    private val itemAt: (Int) -> T
) : AbstractList<T>() {
    override val size: Int
        get() = itemsCount

    override fun get(index: Int): T {
        require(index in 0 until itemsCount) {
            "Index $index is out of bounds for list size $itemsCount"
        }
        return itemAt(index)
    }
}

@Composable
@Suppress("LongParameterList")
context(_: TableItemScope)
internal fun <T : Any, C, E> TableBodyRow(
    index: Int,
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
