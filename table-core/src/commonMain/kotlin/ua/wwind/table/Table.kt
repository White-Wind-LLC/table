package ua.wwind.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.component.ActiveFiltersHeader
import ua.wwind.table.component.ContextMenuHost
import ua.wwind.table.component.TableHeader
import ua.wwind.table.component.TableHeaderDefaults
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.component.body.GroupStickyOverlay
import ua.wwind.table.component.body.TableBody
import ua.wwind.table.component.body.TableBodyEmbedded
import ua.wwind.table.component.footer.TableFooter
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.interaction.ApplyAutoWidthEffect
import ua.wwind.table.interaction.ApplyAutoWidthEmbeddedEffect
import ua.wwind.table.interaction.ContextMenuState
import ua.wwind.table.interaction.EnsureSelectedCellVisibleEffect
import ua.wwind.table.interaction.draggableTable
import ua.wwind.table.interaction.tableKeyboardNavigation
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile
import ua.wwind.table.state.LocalTableState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.mapNotNullToImmutable
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.LocalStringProvider
import ua.wwind.table.strings.StringProvider

/**
 * Composable editable data table that renders a header and a virtualized list of rows.
 *
 * - Columns are described by [columns] (`ColumnSpec`).
 * - Data is provided via [itemsCount] and [itemAt] loader.
 * - Sorting, filters, ordering and selection are controlled by [state].
 *
 * Generic parameters:
 * - [T] actual row item type.
 * - [C] column key type.
 * - [E] table data type - shared state accessible in headers, footers, and edit cells.
 *
 * @param itemsCount total number of rows to display
 * @param itemAt loader that returns an item for the given index; may return null while loading
 * @param state mutable table state (sorting, filters, order, selection)
 * @param columns list of visible/available column specifications
 * @param tableData current table data instance - accessible in headers, footers, and edit cells
 * @param modifier layout modifier for the whole table
 * @param placeholderRow optional row content shown when an item is null
 * @param rowKey stable key for rows; defaults to index
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
 * @param border outer border stroke; `null` uses theme default, [TableDefaults.NoBorder] disables border
 */
