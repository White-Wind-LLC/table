package ua.wwind.table.component.header

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> TableHeaderRow(
    tableWidth: Dp,
    leadingColumnWidth: Dp?,
    lazyListState: LazyListState,
    reorderState: ReorderableLazyListState,
    visibleColumns: List<ColumnSpec<T, C>>,
    widthResolver: (C) -> Dp,
    style: TableHeaderStyle,
    state: TableState<C>,
    strings: StringProvider,
    filterColumn: C?,
    onFilterColumnChange: (C?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.width(tableWidth),
        state = lazyListState,
        userScrollEnabled = false,
    ) {
        if (leadingColumnWidth != null) {
            item(key = "leading") {
                Row {
                    Spacer(
                        modifier =
                            Modifier
                                .width(leadingColumnWidth)
                                .height(style.dimensions.defaultRowHeight),
                    )
                    VerticalDivider(
                        modifier = Modifier.height(style.dimensions.defaultRowHeight),
                        thickness = style.dimensions.verticalDividerThickness,
                    )
                }
            }
        }

        items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
            ReorderableItem(reorderState, key = spec.key as Any) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp).value
                Surface(
                    color = style.headerColor,
                    contentColor = style.headerContentColor,
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    modifier = Modifier.draggableHandle(),
                ) {
                    val width = widthResolver(spec.key) + style.dimensions.verticalDividerThickness
                    val clickableModifier =
                        if (spec.sortable && spec.headerClickToSort) {
                            Modifier.clickable { state.setSort(spec.key) }
                        } else {
                            Modifier
                        }
                    Box(modifier = clickableModifier) {
                        HeaderCell(
                            spec = spec,
                            state = state,
                            strings = strings,
                            width = width,
                            dimensions = style.dimensions,
                            isFilterOpen = filterColumn == spec.key,
                            onOpenFilter = { onFilterColumnChange(spec.key) },
                            onDismissFilter = { onFilterColumnChange(null) },
                            onToggleSort = { state.setSort(spec.key) },
                        )
                    }
                }
            }
        }
    }
}
