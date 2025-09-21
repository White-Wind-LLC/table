package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

@Immutable
/** Logical combinators used to build nested filter expressions. */
public enum class TableCombinator {
    AND,
    OR,
    NOT,
}

@Immutable
/** Base sealed type for a single-column filter predicate. */
public sealed class TableFilterPredicate<T>

@Immutable
/** Single-value predicate, e.g. EQUALS value or CONTAINS value. */
public data class TableSinglePredicate<T>(
    val op: FilterConstraint,
    val value: T?,
) : TableFilterPredicate<T>()

@Immutable
/** Multi-value predicate, e.g. IN/NOT_IN or BETWEEN. */
public data class TableMultiplePredicate<T>(
    val op: FilterConstraint,
    val values: List<T>?,
) : TableFilterPredicate<T>()

@Immutable
/** Set of predicates targeting a single [column]. */
public data class ColumnFilter<E : Enum<E>, T>(
    val column: E,
    val predicates: List<TableFilterPredicate<T>>,
)

@Immutable
/**
 * Recursive filter expression consisting of [filters] and [children] combined by [combinator].
 */
public data class TableFilterExpression<E : Enum<E>>(
    val combinator: TableCombinator? = null,
    val filters: List<ColumnFilter<E, *>>? = null,
    val children: List<TableFilterExpression<E>>? = null,
)
