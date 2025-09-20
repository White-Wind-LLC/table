package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

@Immutable
public enum class TableCombinator { AND, OR, NOT }

@Immutable
public sealed class TableFilterPredicate<T>

@Immutable
public data class TableSinglePredicate<T>(
    val op: FilterConstraint,
    val value: T?,
) : TableFilterPredicate<T>()

@Immutable
public data class TableMultiplePredicate<T>(
    val op: FilterConstraint,
    val values: List<T>?,
) : TableFilterPredicate<T>()

@Immutable
public data class ColumnFilter<E : Enum<E>, T>(
    val column: E,
    val predicates: List<TableFilterPredicate<T>>,
)

@Immutable
public data class TableFilterExpression<E : Enum<E>>(
    val combinator: TableCombinator? = null,
    val filters: List<ColumnFilter<E, *>>? = null,
    val children: List<TableFilterExpression<E>>? = null,
)
