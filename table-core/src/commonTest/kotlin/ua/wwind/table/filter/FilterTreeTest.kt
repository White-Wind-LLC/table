package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator.AND
import ua.wwind.table.filter.data.TableFilterNode.Operator.OR
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.asRoot
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.nodeAt
import ua.wwind.table.filter.data.removeAt
import ua.wwind.table.filter.data.replaceAt
import ua.wwind.table.filter.data.setOperatorAt
import ua.wwind.table.filter.data.toggleNotAt
import ua.wwind.table.filter.data.unresolvedLeaves
import kotlin.test.Test

class FilterTreeTest {
    private fun eq(v: String) = TableFilterState(FilterConstraint.EQUALS, listOf(v))

    private val a = Condition("a", eq("1"))
    private val b = Condition("b", eq("2"))
    private val b2 = Condition("b", eq("3"))
    private val c = Condition("c", eq("4"))

    private fun and(vararg n: TableFilterNode<String>) = Group(AND, n.toList())

    private fun or(vararg n: TableFilterNode<String>) = Group(OR, n.toList())

    @Test
    fun `asRoot wraps a non-AND node and keeps an AND group`() {
        assertThat(or(a, b).asRoot()).isEqualTo(and(or(a, b)))
        val root = and(a)
        assertThat(root.asRoot()).isSameInstanceAs(root)
    }

    @Test
    fun `removeAt dissolves a group left with one child`() {
        assertThat(and(or(a, b), c).removeAt(listOf(0, 1))).isEqualTo(and(a, c))
    }

    @Test
    fun `removeAt drops an emptied group and an emptied not`() {
        assertThat(and(Not(or(a)), c).removeAt(listOf(0, 0, 0))).isEqualTo(and(c))
    }

    @Test
    fun `removeAt keeps a not over a dissolved group`() {
        assertThat(and(Not(or(a, b))).removeAt(listOf(0, 0, 1))).isEqualTo(and(Not(a)))
    }

    @Test
    fun `removeAt never flattens same-operator nesting`() {
        assertThat(and(or(or(a, b), c)).removeAt(listOf(0, 1))).isEqualTo(and(or(a, b)))
    }

    @Test
    fun `removeAt on a top-level child never dissolves the root`() {
        assertThat(and(a).removeAt(listOf(0))).isEqualTo(and())
    }

    @Test
    fun `removeAt with an invalid path returns the receiver`() {
        val root = and(or(a, b))
        assertThat(root.removeAt(listOf(5))).isSameInstanceAs(root)
        assertThat(root.removeAt(listOf(0, 0, 0))).isSameInstanceAs(root)
        assertThat(root.removeAt(emptyList())).isSameInstanceAs(root)
    }

    @Test
    fun `setOperatorAt switches a nested group and ignores the root`() {
        val root = and(or(a, b), c)
        assertThat(root.setOperatorAt(listOf(0), AND)).isEqualTo(and(and(a, b), c))
        assertThat(root.setOperatorAt(emptyList(), OR)).isSameInstanceAs(root)
        assertThat(root.setOperatorAt(listOf(1), OR)).isSameInstanceAs(root)
    }

    @Test
    fun `toggleNotAt wraps then unwraps`() {
        val wrapped = and(or(a, b)).toggleNotAt(listOf(0))
        assertThat(wrapped).isEqualTo(and(Not(or(a, b))))
        assertThat(wrapped.toggleNotAt(listOf(0))).isEqualTo(and(or(a, b)))
    }

    @Test
    fun `toggleNotAt on a not over a condition leaves the condition`() {
        assertThat(and(Not(a), c).toggleNotAt(listOf(0))).isEqualTo(and(a, c))
        assertThat(and(or(Not(a), b)).toggleNotAt(listOf(0, 0))).isEqualTo(and(or(a, b)))
    }

