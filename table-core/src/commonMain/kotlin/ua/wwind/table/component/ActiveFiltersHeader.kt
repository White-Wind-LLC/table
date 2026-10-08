package ua.wwind.table.component

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ActiveFilterChips
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.header.ColumnFilterPanel
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C, E> ActiveFiltersHeader(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    tableData: E,
    strings: StringProvider,
    modifier: Modifier = Modifier,
) {
    ActiveFilterChips(
        columns = columns,
        state = state,
        strings = strings,
        includeClearAllChip = true,
        filterPanel = { spec, onDismiss -> ColumnFilterPanel(spec, state, tableData, strings, onDismiss) },
        modifier = modifier,
        footer = { HorizontalDivider(color = currentTableColors().dividerColor) },
    )
}
