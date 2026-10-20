package ua.wwind.table.state

import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterState

/**
 * `filters` as a view of the filter tree's root: the first top-level [Condition] per column, in tree
 * order. Writes touch only those conditions; groups and nots are left where they are.
 */
internal class FilterExpressionMap<C>(
    private val read: () -> Group<C>,
    private val write: (Group<C>) -> Unit,
) : AbstractMutableMap<C, TableFilterState<*>>() {
    // Every read rebuilds this list, so get and size are O(n) in the root's children; fine at the
    // filter counts tables have, and simpler than a cache kept in step with the tree.
    private fun firstConditions(): List<Condition<C>> =
        read()
            .children
            .filterIsInstance<Condition<C>>()
            .distinctBy { it.column }

    override fun put(
        key: C,
        value: TableFilterState<*>,
    ): TableFilterState<*>? {
        val root = read()
        val index = root.children.indexOfFirst { it is Condition && it.column == key }
        return if (index < 0) {
            write(root.copy(children = root.children + Condition(key, value)))
            null
        } else {
            val old = (root.children[index] as Condition<C>).state
            write(root.copy(children = root.children.toMutableList().also { it[index] = Condition(key, value) }))
            old
        }
    }

    override fun remove(key: C): TableFilterState<*>? {
        val root = read()
        val index = root.children.indexOfFirst { it is Condition && it.column == key }
        if (index < 0) return null
        val old = (root.children[index] as Condition<C>).state
        write(root.copy(children = root.children.filterIndexed { i, _ -> i != index }))
        return old
    }

    override fun clear() {
        val root = read()
        write(root.copy(children = root.children.filter { it !is Condition }))
    }

    override val entries: MutableSet<MutableMap.MutableEntry<C, TableFilterState<*>>>
        get() = EntrySet()

    private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<C, TableFilterState<*>>>() {
        override val size: Int get() = firstConditions().size

        override fun add(element: MutableMap.MutableEntry<C, TableFilterState<*>>): Boolean =
            throw UnsupportedOperationException()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<C, TableFilterState<*>>> {
            val snapshot = firstConditions().iterator()
            val map = this@FilterExpressionMap
            return object : MutableIterator<MutableMap.MutableEntry<C, TableFilterState<*>>> {
                private var last: Condition<C>? = null

                override fun hasNext(): Boolean = snapshot.hasNext()

                override fun next(): MutableMap.MutableEntry<C, TableFilterState<*>> {
                    val condition = snapshot.next()
                    last = condition
                    return Entry(condition.column, condition.state)
                }

                override fun remove() {
                    val condition = checkNotNull(last) { "next() has not been called" }
                    last = null
                    map.remove(condition.column)
                }
            }
        }
    }

    private inner class Entry(
        override val key: C,
        override var value: TableFilterState<*>,
    ) : MutableMap.MutableEntry<C, TableFilterState<*>> {
        override fun setValue(newValue: TableFilterState<*>): TableFilterState<*> {
            val old = value
            value = newValue
            put(key, newValue)
            return old
        }

        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && other.key == key && other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
