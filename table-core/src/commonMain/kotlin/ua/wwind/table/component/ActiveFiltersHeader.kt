package ua.wwind.table.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ActiveFilterChips
import ua.wwind.table.ColumnSpec
import ua.wwind.table.TableFilterChipStyle
import ua.wwind.table.component.filterchips.FilterGroupPopover
import ua.wwind.table.component.header.ColumnFilterPanel
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> ActiveFiltersHeader(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    tableData: E,
    strings: StringProvider,
    filterChips: TableFilterChipStyle<C>,
    modifier: Modifier = Modifier,
) {
    ActiveFilterChips(
        columns = columns,
        state = state,
        strings = strings,
        includeClearAllChip = true,
        filterPanel = { spec, filter, onChange, onDismiss ->
            ColumnFilterPanel(spec, state, tableData, strings, onDismiss, filter, onChange)
        },
        groupPopover = { path, keyToSpec, onEdit, onDismiss ->
            FilterGroupPopover(path, state, keyToSpec, tableData, filterChips, strings, onEdit, onDismiss)
        },
        filterChips = filterChips,
        modifier = modifier,
        footer = { HorizontalDivider(color = currentTableColors().dividerColor) },
    )
}
