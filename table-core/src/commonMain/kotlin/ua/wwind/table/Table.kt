package ua.wwind.table

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.component.TableHeader
import ua.wwind.table.component.TableHeaderDefaults
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.interaction.ContextMenuState
import ua.wwind.table.interaction.ensureCellFullyVisible
import ua.wwind.table.interaction.tableKeyboardNavigation
import ua.wwind.table.interaction.tableRowInteractions
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider

@Suppress("LongParameterList")
@ExperimentalTableApi
@Composable
/**
 * Composable data table that renders a header and a virtualized list of rows.
 *
 * - Columns are described by [columns] (`ColumnSpec`).
 * - Data is provided via [itemsCount] and [itemAt] loader.
 * - Sorting, filters, ordering and selection are controlled by [state].
 *
 * Generic parameters:
 * - [T] actual row item type.
 * - [C] column key type.
 *
 * @param itemsCount total number of rows to display
 * @param itemAt loader that returns an item for the given index; may return null while loading
 * @param state mutable table state (sorting, filters, order, selection)
 * @param columns list of visible/available column specifications
 * @param modifier layout modifier for the whole table
 * @param placeholderRow optional row content shown when an item is null
 * @param rowKey stable key for rows; defaults to index
 * @param rowLeading optional leading content per non-null row (e.g., avatar, checkbox)
 * @param rowTrailing optional trailing content per non-null row
 * @param onRowClick row primary action handler
 * @param onRowLongClick optional long-press handler
 * @param contextMenu optional context menu host, invoked with item and absolute position
 * @param customization styling hooks for rows and cells
 * @param colors container/content colors
 * @param strings string provider for UI text
 * @param verticalState list scroll state
 * @param horizontalState horizontal scroll state of the whole table
 * @param icons header icons used for sort and filter affordances
 */
