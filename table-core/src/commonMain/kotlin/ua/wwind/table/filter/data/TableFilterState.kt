package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

@Immutable
public data class TableFilterState<T>(
    val constraint: FilterConstraint?,
    val values: List<T>?,
)
