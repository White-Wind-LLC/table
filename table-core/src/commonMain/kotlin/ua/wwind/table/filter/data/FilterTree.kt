package ua.wwind.table.filter.data

import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator

/** Child indices from the root; a [Not]'s child is index 0. */
internal typealias FilterPath = List<Int>

internal fun <C> TableFilterNode<C>.asRoot(): Group<C> =
    if (this is Group && operator == Operator.AND) this else Group(Operator.AND, listOf(this))

internal fun <C> Group<C>.nodeAt(path: FilterPath): TableFilterNode<C>? {
    var node: TableFilterNode<C> = this
    for (index in path) {
        node =
            when (node) {
                is Group -> node.children.getOrNull(index)
                is Not -> if (index == 0) node.child else null
                is Condition -> null
            } ?: return null
    }
    return node
}

internal fun <C> Group<C>.replaceAt(
    path: FilterPath,
    node: TableFilterNode<C>,
): Group<C> {
    if (path.isEmpty() || nodeAt(path) == null) return this
    return rewrite(path, depth = 0) { node } as Group<C>
}

/**
 * Removes the node at [path]. Emptied groups and nots are removed too; a non-root group left with
 * one child is replaced by it. Nothing else is rewritten.
 */
internal fun <C> Group<C>.removeAt(path: FilterPath): Group<C> {
    if (path.isEmpty() || nodeAt(path) == null) return this
    return removeFrom(path, depth = 0) as Group<C>
}

internal fun <C> Group<C>.setOperatorAt(
    path: FilterPath,
    operator: Operator,
): Group<C> {
    if (path.isEmpty()) return this
    val target = nodeAt(path) as? Group ?: return this
    return rewrite(path, depth = 0) { target.copy(operator = operator) } as Group<C>
}

internal fun <C> Group<C>.toggleNotAt(path: FilterPath): Group<C> {
    if (path.isEmpty()) return this
    val target = nodeAt(path) ?: return this
    return rewrite(path, depth = 0) { if (target is Not) target.child else Not(target) } as Group<C>
}

// Callers check the path with nodeAt first, so every step below a node goes into a group or a not.

private fun <C> TableFilterNode<C>.rewrite(
    path: FilterPath,
    depth: Int,
    transform: () -> TableFilterNode<C>,
): TableFilterNode<C> {
    if (depth == path.size) return transform()
    return when (this) {
        is Not -> {
            copy(child = child.rewrite(path, depth + 1, transform))
        }

        else -> {
            val group = this as Group
            val index = path[depth]
            group.copy(
                children =
                    group.children.mapIndexed { i, child ->
                        if (i == index) child.rewrite(path, depth + 1, transform) else child
                    },
            )
        }
    }
}

private fun <C> TableFilterNode<C>.removeFrom(
    path: FilterPath,
    depth: Int,
): TableFilterNode<C>? {
    val isTarget = depth == path.lastIndex
    if (this is Not) return if (isTarget) null else child.removeFrom(path, depth + 1)?.let { Not(it) }
    val group = this as Group
    val index = path[depth]
    val children =
        group.children.mapIndexedNotNull { i, child ->
            when {
                i != index -> child
                isTarget -> null
                else -> child.removeFrom(path, depth + 1)
            }
        }
    return when {
        depth == 0 -> group.copy(children = children)
        children.isEmpty() -> null
        children.size == 1 && group.children.size != 1 -> children.single()
        else -> group.copy(children = children)
    }
}
