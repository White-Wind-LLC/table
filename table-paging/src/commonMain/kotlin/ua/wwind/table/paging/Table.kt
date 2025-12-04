package ua.wwind.table.paging

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.getOrNull
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.component.TableHeaderDefaults
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider

/**
 * Composable data table with paging support.
 *
 * This overload wraps [PagingData] and delegates to the core [Table] composable.
 *
 * - Columns are described by [columns] (`ColumnSpec`).
 * - Data is provided via [items] (`PagingData`).
 * - Sorting, filters, ordering and selection are controlled by [state].
 *
 * Generic parameters:
 * - [T] actual row item type.
 * - [C] column key type.
 * - [E] table data type - shared state accessible in headers, footers, and edit cells.
 *
 * @param items paging data container with loaded items
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
 */
@OptIn(ExperimentalTableApi::class)
@Composable
@Suppress("LongParameterList", "ktlint:standard:function-naming")
public fun <T : Any, C, E> Table(
    items: PagingData<T>?,
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
) {
    val itemsCount = remember(items) { items?.data?.size ?: 0 }
    val itemAt = remember(items) { { index: Int -> items?.data?.get(index)?.getOrNull() } }

    Table(
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
    )
}

@OptIn(ExperimentalTableApi::class)
@Composable
@Suppress("LongParameterList", "ktlint:standard:function-naming")
public fun <T : Any, C> Table(
    items: PagingData<T>?,
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
) {
    val itemsCount = remember(items) { items?.data?.size ?: 0 }
    val itemAt = remember(items) { { index: Int -> items?.data?.get(index)?.getOrNull() } }

    Table(
        itemsCount = itemsCount,
        itemAt = itemAt,
        state = state,
        columns = columns,
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
    )
}
