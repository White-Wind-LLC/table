package ua.wwind.table.state

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

class TableStateFilterExpressionTest {
    private val s1 = TableFilterState(FilterConstraint.EQUALS, listOf("1"))
    private val s2 = TableFilterState(FilterConstraint.EQUALS, listOf("2"))
    private val s3 = TableFilterState(FilterConstraint.EQUALS, listOf("3"))
    private val x = Condition("x", s1)
    private val y = Condition("y", s2)

    private fun and(vararg children: TableFilterNode<String>) = Group(Operator.AND, children.toList())

    private fun or(vararg children: TableFilterNode<String>) = Group(Operator.OR, children.toList())

    private fun newState() =
        TableState(
            initialColumns = listOf("a", "b", "c"),
            initialSort = null,
            initialOrder = listOf("a", "b", "c"),
            initialWidths = emptyMap(),
            settings = TableSettings(),
            dimensions = TableDefaults.standardDimensions(),
        )

    @Test
    fun `flat writes round-trip through the view`() {
        val state = newState()
        state.setFilter("a", s1)
        assertThat(state.filters["a"]).isEqualTo(s1)
        assertThat(state.filterExpression).isEqualTo(and(Condition("a", s1)))
        state.setFilter("a", null as TableFilterState<String>?)
        assertThat(state.filters).isEmpty()
        assertThat(state.filterExpression).isEqualTo(and())
    }

    @Test
    fun `header edit leaves complex nodes intact`() {
        val state = newState()
        state.setFilterExpression(and(or(x, y), Condition("c", s1)))
        state.setFilter("c", s2)
        assertThat(state.filterExpression).isEqualTo(and(or(x, y), Condition("c", s2)))
    }

    @Test
    fun `map writes leave complex nodes in place`() {
        val state = newState()
        state.setFilterExpression(and(Condition("a", s1), or(x, y)))
        state.filters["b"] = s2
        assertThat(state.filterExpression).isEqualTo(and(Condition("a", s1), or(x, y), Condition("b", s2)))
        state.filters.clear()
        assertThat(state.filterExpression).isEqualTo(and(or(x, y)))

        state.filters["a"] = s1
        val iterator = state.filters.entries.iterator()
        iterator.next()
        iterator.remove()
        assertThat(state.filterExpression).isEqualTo(and(or(x, y)))
    }

    @Test
    fun `duplicate column keeps both conditions`() {
        val state = newState()
        state.setFilterExpression(and(Condition("a", s1), Condition("a", s2)))
        assertThat(state.filters["a"]).isEqualTo(s1)
        assertThat(state.filters.size).isEqualTo(1)
        state.setFilter("a", s3)
        assertThat(state.filterExpression).isEqualTo(and(Condition("a", s3), Condition("a", s2)))
    }

    @Test
    fun `setFilterExpression wraps a non-AND root`() {
        val state = newState()
        state.setFilterExpression(or(x, y))
        assertThat(state.filterExpression).isEqualTo(and(or(x, y)))
    }

    @Test
    fun `internal writers wrap a non-AND root too`() {
        val state = newState()
        state.filterExpression = or(x, y)
        assertThat(state.filterExpression).isEqualTo(and(or(x, y)))
        state.updateFilterExpression { or(x) }
        assertThat(state.filterExpression).isEqualTo(and(or(x)))
    }

    @Test
    fun `entry setValue writes through to the tree`() {
        val state = newState()
        state.setFilterExpression(and(Condition("a", s1), or(x, y), Condition("b", s2)))
        val entry = state.filters.entries.first { it.key == "b" }

        assertThat(entry.setValue(s3)).isEqualTo(s2)

        assertThat(entry.value).isEqualTo(s3)
        assertThat(state.filterExpression).isEqualTo(and(Condition("a", s1), or(x, y), Condition("b", s3)))
    }

    @Test
    fun `iterator remove drops the shown condition and the next one on that column shows`() {
        val state = newState()
        state.setFilterExpression(and(Condition("a", s1), Condition("b", s2), Condition("a", s3)))
        val iterator = state.filters.entries.iterator()

        assertThat(iterator.next().key).isEqualTo("a")
        iterator.remove()

        assertThat(state.filterExpression).isEqualTo(and(Condition("b", s2), Condition("a", s3)))
        assertThat(state.filters["a"]).isEqualTo(s3)
        // The iteration goes on over the entries it started with.
        assertThat(iterator.next().key).isEqualTo("b")
        assertThat(iterator.hasNext()).isEqualTo(false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `filters view is snapshot observable`() =
        runTest(UnconfinedTestDispatcher()) {
            val state = newState()
            val seen = mutableListOf<Map<String, TableFilterState<*>>>()
            val job = launch { snapshotFlow { state.filters.toMap() }.collect { seen += it } }
            state.setFilter("a", s1)
            Snapshot.sendApplyNotifications()
            state.setFilter("b", s2)
            Snapshot.sendApplyNotifications()
            state.setFilter("a", null as TableFilterState<String>?)
            Snapshot.sendApplyNotifications()
            job.cancel()
            assertThat(seen).containsExactly(
                emptyMap<String, TableFilterState<*>>(),
                mapOf("a" to s1),
                mapOf("a" to s1, "b" to s2),
                mapOf("b" to s2),
            )
        }

    @Test
    fun `clearComplexFilters keeps simple conditions`() {
        val state = newState()
        state.setFilterExpression(and(Condition("a", s1), or(x, y), TableFilterNode.Not(x), Condition("b", s2)))
        state.clearComplexFilters()
        assertThat(state.filterExpression).isEqualTo(and(Condition("a", s1), Condition("b", s2)))
    }
}
