package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

/** Showing a hidden column never takes a slot in the pinned block from a pinned column. */
class TableColumnsStateShowPinnedTest {
    private fun stateWith(
        pinned: Int,
        side: PinnedSide = PinnedSide.Left,
        keys: List<String> = listOf("a", "b", "c", "d"),
    ) = TableState(
        initialColumns = keys,
        initialSort = null,
        initialOrder = keys,
        initialWidths = emptyMap(),
        settings = TableSettings(pinnedColumnsCount = pinned, pinnedColumnsSide = side),
        dimensions = TableDefaults.standardDimensions(),
    )

    private fun TableState<String>.pinnedKeys() = columns.visibleKeys().filter { columns.isPinned(it) }

    @Test
    fun `hiding a pinned column and showing it again keeps the other pin`() {
        val state = stateWith(pinned = 2)
        state.columns.hide("a")
        assertThat(state.pinnedKeys()).containsExactly("b")
        state.columns.showAll()
        assertThat(state.columns.pinnedCount).isEqualTo(1)
        assertThat(state.pinnedKeys()).containsExactly("b")
        assertThat(state.columns.isPinned("a")).isFalse()
    }

    @Test
    fun `showing a column that sat inside the pinned block keeps it unpinned`() {
        val state = stateWith(pinned = 1)
        state.columns.hide("b")
        state.columns.pin("c")
        assertThat(state.pinnedKeys()).containsExactly("a", "c")
        state.columns.show("b")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
        assertThat(state.pinnedKeys()).containsExactly("a", "c")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "b", "d")
    }

    @Test
    fun `on the right a shown column lands at the last unpinned position`() {
        val state = stateWith(pinned = 1, side = PinnedSide.Right)
        state.columns.hide("c")
        state.columns.pin("b")
        assertThat(state.pinnedKeys()).containsExactly("b", "d")
        state.columns.show("c")
        assertThat(state.pinnedKeys()).containsExactly("b", "d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "b", "d")
    }

    @Test
    fun `on the right hiding a pinned column and showing it keeps the other pin`() {
        val state = stateWith(pinned = 2, side = PinnedSide.Right)
        state.columns.hide("d")
        assertThat(state.pinnedKeys()).containsExactly("c")
        state.columns.show("d")
        assertThat(state.pinnedKeys()).containsExactly("c")
        assertThat(state.columns.isPinned("d")).isFalse()
    }

    @Test
    fun `showAll keeps every pin when several hidden columns sat inside the block`() {
        val state = stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e", "f"))
        state.columns.hide("b")
        state.columns.hide("c")
        state.columns.pin("d")
        state.columns.pin("e")
        assertThat(state.pinnedKeys()).containsExactly("a", "d", "e")
        state.columns.showAll()
        assertThat(state.columns.pinnedCount).isEqualTo(3)
        assertThat(state.pinnedKeys()).containsExactly("a", "d", "e")
        assertThat(state.columns.isPinned("b")).isFalse()
        assertThat(state.columns.isPinned("c")).isFalse()
    }

    @Test
    fun `the documented restore recipe brings back order, hidden and pins`() {
        val original = stateWith(pinned = 1)
        original.columns.hide("b")
        original.columns.pin("c")
        val savedOrder = original.columns.order.toList()
        val savedHidden = original.columns.hidden.toSet()
        val savedPinned = original.columns.pinnedCount

        val restored =
            TableState(
                initialColumns = savedOrder,
                initialSort = null,
                initialOrder = savedOrder,
                initialWidths = emptyMap(),
                settings = TableSettings(),
                dimensions = TableDefaults.standardDimensions(),
            )
        savedHidden.forEach { restored.columns.hide(it) }
        savedOrder.filterNot { it in savedHidden }.take(savedPinned).forEach { restored.columns.pin(it) }

        assertThat(restored.columns.visibleKeys()).containsExactly(*original.columns.visibleKeys().toTypedArray())
        assertThat(restored.pinnedKeys()).containsExactly(*original.pinnedKeys().toTypedArray())
        assertThat(restored.columns.hidden.toList()).containsExactly("b")
    }

    @Test
    fun `show works while the rendered list still omits the shown column`() {
        val state = stateWith(pinned = 1)
        state.columns.hide("b")
        state.columns.pin("c")
        // The composed list lags a frame: it still omits "b" when show runs.
        state.columns.renderedKeys = { listOf("a", "c", "d") }
        state.columns.show("b")
        assertThat(state.columns.order.toList()).containsExactly("a", "c", "b", "d")
        state.columns.renderedKeys = null
        assertThat(state.columns.isPinned("c")).isTrue()
    }
}
