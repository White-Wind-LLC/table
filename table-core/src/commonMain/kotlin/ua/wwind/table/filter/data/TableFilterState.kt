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

/**
 * Determines whether this filter should be treated as active in the UI.
 */
public fun TableFilterState<*>.isActive(): Boolean =
    when (constraint) {
        FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> true
        else -> !values.isNullOrEmpty()
    }
