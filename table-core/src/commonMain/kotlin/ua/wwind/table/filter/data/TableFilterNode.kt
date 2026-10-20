package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

/**
 * A node of the filter expression tree: a column [Condition], an [Operator.AND]/[Operator.OR] [Group], or a [Not].
 */
@Immutable
public sealed interface TableFilterNode<out C> {
    /** Logical operator joining the children of a [Group]. */
    public enum class Operator { AND, OR }

    /** Leaf: the [state] applied to [column]. */
    public data class Condition<C>(
        val column: C,
        val state: TableFilterState<*>,
    ) : TableFilterNode<C>

    /** Children joined by [operator]. */
    public data class Group<C>(
        val operator: Operator,
        val children: List<TableFilterNode<C>>,
    ) : TableFilterNode<C>

    /** Negation of [child]. */
    public data class Not<C>(
        val child: TableFilterNode<C>,
    ) : TableFilterNode<C>
}

/**
 * True when any condition under this node is active.
 */
public fun TableFilterNode<*>.isActive(): Boolean =
    when (this) {
        is TableFilterNode.Condition -> state.isActive()
        is TableFilterNode.Group -> children.any { it.isActive() }
        is TableFilterNode.Not -> child.isActive()
    }
