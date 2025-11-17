package ua.wwind.table.format

import androidx.compose.runtime.Immutable
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType

@Immutable
public data class FormatFilterData<E : Enum<E>>(
    val field: E,
    val filterType: TableFilterType<*>,
    val filterState: TableFilterState<*>,
    val onChange: (TableFilterState<*>) -> Unit,
)
