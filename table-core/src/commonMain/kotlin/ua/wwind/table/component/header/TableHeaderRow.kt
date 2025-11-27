package ua.wwind.table.component.header

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculateFixedColumnState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C, E> TableHeaderRow(
    lazyListState: LazyListState,
    reorderState: ReorderableLazyListState,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    style: TableHeaderStyle,
    state: TableState<C>,
    strings: StringProvider,
    filterColumn: C?,
    onFilterColumnChange: (C?) -> Unit,
    isResizing: Boolean,
    horizontalState: ScrollState,
) {
    val settings = state.settings

    LazyRow(
        modifier = Modifier.width(state.tableWidth),
        state = lazyListState,
        userScrollEnabled = false,
    ) {
        items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
            val index = visibleColumns.indexOf(spec)

            val fixedState =
                calculateFixedColumnState(
                    columnIndex = index,
                    totalVisibleColumns = visibleColumns.size,
                    fixedColumnsCount = settings.fixedColumnsCount,
                    fixedColumnsSide = settings.fixedColumnsSide,
                    horizontalState = horizontalState,
                )

            ReorderableItem(
                state = reorderState,
                key = spec.key as Any,
                animateItemModifier = if (isResizing) Modifier else Modifier.animateItem(),
                enabled = !fixedState.isFixed,
                modifier =
                    Modifier
                        .zIndex(fixedState.zIndex)
                        .graphicsLayer {
                            this.translationX = fixedState.translationX
                        },
            ) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp).value

                Surface(
                    color = style.headerColor,
                    contentColor = style.headerContentColor,
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    modifier =
                        Modifier
                            .draggableHandle(enabled = !fixedState.isFixed),
                ) {
                    val width = widthResolver(spec.key)

                    ColumnHeaderDropdownMenuBox(
                        spec = spec,
                        state = state,
                    ) {
                        val dividerThickness =
                            if (fixedState.isLastLeftFixed) {
                                style.dimensions.fixedColumnDividerThickness
                            } else {
                                style.dimensions.dividerThickness
                            }

                        HeaderCell(
                            spec = spec,
                            state = state,
                            strings = strings,
                            width = width,
                            dividerThickness = dividerThickness,
                            isFilterOpen = filterColumn == spec.key,
                            onOpenFilter = { onFilterColumnChange(spec.key) },
                            onDismissFilter = { onFilterColumnChange(null) },
                            onToggleSort = { state.setSort(spec.key) },
                            showLeftDivider = fixedState.isFirstRightFixed,
                            leftDividerThickness = style.dimensions.fixedColumnDividerThickness,
                            showRightDivider = !fixedState.isLastBeforeRightFixed,
                        )
                    }
                }
            }
        }
    }
}
