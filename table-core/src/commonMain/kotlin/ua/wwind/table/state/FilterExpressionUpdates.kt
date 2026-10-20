package ua.wwind.table.state

import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group

internal fun <C> TableState<C>.updateFilterExpression(transform: (Group<C>) -> Group<C>) {
    filterExpression = transform(filterExpression)
}

/** Removes every top-level child that is not a plain condition. */
internal fun <C> TableState<C>.clearComplexFilters() {
    updateFilterExpression { root -> root.copy(children = root.children.filterIsInstance<Condition<C>>()) }
}
