package ua.wwind.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.component.ActiveFiltersHeader
import ua.wwind.table.component.ContextMenuHost
import ua.wwind.table.component.TableHeader
import ua.wwind.table.component.TableHeaderDefaults
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.component.body.GroupHeaderCell
import ua.wwind.table.component.body.TableBody
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.interaction.ApplyAutoWidthEffect
import ua.wwind.table.interaction.ContextMenuState
import ua.wwind.table.interaction.ensureCellFullyVisible
import ua.wwind.table.interaction.tableKeyboardNavigation
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile
import ua.wwind.table.state.LocalTableState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.LocalStringProvider
import ua.wwind.table.strings.StringProvider
import kotlin.math.min

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
 * @param shape surface shape of the table
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
    shape: Shape = RoundedCornerShape(4.dp),
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

    // Consume drag and fling deltas so parent containers don't scroll while dragging inside the table
    val blockParentScrollConnection =
        remember {
            object : NestedScrollConnection {
                // Do not let parents pre-consume drag deltas from our gestures
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = Offset.Zero

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = if (source == NestedScrollSource.UserInput) available else Offset.Zero

                override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity = available
            }
        }

    // Used to participate in nested scroll from our custom drag handling
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }

    // Reset cached row heights when dataset size changes to avoid stale measurements
    LaunchedEffect(itemsCount) {
        state.rowHeightsPx.clear()
    }

    CompositionLocalProvider(
        LocalTableState provides state,
        LocalStringProvider provides strings,
    ) {
        // Ensure selected cell is fully visible whenever it changes (including external API calls)
        EnsureSelectedCellVisibleEffect(
            visibleColumns = visibleColumns,
            rowLeadingPresent = rowLeading != null,
            verticalState = verticalState,
            horizontalState = horizontalState,
        )
        val enableScrolling = remember { !getPlatform().isMobile() }

        Surface(
            shape = shape,
            border = BorderStroke(state.dimensions.dividerThickness, MaterialTheme.colorScheme.outlineVariant),
            modifier =
                modifier
                    .draggableTable(
                        horizontalState = horizontalState,
                        verticalState = verticalState,
                        blockParentScrollConnection = blockParentScrollConnection,
                        nestedScrollDispatcher = nestedScrollDispatcher,
                        enableScrolling = enableScrolling,
                    ).clip(shape)
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
            Column(
                modifier = Modifier,
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
                    leadingColumnWidth = if (rowLeading != null) dimensions.rowHeight else null,
                    icons = icons,
                )
                HorizontalDivider(modifier = Modifier.width(tableWidth))

                Box(modifier = Modifier.clipToBounds()) {
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
                                    contextMenuState =
                                        contextMenuState.copy(visible = true, position = pos, item = item)
                                }
                            },
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                        requestTableFocus = { tableFocusRequester.requestFocus() },
                        enableScrolling = enableScrolling,
                    )
                    if (state.groupBy != null) {
                        GroupStickyOverlay(
                            itemAt = itemAt,
                            visibleColumns = visibleColumns,
                            customization = customization,
                            colors = colors,
                            width = tableWidth,
                            verticalState = verticalState,
                            horizontalState = horizontalState,
                        )
                    }
                }
            }
        }
    }

    ContextMenuHost(
        contextMenuState = contextMenuState,
        contextMenu = contextMenu,
        onDismiss = { contextMenuState = contextMenuState.copy(visible = false) },
    )

    ApplyAutoWidthEffect(visibleColumns, itemsCount, verticalState, state)
}

private fun Modifier.draggableTable(
    horizontalState: ScrollState,
    verticalState: LazyListState,
    blockParentScrollConnection: NestedScrollConnection,
    nestedScrollDispatcher: NestedScrollDispatcher,
    enableScrolling: Boolean,
): Modifier =
    this
        .nestedScroll(blockParentScrollConnection, nestedScrollDispatcher)
        .pointerInput(horizontalState, verticalState) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    // Integrate with nested scroll: consume locally then report post-scroll
                    val consumedX =
                        if (dragAmount.x != 0f) {
                            horizontalState.dispatchRawDelta(-dragAmount.x)
                            dragAmount.x
                        } else {
                            0f
                        }
                    val consumedY =
                        if (dragAmount.y != 0f) {
                            verticalState.dispatchRawDelta(-dragAmount.y)
                            dragAmount.y
                        } else {
                            0f
                        }
                    nestedScrollDispatcher.dispatchPostScroll(
                        consumed = Offset(consumedX, consumedY),
                        available = Offset.Zero,
                        source = NestedScrollSource.UserInput,
                    )
                },
            )
        }.horizontalScroll(horizontalState, enableScrolling)

