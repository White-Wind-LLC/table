package ua.wwind.table.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.header.ColumnResizersOverlay
import ua.wwind.table.component.header.TableHeaderRow
import ua.wwind.table.component.header.TableHeaderStyle
import ua.wwind.table.component.header.computeReorderMove
import ua.wwind.table.component.header.rememberHeaderDerivedState
import ua.wwind.table.config.LocalTableTypography
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.filter.component.fast.FastFiltersRow
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.interaction.ensureColumnFullyVisible
import ua.wwind.table.interaction.tableHeaderKeyboardNavigation
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> TableHeader(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    tableData: E,
    headerColor: Color,
    headerContentColor: Color,
    rowContainerColor: Color,
    dimensions: TableDimensions,
    strings: StringProvider,
    horizontalState: ScrollState,
    onEnterBody: () -> Unit,
    icons: TableHeaderIcons =
        TableHeaderIcons(
            sortAsc = TableIcons.ArrowUpward,
            sortDesc = TableIcons.ArrowDownward,
            sortNeutral = TableIcons.Sort,
            filterActive = TableIcons.FilterAltFilled,
            filterInactive = TableIcons.FilterAltOutlined,
        ),
) {
    val lazyListState = remember { LazyListState() }
    var filterColumn by remember { mutableStateOf<C?>(null) }
    var restoreFocusAfterFilter by remember { mutableStateOf(false) }
    val derived = rememberHeaderDerivedState(columns, state, dimensions)
    var isResizing by remember { mutableStateOf(false) }
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fullOrder = state.columns.order.toList()
            val visibleKeys = derived.visibleColumns.map { it.key }
            val move = computeReorderMove(from.index, to.index, fullOrder, visibleKeys)
            if (move != null) state.columns.move(move.first, move.second)
        }

    // Keep the keyboard-focused header column scrolled into view. A pointer focuses a column already on screen.
    val density = LocalDensity.current
    LaunchedEffect(state, derived.visibleColumns) {
        snapshotFlow {
            if (state.isHeaderFocused && state.isHeaderFocusFromKeyboard) state.focusedHeaderColumn else null
        }.collectLatest { column ->
            val index = derived.visibleColumns.indexOfFirst { it.key == column }
            if (column != null && index >= 0) {
                ensureColumnFullyVisible(index, column, derived.visibleColumns, state, horizontalState, density)
            }
        }
    }

    // A stable resolver that reads the current widths when a header cell asks, so a resize drag
    // invalidates the cells' width reads instead of re-creating (and recomposing) every header.
    val widthMap by rememberUpdatedState(derived.widthMap)
    val headerWidthResolver: (
        C,
    ) -> Dp = remember(dimensions) { { key -> widthMap[key] ?: dimensions.defaultColumnWidth } }

    Column {
        Surface(color = headerColor, contentColor = headerContentColor) {
            CompositionLocalProvider(
                LocalTableHeaderIcons provides icons,
                LocalTextStyle provides LocalTextStyle.current.merge(LocalTableTypography.current.header),
            ) {
                Box(
                    Modifier
                        .height(state.dimensions.headerHeight)
                        .tableHeaderKeyboardNavigation(state, derived.visibleColumns, onEnterBody),
                ) {
                    TableHeaderRow(
                        lazyListState = lazyListState,
                        reorderState = reorderState,
                        visibleColumns = derived.visibleColumns,
                        widthResolver = headerWidthResolver,
                        style = TableHeaderStyle(headerColor, headerContentColor, dimensions, icons),
                        state = state,
                        tableData = tableData,
                        strings = strings,
                        filterColumn = filterColumn,
                        onFilterColumnChange = { column ->
                            filterColumn = column
                            if (column == null && restoreFocusAfterFilter) {
                                restoreFocusAfterFilter = false
                                state.headerFocusRequester.requestFocus()
                            }
                        },
                        onOpenFilterFromMenu = { key ->
                            filterColumn = key
                            restoreFocusAfterFilter = state.lastColumnMenuFromKeyboard
                        },
                        isResizing = isResizing,
                        horizontalState = horizontalState,
                    )

                    ColumnResizersOverlay(
                        visibleColumns = derived.visibleColumns,
                        widthResolver = { key -> derived.widthMap[key] ?: dimensions.defaultColumnWidth },
                        dimensions = dimensions,
                        horizontalState = horizontalState,
                        onResize = { key, newWidth -> state.columns.resize(key, ColumnWidthAction.Set(newWidth)) },
                        onResizeStart = { isResizing = true },
                        onResizeEnd = { isResizing = false },
                        onDoubleClick = { key -> state.columns.fitToContent(key) },
                    )
                }
            }
        }
        if (state.settings.showHeaderDivider) {
            HorizontalDivider(modifier = Modifier.width(state.tableWidth), color = currentTableColors().dividerColor)
        }
        AnimatedVisibility(
            visible =
                state.settings.showFastFilters &&
                    derived.visibleColumns.any {
                        it.filter != null && it.filter !is TableFilterType.DisabledTableFilter
                    },
            enter =
                slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight },
                ) + fadeIn(),
            exit =
                slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight },
                ) + fadeOut(),
        ) {
            FastFiltersRow(
                visibleColumns = derived.visibleColumns,
                widthResolver = { key -> derived.widthMap[key] ?: dimensions.defaultColumnWidth },
                rowContainerColor = rowContainerColor,
                state = state,
                tableData = tableData,
                strings = strings,
                onChange = { spec, newState -> state.setFilter(spec.key, newState) },
                horizontalState = horizontalState,
            )
        }
    }
}
