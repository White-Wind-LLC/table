package ua.wwind.table.component.filterchips

import ua.wwind.table.filter.data.FilterPath
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterNode.Unresolved
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.unresolvedLeaves

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

    data class Unresolved<C>(
        override val path: FilterPath,
        val node: TableFilterNode.Unresolved,
    ) : FilterChipItem<C>

    data class Group<C>(
        override val path: FilterPath,
        val summary: FilterGroupSummary<C>,
    ) : FilterChipItem<C>
}

internal data class FilterGroupSummary<C>(
    /** False for a [Not] over a single condition, whose [operator] means nothing. */
    val isGroup: Boolean,
    val operator: Operator,
    val negated: Boolean,
    /** Active direct children of the group. */
    val activeCount: Int,
    /** Distinct columns of active leaves, in tree order; of all leaves while none is active. */
    val columns: List<C>,
    /** Set only for a non-negated OR whose children are all active conditions on one column. */
    val sharedColumn: C?,
    /** Unresolved leaves under the node, in tree order. */
    val unresolved: List<TableFilterNode.Unresolved> = emptyList(),
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

            is Unresolved -> {
                FilterChipItem.Unresolved(path, node)
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
    val columns =
        LinkedHashSet<C>()
            .also { inner.collectColumns(it, activeOnly = true) }
            .ifEmpty { LinkedHashSet<C>().also { inner.collectColumns(it, activeOnly = false) } }
            .toList()
    val unresolved = inner.unresolvedLeaves()
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
            FilterGroupSummary(
                true,
                node.operator,
                negated,
                node.children.count { it.isActive() },
                columns,
                shared,
                unresolved,
            )
        }

        else -> {
            FilterGroupSummary(false, Operator.AND, negated, if (node.isActive()) 1 else 0, columns, null, unresolved)
        }
    }
}

private fun <C> TableFilterNode<C>.collectColumns(
    into: MutableSet<C>,
    activeOnly: Boolean,
) {
    when (this) {
        is Condition -> if (!activeOnly || state.isActive()) into.add(column)
        is Group -> children.forEach { it.collectColumns(into, activeOnly) }
        is Not -> child.collectColumns(into, activeOnly)
        is Unresolved -> Unit
    }
}