    @Test
    fun `toggleNotAt and setOperatorAt with an invalid path return the receiver`() {
        val root = and(or(a, b), Not(c))
        assertThat(root.toggleNotAt(listOf(5))).isSameInstanceAs(root)
        assertThat(root.toggleNotAt(listOf(0, 0, 0))).isSameInstanceAs(root)
        assertThat(root.toggleNotAt(listOf(1, 1))).isSameInstanceAs(root)
        assertThat(root.toggleNotAt(emptyList())).isSameInstanceAs(root)
        assertThat(root.setOperatorAt(listOf(5), AND)).isSameInstanceAs(root)
        assertThat(root.setOperatorAt(listOf(1, 0), AND)).isSameInstanceAs(root)
        assertThat(root.setOperatorAt(listOf(0, 0), AND)).isSameInstanceAs(root)
    }

    @Test
    fun `nodeAt walks groups and nots`() {
        val root = and(or(a, Not(b)), c)
        assertThat(root.nodeAt(emptyList())).isSameInstanceAs(root)
        assertThat(root.nodeAt(listOf(0))).isEqualTo(or(a, Not(b)))
        assertThat(root.nodeAt(listOf(0, 1, 0))).isEqualTo(b)
        assertThat(root.nodeAt(listOf(1))).isEqualTo(c)
        assertThat(root.nodeAt(listOf(2))).isNull()
        assertThat(root.nodeAt(listOf(0, 1, 1))).isNull()
        assertThat(root.nodeAt(listOf(1, 0))).isNull()
        assertThat(root.nodeAt(listOf(-1))).isNull()
    }

    @Test
    fun `replaceAt swaps only the target leaf`() {
        assertThat(and(or(a, b), c).replaceAt(listOf(0, 1), b2)).isEqualTo(and(or(a, b2), c))
    }

    @Test
    fun `isActive looks through groups and nots`() {
        val empty = Condition("e", TableFilterState<String>(FilterConstraint.EQUALS, emptyList()))
        assertThat(and(or(empty), Not(empty)).isActive()).isFalse()
        assertThat(and(or(empty), Not(a)).isActive()).isTrue()
    }

    private val u = TableFilterNode.Unresolved("old", payload = Any())

    @Test
    fun `unresolved leaf is active`() {
        assertThat(u.isActive()).isTrue()
    }

    @Test
    fun `removeAt keeps an unresolved leaf in a group`() {
        val result = and(or(a, b, u), c).removeAt(listOf(0, 1))
        assertThat(result).isEqualTo(and(or(a, u), c))
        val leaf = result.nodeAt(listOf(0, 1)) as TableFilterNode.Unresolved
        assertThat(leaf).isSameInstanceAs(u)
        assertThat(leaf.payload).isSameInstanceAs(u.payload)
    }

    @Test
    fun `removing the unresolved leaf keeps its siblings`() {
        assertThat(and(or(a, b, u), c).removeAt(listOf(0, 2))).isEqualTo(and(or(a, b), c))
    }

    @Test
    fun `collapsing a group leaves the unresolved leaf at its place`() {
        val result = and(or(a, b, u), c).removeAt(listOf(0, 0)).removeAt(listOf(0, 0))
        assertThat(result).isEqualTo(and(u, c))
        assertThat(result.children[0]).isSameInstanceAs(u)
    }

    @Test
    fun `setOperatorAt and toggleNotAt keep the unresolved leaf`() {
        val root = and(or(a, b, u), c)
        assertThat(root.setOperatorAt(listOf(0), AND)).isEqualTo(and(Group(AND, listOf(a, b, u)), c))
        assertThat(root.toggleNotAt(listOf(0))).isEqualTo(and(Not(or(a, b, u)), c))
    }

    @Test
    fun `nodeAt does not step into an unresolved leaf`() {
        assertThat(and(u).nodeAt(listOf(0, 0))).isNull()
    }

    @Test
    fun `unresolvedLeaves lists leaves in tree order`() {
        val u2 = TableFilterNode.Unresolved("older")
        assertThat(and(u, or(a, Not(u2))).unresolvedLeaves()).containsExactly(u, u2)
        assertThat(and(a).unresolvedLeaves()).isEmpty()
    }
}