@Suppress("LongParameterList")
@ExperimentalTableApi
@Composable
public fun <T : Any, C, E> EditableTable(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    tableData: E,
    modifier: Modifier = Modifier,
    placeholderRow: (@Composable () -> Unit)? = null,
    rowKey: (item: T?, index: Int) -> Any = { _, i -> i },
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
    border: BorderStroke? = null,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)? = null,
    embedded: Boolean = false,
    /** Callback when row editing starts. Receives non-null item and row index. */
    onRowEditStart: ((item: T, rowIndex: Int) -> Unit)? = null,
    /**
     * Callback to validate row edit completion. Returns true to allow exit, false to stay in
     * edit mode.
     */
    onRowEditComplete: ((rowIndex: Int) -> Boolean)? = null,
    /** Callback when editing is cancelled */
    onEditCancelled: ((rowIndex: Int) -> Unit)? = null,
) {
    val dimensions = state.dimensions
    val visibleColumns by
        remember(columns, state.columnOrder) {
            derivedStateOf {
                state.columnOrder.mapNotNullToImmutable { key ->
                    columns.find { it.key == key && it.visible }
                }
            }
        }

    // Update visible columns in state for tableWidth calculation
    state.visibleColumns = visibleColumns

    var contextMenuState by remember { mutableStateOf(ContextMenuState<T>()) }

    val tableFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Consume drag and fling deltas so parent containers don't scroll while dragging inside the
    // table
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
    LaunchedEffect(itemsCount) { state.rowHeightsPx.clear() }
    LaunchedEffect(state.sort) {
        if (verticalState.canScrollBackward) verticalState.scrollToItem(0)
    }

    // Set edit mode callbacks
    LaunchedEffect(state, onRowEditStart, onRowEditComplete, onEditCancelled) {
        state.setEditCallbacks(
            onStart =
                onRowEditStart?.let { callback ->
                    { rowIndex: Int ->
                        // Item is guaranteed to be non-null at this point (verified in startEditing)
                        val item = itemAt(rowIndex)
                        if (item != null) {
                            callback(item, rowIndex)
                        }
                    }
                },
            onComplete = onRowEditComplete,
            onCancel = onEditCancelled,
        )
    }

    CompositionLocalProvider(
        LocalTableState provides state,
        LocalStringProvider provides strings,
    ) {
        // Ensure selected cell is fully visible whenever it changes (including external API calls)
        EnsureSelectedCellVisibleEffect(
            visibleColumns = visibleColumns,
            verticalState = verticalState,
            horizontalState = horizontalState,
        )
        val enableScrolling = remember { !getPlatform().isMobile() && !embedded }

        // Outer container with border and shape - always maintains visual form
        Surface(
            shape = shape,
            border =
                when {
                    border == TableDefaults.NoBorder -> {
                        null
                    }

                    border != null -> {
                        border
                    }

                    else -> {
                        BorderStroke(
                            dimensions.dividerThickness,
                            MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                },
            modifier = modifier,
        ) {
            // Inner content with clipping and interactions
            val innerModifier =
                Modifier
                    .then(
                        if (embedded) {
                            Modifier
                        } else {
                            Modifier.draggableTable(
                                horizontalState = horizontalState,
                                verticalState = verticalState,
                                blockParentScrollConnection = blockParentScrollConnection,
                                nestedScrollDispatcher = nestedScrollDispatcher,
                                enableScrolling = enableScrolling,
                                enableDragToScroll = state.settings.enableDragToScroll,
                                coroutineScope = coroutineScope,
                            )
                        },
                    ).clipToBounds()
                    .tableKeyboardNavigation(
                        focusRequester = tableFocusRequester,
                        itemsCount = itemsCount,
                        state = state,
                        visibleColumns = visibleColumns,
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                    )

            Column(
                modifier = innerModifier,
            ) {
                if (state.settings.showActiveFiltersHeader) {
                    ActiveFiltersHeader(
                        columns = columns,
                        state = state,
                        strings = strings,
                    )
                }

                TableHeader(
                    columns = columns,
                    state = state,
                    tableData = tableData,
                    headerColor = colors.headerContainerColor,
                    headerContentColor = colors.headerContentColor,
                    rowContainerColor = colors.rowContainerColor,
                    dimensions = dimensions,
                    strings = strings,
                    icons = icons,
                    horizontalState = horizontalState,
                )
                HorizontalDivider(modifier = Modifier.width(state.tableWidth))

                val bodyContent: @Composable () -> Unit = {
                    Box(if (embedded) Modifier else Modifier.weight(1f, false)) {
                        if (embedded) {
                            TableBodyEmbedded(
                                itemsCount = itemsCount,
                                itemAt = itemAt,
                                rowKey = rowKey,
                                visibleColumns = visibleColumns,
                                state = state,
                                colors = colors,
                                customization = customization,
                                tableData = tableData,
                                rowEmbedded = rowEmbedded,
                                placeholderRow = placeholderRow,
                                onRowClick = onRowClick,
                                onRowLongClick = onRowLongClick,
                                onContextMenu =
                                    contextMenu?.let {
                                        { item: T, pos: Offset ->
                                            contextMenuState =
                                                contextMenuState.copy(
                                                    visible = true,
                                                    position = pos,
                                                    item = item,
                                                )
                                        }
                                    },
                                horizontalState = horizontalState,
                                requestTableFocus = { tableFocusRequester.requestFocus() },
                            )
                        } else {
                            TableBody(
                                itemsCount = itemsCount,
                                itemAt = itemAt,
                                rowKey = rowKey,
                                visibleColumns = visibleColumns,
                                state = state,
                                colors = colors,
                                customization = customization,
                                tableData = tableData,
                                placeholderRow = placeholderRow,
                                onRowClick = onRowClick,
                                onRowLongClick = onRowLongClick,
                                onContextMenu =
                                    contextMenu?.let {
                                        { item: T, pos: Offset ->
                                            contextMenuState =
                                                contextMenuState.copy(
                                                    visible = true,
                                                    position = pos,
                                                    item = item,
                                                )
                                        }
                                    },
                                rowEmbedded = rowEmbedded,
                                verticalState = verticalState,
                                horizontalState = horizontalState,
                                requestTableFocus = { tableFocusRequester.requestFocus() },
                                enableScrolling = enableScrolling,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (state.groupBy != null) {
                            GroupStickyOverlay(
                                itemAt = itemAt,
                                tableData = tableData,
                                visibleColumns = visibleColumns,
                                customization = customization,
                                colors = colors,
                                verticalState = verticalState,
                                horizontalState = horizontalState,
                            )
                        }
                    }
                }

                if (state.settings.enableTextSelection) {
                    SelectionContainer { bodyContent() }
                } else {
                    bodyContent()
                }

                // Footer rendering
                if (state.settings.showFooter) {
                    // If footer is pinned and table is not embedded, it's rendered outside the Box
                    // Otherwise, it's rendered as part of the TableBody scroll
                    if (!embedded && state.settings.footerPinned) {
                        HorizontalDivider(modifier = Modifier.width(state.tableWidth))
                        TableFooter(
                            visibleColumns = visibleColumns,
                            widthResolver = { key ->
                                val spec = columns.firstOrNull { it.key == key }
                                state.resolveColumnWidth(key, spec)
                            },
                            tableData = tableData,
                            footerColor = colors.footerContainerColor,
                            footerContentColor = colors.footerContentColor,
                            dimensions = dimensions,
                            horizontalState = horizontalState,
                            tableWidth = state.tableWidth,
                            pinnedColumnsCount = state.settings.pinnedColumnsCount,
                            pinnedColumnsSide = state.settings.pinnedColumnsSide,
                        )
                    }
                }
            }
        }

        ContextMenuHost(
            contextMenuState = contextMenuState,
            contextMenu = contextMenu,
            onDismiss = { contextMenuState = contextMenuState.copy(visible = false) },
        )

        // Apply auto-width effect based on table mode
        if (embedded) {
            ApplyAutoWidthEmbeddedEffect(visibleColumns, itemsCount, state)
        } else {
            ApplyAutoWidthEffect(visibleColumns, itemsCount, verticalState, state)
        }
    }
}

/**
 * Composable read-only data table that renders a header and a virtualized list of rows.
 *
 * This is a convenience wrapper around [EditableTable] for tables without editing support.
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
 * @param border outer border stroke; `null` uses theme default, [TableDefaults.NoBorder] disables border
 */
@Suppress("LongParameterList")
@ExperimentalTableApi
@Composable
public fun <T : Any, C> Table(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    columns: ImmutableList<ColumnSpec<T, C, Unit>>,
    modifier: Modifier = Modifier,
    placeholderRow: (@Composable () -> Unit)? = null,
    rowKey: (item: T?, index: Int) -> Any = { _, i -> i },
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
    border: BorderStroke? = null,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)? = null,
    embedded: Boolean = false,
) {
    EditableTable(
        itemsCount = itemsCount,
        itemAt = itemAt,
        state = state,
        columns = columns,
        tableData = Unit,
        modifier = modifier,
        placeholderRow = placeholderRow,
        rowKey = rowKey,
        onRowClick = onRowClick,
        onRowLongClick = onRowLongClick,
        contextMenu = contextMenu,
        customization = customization,
        colors = colors,
        strings = strings,
        verticalState = verticalState,
        horizontalState = horizontalState,
        icons = icons,
        shape = shape,
        border = border,
        rowEmbedded = rowEmbedded,
        embedded = embedded,
        onRowEditStart = null,
        onRowEditComplete = null,
        onEditCancelled = null,
    )
}

/**
 * Composable data table with custom table data state.
 *
 * This overload allows passing custom table data that will be accessible in headers, footers, and edit cells.
 *
 * - Columns are described by [columns] (`ColumnSpec`).
 * - Data is provided via [itemsCount] and [itemAt] loader.
 * - Sorting, filters, ordering and selection are controlled by [state].
 *
 * Generic parameters:
 * - [T] actual row item type.
 * - [C] column key type.
 * - [E] table data type - shared state accessible in headers, footers, and edit cells.
 *
 * @param itemsCount total number of rows to display
 * @param itemAt loader that returns an item for the given index; may return null while loading
 * @param state mutable table state (sorting, filters, order, selection)
 * @param columns list of visible/available column specifications
 * @param tableData current table data instance - accessible in headers, footers, and edit cells
 * @param modifier layout modifier for the whole table
 * @param placeholderRow optional row content shown when an item is null
 * @param rowKey stable key for rows; defaults to index
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
 * @param border outer border stroke; `null` uses theme default, [TableDefaults.NoBorder] disables border
 */
@Suppress("LongParameterList")
@ExperimentalTableApi
@Composable
public fun <T : Any, C, E> Table(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    tableData: E,
    modifier: Modifier = Modifier,
    placeholderRow: (@Composable () -> Unit)? = null,
    rowKey: (item: T?, index: Int) -> Any = { _, i -> i },
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
    border: BorderStroke? = null,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)? = null,
    embedded: Boolean = false,
) {
    EditableTable(
        itemsCount = itemsCount,
        itemAt = itemAt,
        state = state,
        columns = columns,
        tableData = tableData,
        modifier = modifier,
        placeholderRow = placeholderRow,
        rowKey = rowKey,
        onRowClick = onRowClick,
        onRowLongClick = onRowLongClick,
        contextMenu = contextMenu,
        customization = customization,
        colors = colors,
        strings = strings,
        verticalState = verticalState,
        horizontalState = horizontalState,
        icons = icons,
        shape = shape,
        border = border,
        rowEmbedded = rowEmbedded,
        embedded = embedded,
        onRowEditStart = null,
        onRowEditComplete = null,
        onEditCancelled = null,
    )
}
