package ua.wwind.table.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.header.ColumnResizersOverlay
import ua.wwind.table.component.header.TableHeaderRow
import ua.wwind.table.component.header.TableHeaderStyle
import ua.wwind.table.component.header.computeReorderMove
import ua.wwind.table.component.header.rememberHeaderDerivedState
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C> TableHeader(
    columns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    tableWidth: Dp,
    headerColor: Color,
    headerContentColor: Color,
    dimensions: TableDimensions,
    strings: StringProvider,
    leadingColumnWidth: Dp? = null,
    icons: TableHeaderIcons =
        TableHeaderIcons(
            sortAsc = Icons.Rounded.ArrowUpward,
            sortDesc = Icons.Rounded.ArrowDownward,
            sortNeutral = Icons.AutoMirrored.Outlined.Sort,
            filterActive = Icons.Rounded.FilterAlt,
            filterInactive = Icons.Outlined.FilterAlt,
        ),
) {
    val lazyListState = remember { LazyListState() }
    var filterColumn by remember { mutableStateOf<C?>(null) }
    val derived = rememberHeaderDerivedState(columns, state, dimensions)
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val leadingOffset = if (leadingColumnWidth != null) 1 else 0
            val fullOrder = state.columnOrder.toList()
            val visibleKeys = derived.visibleColumns.map { it.key }
            val move = computeReorderMove(from.index, to.index, leadingOffset, fullOrder, visibleKeys)
            if (move != null) state.moveColumn(move.first, move.second)
        }

    Surface(color = headerColor, contentColor = headerContentColor) {
        CompositionLocalProvider(LocalTableHeaderIcons provides icons) {
            Box {
                TableHeaderRow(
                    tableWidth = tableWidth,
                    leadingColumnWidth = leadingColumnWidth,
                    lazyListState = lazyListState,
                    reorderState = reorderState,
                    visibleColumns = derived.visibleColumns,
                    widthResolver = { key -> derived.widthMap[key] ?: dimensions.defaultColumnWidth },
                    style = TableHeaderStyle(headerColor, headerContentColor, dimensions, icons),
                    state = state,
                    strings = strings,
                    filterColumn = filterColumn,
                    onFilterColumnChange = { filterColumn = it },
                )

                ColumnResizersOverlay(
                    tableWidth = tableWidth,
                    visibleColumns = derived.visibleColumns,
                    widthResolver = { key -> derived.widthMap[key] ?: dimensions.defaultColumnWidth },
                    dimensions = dimensions,
                    leadingColumnWidth = leadingColumnWidth,
                    onResize = { key, newWidth -> state.resizeColumn(key, ColumnWidthAction.Set(newWidth)) },
                )
            }
        }
    }
}
