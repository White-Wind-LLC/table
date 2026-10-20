package ua.wwind.table.component.filterchips

import ua.wwind.table.filter.data.FilterPath
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isActive

internal sealed interface FilterChipItem<C> {
    val path: FilterPath

    data class Condition<C>(
        override val path: FilterPath,
        val column: C,
        val state: TableFilterState<*>,
    ) : FilterChipItem<C>

    /** A condition whose column is not known to the table. */
    data class Unknown<C>(
        override val path: FilterPath,
        val column: C,
        val state: TableFilterState<*>,
    ) : FilterChipItem<C>

    data class Group<C>(
        override val path: FilterPath,
        val summary: FilterGroupSummary<C>,
    ) : FilterChipItem<C>
}

internal data class FilterGroupSummary<C>(
    val operator: Operator,
    val negated: Boolean,
    /** Active direct children of the group. */
    val activeCount: Int,
    /** Distinct columns of active leaves, in tree order. */
    val columns: List<C>,
    /** Set only for a non-negated OR whose children are all active conditions on one column. */
    val sharedColumn: C?,
)

/** Top-level items of [root]; inactive nodes are skipped except the one at [keepPath] (the open chip). */
internal fun <C> rowItems(
    root: Group<C>,
    isKnown: (C) -> Boolean,
    keepPath: FilterPath?,
): List<FilterChipItem<C>> =
    root.children.mapIndexedNotNull { index, node ->
        val path = listOf(index)
        if (!node.isActive() && path != keepPath) return@mapIndexedNotNull null
        when (node) {
            is Condition -> {
                if (isKnown(node.column)) {
                    FilterChipItem.Condition(path, node.column, node.state)
                } else {
                    FilterChipItem.Unknown(path, node.column, node.state)
                }
            }

            is Group, is Not -> {
                FilterChipItem.Group(path, node.summary())
            }
        }
    }

/** Receiver is a [Group], or a [Not] wrapping a group or a condition. */
internal fun <C> TableFilterNode<C>.summary(): FilterGroupSummary<C> {
    var inner: TableFilterNode<C> = this
    var negated = false
    while (inner is Not) {
        negated = !negated
        inner = inner.child
    }
    val columns = LinkedHashSet<C>().also { inner.collectActiveColumns(it) }.toList()
    return when (val node = inner) {
        is Group -> {
            val shared =
                if (!negated && node.operator == Operator.OR) {
                    val leaves = node.children.map { it as? Condition }
                    leaves
                        .firstOrNull()
                        ?.column
                        ?.takeIf { col -> leaves.all { it != null && it.state.isActive() && it.column == col } }
                } else {
                    null
                }
            FilterGroupSummary(node.operator, negated, node.children.count { it.isActive() }, columns, shared)
        }

        else -> {
            FilterGroupSummary(Operator.AND, negated, 1, columns, null)
        }
    }
}

private fun <C> TableFilterNode<C>.collectActiveColumns(into: MutableSet<C>) {
    when (this) {
        is Condition -> if (state.isActive()) into.add(column)
        is Group -> children.forEach { it.collectActiveColumns(into) }
        is Not -> child.collectActiveColumns(into)
    }
}
