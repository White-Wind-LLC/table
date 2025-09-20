package ua.wwind.table.format

import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType

public data class FormatFilterData<E : Enum<E>>(
    val field: E,
    val filterType: TableFilterType<*>,
    val filterState: TableFilterState<*>,
    val onChange: (TableFilterState<*>) -> Unit,
)
