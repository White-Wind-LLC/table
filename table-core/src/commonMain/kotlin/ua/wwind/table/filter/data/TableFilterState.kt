package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

/**
 * Current UI state for a single column filter: selected [constraint] and optional [values].
 */
@Immutable
public data class TableFilterState<T>(
    val constraint: FilterConstraint?,
    val values: List<T>?,
)
