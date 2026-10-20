package ua.wwind.table.filter.data

import androidx.compose.runtime.Immutable

/**
 * A node of the filter expression tree: a column [Condition], an [Operator.AND]/[Operator.OR] [Group], a [Not], or
 * an [Unresolved] condition the app could not map to a column.
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

    /**
     * Leaf: a stored condition the app cannot express as a [Condition], e.g. on a renamed or removed field.
     * The table never interprets it and keeps it in place, with the same [payload], on every edit; the user
     * can only remove it. While a tree holds one, apps should not apply it at all (see
     * [ua.wwind.table.state.TableState.unresolvedFilters]).
     *
     * Equality includes [payload], and the table compares nodes to keep a chip open, so give it value
     * equality or keep passing the same instance.
     *
     * @property key the raw field key as stored, shown to the user
     * @property payload opaque app data, e.g. the original predicates, round-tripped unchanged
     */
    public data class Unresolved(
        val key: String,
        val payload: Any? = null,
    ) : TableFilterNode<Nothing>
}

/**
 * True when any condition under this node is active; an [TableFilterNode.Unresolved] leaf always is.
 */
public fun TableFilterNode<*>.isActive(): Boolean =
    when (this) {
        is TableFilterNode.Condition -> state.isActive()
        is TableFilterNode.Group -> children.any { it.isActive() }
        is TableFilterNode.Not -> child.isActive()
        is TableFilterNode.Unresolved -> true
    }

/** The [TableFilterNode.Unresolved] leaves under this node, in tree order. */
public fun TableFilterNode<*>.unresolvedLeaves(): List<TableFilterNode.Unresolved> =
    buildList { collectUnresolved(this@unresolvedLeaves) }

private fun MutableList<TableFilterNode.Unresolved>.collectUnresolved(node: TableFilterNode<*>) {
    when (node) {
        is TableFilterNode.Condition -> Unit
        is TableFilterNode.Group -> node.children.forEach { collectUnresolved(it) }
        is TableFilterNode.Not -> collectUnresolved(node.child)
        is TableFilterNode.Unresolved -> add(node)
    }
}
