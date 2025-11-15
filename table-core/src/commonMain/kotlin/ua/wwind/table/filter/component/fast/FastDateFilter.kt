package ua.wwind.table.filter.component.fast

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> FastDateFilter(
    spec: ColumnSpec<T, C>,
    state: TableFilterState<LocalDate>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
) {
    // Date filter is not yet implemented in core. Provide no-op for now.
}
