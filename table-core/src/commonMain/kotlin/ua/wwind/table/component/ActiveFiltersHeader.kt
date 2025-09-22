package ua.wwind.table.component

import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.TableActiveFilters
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> ActiveFiltersHeader(
    columns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    strings: StringProvider,
    width: Dp,
) {
    TableActiveFilters(
        columns = columns,
        state = state,
        strings = strings,
        modifier = Modifier.width(width),
        includeClearAllChip = true,
    )
    HorizontalDivider(modifier = Modifier.width(width))
}