@Composable
@Suppress("LongParameterList")
private fun <T : Any, C> EnsureSelectedCellVisibleEffect(
    visibleColumns: List<ColumnSpec<T, C>>,
    rowLeadingPresent: Boolean,
    verticalState: LazyListState,
    horizontalState: ScrollState,
) {
    @Suppress("UNCHECKED_CAST")
    val state = currentTableState() as TableState<C>
    val density = LocalDensity.current
    var previousSelectedRowIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { state.selectedCell }.collectLatest { cell ->
            if (cell == null) return@collectLatest
            val colIndex = visibleColumns.indexOfFirst { it.key == cell.column }
            if (colIndex >= 0) {
                val prevRow = previousSelectedRowIndex
                val movement =
                    if (prevRow != null && cell.rowIndex != prevRow) {
                        if (cell.rowIndex > prevRow) 1 else -1
                    } else {
                        0
                    }
                ensureCellFullyVisible(
                    rowIndex = cell.rowIndex,
                    targetColIndex = colIndex,
                    targetColKey = cell.column,
                    visibleColumns = visibleColumns,
                    state = state,
                    hasLeading = rowLeadingPresent,
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    density = density,
                    movement = movement,
                )
                previousSelectedRowIndex = cell.rowIndex
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun <T : Any, C> GroupStickyOverlay(
    itemAt: (Int) -> T?,
    visibleColumns: List<ColumnSpec<T, C>>,
    customization: TableCustomization<T, C>,
    colors: TableColors,
    width: Dp,
    verticalState: LazyListState,
    horizontalState: ScrollState,
) {
    @Suppress("UNCHECKED_CAST")
    val state = currentTableState() as TableState<C>
    val groupKey = state.groupBy ?: return
    val spec = visibleColumns.firstOrNull { it.key == groupKey } ?: return

    var currentItem by remember { mutableStateOf<T?>(null) }
    var overlayOffsetPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightPx =
        remember(state.dimensions.rowHeight, density) {
            with(density) { state.dimensions.rowHeight.roundToPx() }
        }
    val dividerThicknessPx =
        remember(state.dimensions.dividerThickness, density) {
            with(density) { state.dimensions.dividerThickness.roundToPx() }
        }
    val overlayHeightPx = headerHeightPx + dividerThicknessPx
    // Track first visible item layout to compute push-up effect precisely
    LaunchedEffect(verticalState, state.groupBy) {
        snapshotFlow {
            val firstInfo = verticalState.layoutInfo.visibleItemsInfo.firstOrNull()
            // Pair of index and bottom-on-screen in px
            Pair(firstInfo?.index ?: -1, (firstInfo?.offset ?: 0) + (firstInfo?.size ?: 0))
        }.collectLatest { (index, bottomOnScreenPx) ->
            if (index < 0) return@collectLatest
            currentItem = itemAt(index)

            val currentValue = currentItem?.let { spec.valueOf(it) }
            val nextValue = itemAt(index + 1)?.let { spec.valueOf(it) }
            val isNextDifferent = currentValue != null && nextValue != null && currentValue != nextValue

            if (isNextDifferent) {
                // Include row divider thickness to match the actual next row top
                val bottomWithDivider = bottomOnScreenPx + dividerThicknessPx
                overlayOffsetPx = min(0, bottomWithDivider - overlayHeightPx)
            } else {
                overlayOffsetPx = 0
            }
        }
    }

    currentItem?.let { item ->
        val value = spec.valueOf(item)
        val viewportWidthDp = with(density) { horizontalState.viewportSize.toDp() }
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = overlayOffsetPx.toFloat()
                // Pin horizontally within the viewport by negating current horizontal scroll
                translationX = horizontalState.value.toFloat()
            },
        ) {
            Column {
                GroupHeaderCell(
                    value = value,
                    item = item,
                    spec = spec,
                    width = viewportWidthDp,
                    height = state.dimensions.rowHeight,
                    colors = colors,
                    customization = customization,
                )
                HorizontalDivider(
                    thickness = state.dimensions.dividerThickness,
                    modifier = Modifier.width(viewportWidthDp),
                )
            }
        }
    }
}
