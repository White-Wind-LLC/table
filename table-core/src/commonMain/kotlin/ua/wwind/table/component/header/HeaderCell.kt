package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.MeasureCellMinWidth
import ua.wwind.table.component.LocalTableHeaderCellInfo
import ua.wwind.table.component.LocalTableHeaderIcons
import ua.wwind.table.component.TableHeaderCellInfo
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.component.FilterPanel
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> HeaderCell(
    spec: ColumnSpec<T, C>,
    state: TableState<C>,
    strings: StringProvider,
    width: Dp,
    dimensions: TableDimensions,
    isFilterOpen: Boolean,
    onOpenFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onToggleSort: () -> Unit,
) {
    val sortOrder: SortOrder? = state.sort?.takeIf { it.column == spec.key }?.order
    val isFilterActive: Boolean = state.filters[spec.key]?.values?.isEmpty() == false

    val info =
        TableHeaderCellInfo(
            columnKey = spec.key as Any?,
            isSortable = spec.sortable,
            sortOrder = sortOrder,
            hasFilter = spec.filter != null,
            isFilterActive = isFilterActive,
            toggleSort = onToggleSort,
            sortIcon = {
                SortButton(
                    enabled = spec.sortable,
                    order = sortOrder,
                    icons = LocalTableHeaderIcons.current,
                    onToggle = onToggleSort,
                    clickable = !spec.headerClickToSort,
                )
            },
            filterIcon = {
                FilterButton(
                    enabled = spec.filter != null,
                    active = isFilterActive,
                    icons = LocalTableHeaderIcons.current,
                    isOpen = isFilterOpen,
                    onOpen = onOpenFilter,
                    onDismiss = onDismissFilter,
                ) {
                    @Suppress("UNCHECKED_CAST")
                    FilterPanel(
                        type = spec.filter as? TableFilterType<Any?>,
                        state = state.filters[spec.key] as? TableFilterState<Any?>,
                        expanded = true,
                        onDismissRequest = onDismissFilter,
                        strings = strings,
                        autoApplyFilters = state.settings.autoApplyFilters,
                        autoFilterDebounce = state.settings.autoFilterDebounce,
                        onChange = { newState ->
                            state.setFilter(spec.key, newState)
                        },
                    )
                }
            },
        )

    // Measure header content minimal width to contribute to max content width for resizable or auto-width columns
    if (spec.resizable || spec.autoWidth) {
        MeasureCellMinWidth(
            item = Unit,
            measureKey = Pair(spec.key, "header"),
            content = { _ ->
                HeaderContent(spec, info)
            },
        ) { measuredMinWidth ->
            val adjusted = maxOf(measuredMinWidth, spec.minWidth)
            state.updateMaxContentWidth(spec.key, adjusted)
        }
    }

    Box(
        modifier = Modifier.width(width).height(dimensions.defaultRowHeight),
        contentAlignment = Alignment.Center,
    ) {
        HeaderContent(spec, info)
        if (spec.headerDecorations) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            ) {
                info.filterIcon.invoke()
            }
        }
        VerticalDivider(
            modifier = Modifier.align(Alignment.CenterEnd).height(dimensions.defaultRowHeight),
            thickness = dimensions.verticalDividerThickness,
        )
    }
}

@Composable
private fun HeaderContent(
    spec: ColumnSpec<*, *>,
    info: TableHeaderCellInfo<Any?>,
) {
    Row(
        modifier = if (spec.headerDecorations) Modifier.padding(horizontal = 16.dp) else Modifier,
    ) {
        CompositionLocalProvider(
            LocalTableHeaderCellInfo provides (info as TableHeaderCellInfo<Any?>),
        ) {
            spec.header()
        }
        if (spec.headerDecorations) {
            info.sortIcon.invoke()
        }
    }
}
