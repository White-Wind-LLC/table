package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.footer.TableFooter
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
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
    onContextMenu: ((T, Offset) -> Unit)?,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
    enableScrolling: Boolean,
    modifier: Modifier = Modifier,
) {
    val showFooter = state.settings.showFooter && !state.settings.footerPinned

    LazyColumn(
        modifier = modifier,
        state = verticalState,
        userScrollEnabled = enableScrolling,
    ) {
        items(count = itemsCount, key = { index -> rowKey(itemAt(index), index) }) { index ->
            TableBodyRow(
                index = index,
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

        // Add footer as last item if not pinned
        if (showFooter) {
            item(key = "footer") {
                HorizontalDivider(modifier = Modifier.width(state.tableWidth))
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
    onContextMenu: ((T, Offset) -> Unit)?,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    if (itemsCount <= 0 && !state.settings.showFooter) return

    Column {
        for (index in 0 until itemsCount) {
            TableBodyRow(
                index = index,
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

        // Add footer for embedded tables (always non-pinned)
        if (state.settings.showFooter) {
            HorizontalDivider(modifier = Modifier.width(state.tableWidth))
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
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun <T : Any, C, E> TableBodyRow(
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
                        spec = groupSpec,
                        width = viewportWidthDp,
                        height = state.dimensions.rowHeight,
                        colors = colors,
                        customization = customization,
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(viewportWidthDp),
                        thickness = state.dimensions.dividerThickness,
                    )
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

    HorizontalDivider(
        modifier = Modifier.width(state.tableWidth),
        thickness = state.dimensions.dividerThickness,
    )
}
