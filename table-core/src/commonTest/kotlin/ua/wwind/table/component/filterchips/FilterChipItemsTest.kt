package ua.wwind.table.component.filterchips

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator.AND
import ua.wwind.table.filter.data.TableFilterNode.Operator.OR
import ua.wwind.table.filter.data.TableFilterNode.Unresolved
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

class FilterChipItemsTest {
    private fun eq(v: String) = TableFilterState(FilterConstraint.EQUALS, listOf(v))

    private fun cond(
        column: String,
        value: String = "1",
    ) = Condition(column, eq(value))

    private val known: (String) -> Boolean = { true }

    @Test
    fun `A OR B group AND C gives one group and one condition`() {
        val root = Group(AND, listOf(Group(OR, listOf(cond("a"), cond("b"))), cond("c")))

        val items = rowItems(root, known, keepPath = null)

        assertThat(items.map { it::class to it.path }).isEqualTo(
            listOf(FilterChipItem.Group::class to listOf(0), FilterChipItem.Condition::class to listOf(1)),
        )
        assertThat((items[1] as FilterChipItem.Condition).column).isEqualTo("c")
    }

    @Test
    fun `same-column OR has a shared column`() {
        val shared = Group(OR, listOf(cond("s", "x"), cond("s", "y")))
        assertThat(shared.summary().sharedColumn).isEqualTo("s")
        assertThat(Not(shared).summary().sharedColumn).isNull()
        assertThat(Group(OR, listOf(cond("s"), cond("t"))).summary().sharedColumn).isNull()
        assertThat(Group(AND, listOf(cond("s"), cond("s", "2"))).summary().sharedColumn).isNull()
    }

    @Test
    fun `summary counts active children and lists distinct columns deep`() {
        val group = Group(OR, listOf(Group(AND, listOf(cond("type"), cond("qty"), cond("type", "2"))), cond("vip")))

        val summary = group.summary()

        assertThat(summary.activeCount).isEqualTo(2)
        assertThat(summary.columns).isEqualTo(listOf("type", "qty", "vip"))
        assertThat(summary.negated).isEqualTo(false)
        assertThat(summary.operator).isEqualTo(OR)
    }

    @Test
    fun `negated group and negated condition summaries`() {
        val negatedGroup = Not(Group(AND, listOf(cond("a"), cond("b"))))
        assertThat(negatedGroup.summary().negated).isEqualTo(true)
        assertThat(negatedGroup.summary().activeCount).isEqualTo(2)

        val item = rowItems(Group(AND, listOf(Not(cond("a")))), known, null).single() as FilterChipItem.Group
        assertThat(item.summary.negated).isEqualTo(true)
        assertThat(item.summary.activeCount).isEqualTo(1)
    }

    @Test
    fun `unknown key becomes Unknown at top level`() {
        val root = Group(AND, listOf(cond("a"), cond("gone")))

        val items = rowItems(root, { it == "a" }, keepPath = null)

        assertThat(items[0] is FilterChipItem.Condition).isEqualTo(true)
        assertThat(items[1] is FilterChipItem.Unknown).isEqualTo(true)
        assertThat(items[1].path).isEqualTo(listOf(1))
    }

    @Test
    fun `inactive nodes are skipped unless open`() {
        val empty = Condition("a", TableFilterState<String>(FilterConstraint.EQUALS, emptyList()))
        val root = Group(AND, listOf(empty, cond("b")))

        assertThat(rowItems(root, known, null).map { it.path }).isEqualTo(listOf(listOf(1)))
        assertThat(rowItems(root, known, listOf(0)).map { it.path }).isEqualTo(listOf(listOf(0), listOf(1)))
    }

    @Test
    fun `an inactive group is kept only at keepPath`() {
        val empty = Condition("a", TableFilterState<String>(FilterConstraint.EQUALS, emptyList()))
        val root = Group(AND, listOf(Group(OR, listOf(empty, empty)), cond("b")))

        assertThat(rowItems(root, known, null).map { it.path }).isEqualTo(listOf(listOf(1)))
        val kept = rowItems(root, known, listOf(0)).first() as FilterChipItem.Group
        assertThat(kept.path).isEqualTo(listOf(0))
        assertThat(kept.summary.activeCount).isEqualTo(0)
        // With nothing active, the group reads as all the columns it is on.
        assertThat(kept.summary.columns).isEqualTo(listOf("a"))
    }

    @Test
    fun `a not over a condition is no group and an inactive one counts nothing`() {
        val empty = Condition("a", TableFilterState<String>(FilterConstraint.EQUALS, emptyList()))

        val active = Not(cond("a")).summary()
        assertThat(active.isGroup).isEqualTo(false)
        assertThat(active.activeCount).isEqualTo(1)
        assertThat(Group(OR, listOf(cond("a"), cond("b"))).summary().isGroup).isEqualTo(true)
        assertThat(Not(Group(OR, listOf(cond("a")))).summary().isGroup).isEqualTo(true)

        val kept = rowItems(Group(AND, listOf(Not(empty))), known, listOf(0)).single() as FilterChipItem.Group
        assertThat(kept.summary.activeCount).isEqualTo(0)
        assertThat(kept.summary.columns).isEqualTo(listOf("a"))
    }

    @Test
    fun `a double not cancels out`() {
        val summary = Not(Not(Group(AND, listOf(cond("a"), cond("b"))))).summary()

        assertThat(summary.negated).isEqualTo(false)
        assertThat(summary.isGroup).isEqualTo(true)
        assertThat(summary.activeCount).isEqualTo(2)

        val leaf = Not(Not(cond("a"))).summary()
        assertThat(leaf.negated).isEqualTo(false)
        assertThat(leaf.isGroup).isEqualTo(false)
    }

    @Test
    fun `a not over an unknown key is a group item`() {
        val root = Group(AND, listOf(Not(cond("gone"))))

        val item = rowItems(root, { it == "a" }, keepPath = null).single()

        assertThat(item is FilterChipItem.Group).isEqualTo(true)
        assertThat((item as FilterChipItem.Group).summary.columns).isEqualTo(listOf("gone"))
    }

    @Test
    fun `two conditions on one column show two items`() {
        val root = Group(AND, listOf(cond("a", "1"), cond("a", "2")))

        assertThat(rowItems(root, known, null).map { it.path }).isEqualTo(listOf(listOf(0), listOf(1)))
    }

    @Test
    fun `rowItems lists a top-level unresolved leaf`() {
        val u = Unresolved("old")
        val items = rowItems(Group(AND, listOf(cond("a"), u)), known, keepPath = null)

        assertThat(items[1]).isEqualTo(FilterChipItem.Unresolved(listOf(1), u))
    }

    @Test
    fun `summary of a group with an unresolved leaf`() {
        val u = Unresolved("old")
        val summary = Group(OR, listOf(cond("a"), u)).summary()

        assertThat(summary.unresolved).isEqualTo(listOf(u))
        assertThat(summary.sharedColumn).isNull()
        assertThat(summary.activeCount).isEqualTo(2)
        assertThat(Not(u).summary().unresolved).isEqualTo(listOf(u))
    }
}
