package ua.wwind.table.filter

import androidx.compose.runtime.Composable
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider

@Suppress("LongParameterList", "UnusedParameter")
@Composable
internal fun DateFilter(
    filter: TableFilterType.DateTableFilter,
    state: TableFilterState<kotlinx.datetime.LocalDate>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<kotlinx.datetime.LocalDate>) -> Unit,
) {
    // Date filter is not yet implemented in core. Provide no-op for now.
}