public fun <T : Any, C> Table(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    columns: List<ColumnSpec<T, C>>,
    modifier: Modifier = Modifier,
    placeholderRow: (@Composable () -> Unit)? = null,
    rowKey: (item: T?, index: Int) -> Any = { _, i -> i },
    rowLeading: (@Composable (T) -> Unit)? = null,
    rowTrailing: (@Composable (T) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    onRowLongClick: ((T) -> Unit)? = null,
    contextMenu: (@Composable (item: T, pos: Offset, dismiss: () -> Unit) -> Unit)? = null,
    customization: TableCustomization<T, C> = DefaultTableCustomization(),
    colors: TableColors = TableDefaults.colors(),
    strings: StringProvider = DefaultStrings,
    verticalState: LazyListState = rememberLazyListState(),
    horizontalState: ScrollState = rememberScrollState(),
    icons: TableHeaderIcons = TableHeaderDefaults.icons(),
) {
    val dimensions = state.dimensions
    val visibleColumns by remember(columns, state.columnOrder) {
        derivedStateOf {
            state.columnOrder.mapNotNull { key -> columns.find { it.key == key && it.visible } }
        }
    }
    val tableWidth by remember(visibleColumns, rowLeading, state.columnWidths, state.dimensions) {
        derivedStateOf { computeTableWidth(visibleColumns, rowLeading != null, state) }
    }

    var contextMenuState by remember { mutableStateOf(ContextMenuState<T>()) }

    val tableFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Ensure selected cell is fully visible whenever it changes (including external API calls)
    LaunchedEffect(Unit) {
        snapshotFlow { state.selectedCell }.collectLatest { cell ->
            if (cell == null) return@collectLatest
            val colIndex = visibleColumns.indexOfFirst { it.key == cell.column }
            if (colIndex >= 0) {
                ensureCellFullyVisible(
                    rowIndex = cell.rowIndex,
                    targetColIndex = colIndex,
                    targetColKey = cell.column,
                    visibleColumns = visibleColumns,
                    state = state,
                    hasLeading = rowLeading != null,
                    tableWidth = tableWidth,
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    density = density,
                )
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .horizontalScroll(horizontalState)
                .tableKeyboardNavigation(
                    focusRequester = tableFocusRequester,
                    itemsCount = itemsCount,
                    state = state,
                    visibleColumns = visibleColumns,
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    hasLeading = rowLeading != null,
                    tableWidth = tableWidth,
                    density = density,
                    coroutineScope = coroutineScope,
                ),
    ) {
        if (state.settings.showActiveFiltersHeader) {
            ActiveFiltersHeader(
                columns = columns,
                state = state,
                strings = strings,
                width = tableWidth,
            )
        }

        TableHeader(
            columns = columns,
            state = state,
            tableWidth = tableWidth,
            headerColor = colors.headerContainerColor,
            headerContentColor = colors.headerContentColor,
            dimensions = dimensions,
            strings = strings,
            leadingColumnWidth = if (rowLeading != null) dimensions.defaultRowHeight else null,
            icons = icons,
        )
        HorizontalDivider(modifier = Modifier.width(tableWidth))

        TableBody(
            itemsCount = itemsCount,
            itemAt = itemAt,
            rowKey = rowKey,
            visibleColumns = visibleColumns,
            state = state,
            colors = colors,
            customization = customization,
            tableWidth = tableWidth,
            rowLeading = rowLeading,
            rowTrailing = rowTrailing,
            placeholderRow = placeholderRow,
            onRowClick = onRowClick,
            onRowLongClick = onRowLongClick,
            onContextMenu =
                contextMenu?.let {
                    { item: T, pos: Offset ->
                        contextMenuState = contextMenuState.copy(visible = true, position = pos, item = item)
                    }
                },
            verticalState = verticalState,
            horizontalState = horizontalState,
            requestTableFocus = { tableFocusRequester.requestFocus() },
        )
    }

    ContextMenuHost(
        contextMenuState = contextMenuState,
        contextMenu = contextMenu,
        onDismiss = { contextMenuState = contextMenuState.copy(visible = false) },
    )
}

private fun <T : Any, C> computeTableWidth(
    visibleColumns: List<ColumnSpec<T, C>>,
    hasLeading: Boolean,
    state: TableState<C>,
): Dp {
    val dimensions = state.dimensions
    var sum = 0.dp
    if (hasLeading) {
        sum += dimensions.defaultRowHeight + dimensions.verticalDividerThickness
    }
    visibleColumns.forEach { spec ->
        val w = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
        sum += w + dimensions.verticalDividerThickness
    }
    return sum
}

@Composable
private fun <T : Any, C> ActiveFiltersHeader(
    columns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    strings: StringProvider,
    width: Dp,
) {
    TableActiveFilters(
        columns = columns,
        state = state,
        strings = strings,
        modifier = Modifier.width(width),
        includeClearAllChip = true,
    )
    HorizontalDivider(modifier = Modifier.width(width))
}

@Composable
@Suppress("LongParameterList")
private fun <T : Any, C> TableBody(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    rowKey: (item: T?, index: Int) -> Any,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableWidth: Dp,
    rowLeading: (@Composable (T) -> Unit)?,
    rowTrailing: (@Composable (T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), state = verticalState) {
        items(count = itemsCount, key = { index -> rowKey(itemAt(index), index) }) { index ->
            val item = itemAt(index)
            TableRowItem(
                item = item,
                index = index,
                visibleColumns = visibleColumns,
                state = state,
                colors = colors,
                customization = customization,
                tableWidth = tableWidth,
                rowLeading = rowLeading,
                rowTrailing = rowTrailing,
                placeholderRow = placeholderRow,
                onRowClick = onRowClick,
                onRowLongClick = onRowLongClick,
                onContextMenu = onContextMenu,
                verticalState = verticalState,
                horizontalState = horizontalState,
                requestTableFocus = requestTableFocus,
            )
            HorizontalDivider(modifier = Modifier.width(tableWidth))
        }
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
private fun <T : Any, C> TableRowItem(
    item: T?,
    index: Int,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableWidth: Dp,
    rowLeading: (@Composable (T) -> Unit)?,
    rowTrailing: (@Composable (T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    val dimensions = state.dimensions
    val isSelected = state.selectedIndex == index

    val defaultRowBackgroundColor =
        when {
            isSelected -> colors.rowSelectedContainerColor
            state.settings.stripedRows && (index % 2 != 0) -> colors.stripedRowContainerColor
            else -> colors.rowContainerColor
        }

    val rowStyle: TableRowStyle? =
        item?.let { nonNull ->
            val ctx =
                TableRowContext<T, C>(
                    item = nonNull,
                    index = index,
                    isSelected = isSelected,
                    isStriped = state.settings.stripedRows && (index % 2 != 0),
                    isGroup = false,
                    isDeleted = false,
                )
            customization.resolveRowStyle(ctx)
        }

    val finalRowColor =
        rowStyle?.containerColor?.takeUnless { it == Unspecified } ?: defaultRowBackgroundColor
    val finalRowContentColor =
        rowStyle?.contentColor?.takeUnless { it == Unspecified } ?: contentColorFor(finalRowColor)

    val tonalElevation = rowStyle?.elevation?.takeUnless { it == Dp.Unspecified } ?: 0.dp

    Surface(
        color = finalRowColor,
        contentColor = finalRowContentColor,
        shape = rowStyle?.shape ?: RectangleShape,
        border = rowStyle?.border,
        tonalElevation = tonalElevation,
    ) {
        Row(modifier = Modifier.width(tableWidth).then(rowStyle?.modifier ?: Modifier)) {
            item?.let { itItem ->
                if (rowLeading != null) {
                    RowLeadingSection(
                        height = dimensions.defaultRowHeight,
                        dividerThickness = dimensions.verticalDividerThickness,
                        rowLeading = rowLeading,
                        item = itItem,
                    )
                }

                visibleColumns.forEach { spec ->
                    val width = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
                    var cellTopLeft by remember(spec.key, index) { mutableStateOf(Offset.Zero) }
                    val cellStyle: TableCellStyle =
                        customization.resolveCellStyle(
                            TableCellContext(
                                row =
                                    TableRowContext(
                                        item = itItem,
                                        index = index,
                                        isSelected = isSelected,
                                        isStriped = state.settings.stripedRows && (index % 2 != 0),
                                        isGroup = false,
                                        isDeleted = false,
                                    ),
                                column = spec.key,
                            ),
                        )

                    val isCellSelected =
                        state.selectedCell?.let { it.rowIndex == index && it.column == spec.key } == true

                    TableCell(
                        width = width,
                        height = dimensions.defaultRowHeight,
                        dividerThickness = dimensions.verticalDividerThickness,
                        cellStyle = cellStyle,
                        alignment = spec.alignment.toCellContentAlignment(),
                        isSelected = isCellSelected,
                        modifier =
                            Modifier
                                .onGloballyPositioned { coordinates ->
                                    cellTopLeft = coordinates.positionInRoot()
                                }.then(
                                    Modifier.tableRowInteractions(
                                        item = itItem,
                                        onFocus = {
                                            requestTableFocus()
                                            state.selectCell(index, spec.key)
                                            state.focusRow(index)
                                        },
                                        useSelectAsPrimary = state.settings.selectionMode != SelectionMode.None,
                                        onSelect = { state.toggleSelect(index) },
                                        onClick = onRowClick,
                                        onLongClick = onRowLongClick,
                                        onContextMenu =
                                            onContextMenu?.let { handler ->
                                                { itemCtx, localPos -> handler(itemCtx, cellTopLeft + localPos) }
                                            },
                                    ),
                                ),
                    ) {
                        spec.cell.invoke(this, itItem)
                    }
                }

                rowTrailing?.invoke(itItem)
            } ?: run {
                Row(
                    modifier = Modifier.height(dimensions.defaultRowHeight).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { placeholderRow?.invoke() }
            }
        }
    }
}

@Composable
private fun <T> RowLeadingSection(
    height: Dp,
    dividerThickness: Dp,
    rowLeading: @Composable (T) -> Unit,
    item: T,
) {
    Row {
        Box(
            modifier =
                Modifier
                    .height(height)
                    .width(height),
            contentAlignment = Alignment.Center,
        ) { rowLeading(item) }
        VerticalDivider(
            modifier = Modifier.height(height),
            thickness = dividerThickness,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun TableCell(
    width: Dp,
    height: Dp,
    dividerThickness: Dp,
    cellStyle: TableCellStyle,
    alignment: Alignment,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundModifier =
        if (cellStyle.background != Unspecified) Modifier.background(cellStyle.background) else Modifier
    val selectionBorderModifier =
        if (isSelected) Modifier.border(2.dp, LocalContentColor.current, RoundedCornerShape(2.dp)) else Modifier

    Row(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .width(width)
                    .height(height)
                    .then(backgroundModifier)
                    .then(selectionBorderModifier),
            contentAlignment = alignment,
        ) {
            if (cellStyle.contentColor != Unspecified) {
                CompositionLocalProvider(LocalContentColor provides cellStyle.contentColor) {
                    content()
                }
            } else {
                content()
            }
        }
        VerticalDivider(modifier = Modifier.height(height), thickness = dividerThickness)
    }
}

@Composable
private fun <T : Any> ContextMenuHost(
    contextMenuState: ContextMenuState<T>,
    contextMenu: (@Composable (item: T, pos: Offset, dismiss: () -> Unit) -> Unit)?,
    onDismiss: () -> Unit,
) {
    if (contextMenuState.visible) {
        contextMenuState.item?.let { item ->
            contextMenu?.invoke(item, contextMenuState.position, onDismiss)
        }
    }
}

private fun Alignment.Horizontal.toCellContentAlignment(): Alignment =
    when (this) {
        Alignment.Start -> Alignment.CenterStart
        Alignment.CenterHorizontally -> Alignment.Center
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
