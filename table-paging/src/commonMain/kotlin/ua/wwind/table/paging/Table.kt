package ua.wwind.table.paging

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

@OptIn(ExperimentalTableApi::class)
@Composable
@Suppress("LongParameterList", "ktlint:standard:function-naming")
public fun <T : Any, C> Table(
    items: PagingData<T>?,
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
        rowLeading = rowLeading,
        rowTrailing = rowTrailing,
        onRowClick = onRowClick,
        onRowLongClick = onRowLongClick,
        contextMenu = contextMenu,
        customization = customization,
        colors = colors,
        strings = strings,
        verticalState = verticalState,
        horizontalState = horizontalState,
        icons = icons,
    )
}
