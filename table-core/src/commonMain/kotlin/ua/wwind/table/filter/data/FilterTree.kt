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
    return rewrite(path) { node } as Group<C>
}

/**
 * Removes the node at [path]. Emptied groups and nots are removed too; a non-root group left with
 * one child is replaced by it. Nothing else is rewritten.
 */
internal fun <C> Group<C>.removeAt(path: FilterPath): Group<C> {
    if (path.isEmpty() || nodeAt(path) == null) return this
    return removeFrom(this, path, isRoot = true) as Group<C>
}

internal fun <C> Group<C>.setOperatorAt(
    path: FilterPath,
    operator: Operator,
): Group<C> {
    if (path.isEmpty()) return this
    val target = nodeAt(path) as? Group ?: return this
    return rewrite(path) { target.copy(operator = operator) } as Group<C>
}

internal fun <C> Group<C>.toggleNotAt(path: FilterPath): Group<C> {
    if (path.isEmpty()) return this
    val target = nodeAt(path) ?: return this
    return rewrite(path) { if (target is Not) target.child else Not(target) } as Group<C>
}

private fun <C> TableFilterNode<C>.rewrite(
    path: FilterPath,
    transform: () -> TableFilterNode<C>,
): TableFilterNode<C> {
    if (path.isEmpty()) return transform()
    val index = path.first()
    val rest = path.drop(1)
    return when (this) {
        is Group -> {
            copy(
                children =
                    children.mapIndexed { i, child ->
                        if (i == index) child.rewrite(rest, transform) else child
                    },
            )
        }

        is Not -> {
            copy(child = child.rewrite(rest, transform))
        }

        is Condition -> {
            this
        }
    }
}

private fun <C> removeFrom(
    node: TableFilterNode<C>,
    path: FilterPath,
    isRoot: Boolean,
): TableFilterNode<C>? {
    val index = path.first()
    val rest = path.drop(1)
    return when (node) {
        is Group -> {
            val children =
                node.children.mapIndexedNotNull { i, child ->
                    when {
                        i != index -> child
                        rest.isEmpty() -> null
                        else -> removeFrom(child, rest, isRoot = false)
                    }
                }
            when {
                isRoot -> node.copy(children = children)
                children.isEmpty() -> null
                children.size == 1 && node.children.size != 1 -> children.single()
                else -> node.copy(children = children)
            }
        }

        is Not -> {
            if (rest.isEmpty()) null else removeFrom(node.child, rest, isRoot = false)?.let { Not(it) }
        }

        is Condition -> {
            node
        }
    }
}
