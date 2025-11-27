package ua.wwind.table.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.TableActiveFilters
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C, E> ActiveFiltersHeader(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    strings: StringProvider,
) {
    TableActiveFilters(
        columns = columns,
        state = state,
        strings = strings,
        modifier = Modifier.width(state.tableWidth),
        includeClearAllChip = true,
    )
    HorizontalDivider(modifier = Modifier.width(state.tableWidth))
}
