package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

class TableColumnsStateMoveTest {
    private fun stateWith(
        pinned: Int = 0,
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

    @Test
    fun `moveBy swaps with the visible neighbour`() {
        val state = stateWith()
        state.columns.moveBy("b", 1)
        assertThat(state.columns.order.toList()).containsExactly("a", "c", "b", "d")
        state.columns.moveBy("b", -1)
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "c", "d")
    }

    @Test
    fun `moveBy skips hidden columns`() {
        val state = stateWith()
        state.columns.hide("c")
        state.columns.moveBy("b", 1)
        assertThat(state.columns.visibleKeys()).containsExactly("a", "d", "b")
    }

    @Test
    fun `moveBy stops at the edges`() {
        val state = stateWith()
        assertThat(state.columns.canMoveBy("a", -1)).isFalse()
        assertThat(state.columns.canMoveBy("d", 1)).isFalse()
        state.columns.moveBy("a", -1)
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "c", "d")
    }

    @Test
    fun `moveBy never crosses the pinned boundary`() {
        val state = stateWith(pinned = 2)
        assertThat(state.columns.canMoveBy("b", 1)).isFalse()
        assertThat(state.columns.canMoveBy("c", -1)).isFalse()
        assertThat(state.columns.canMoveBy("a", 1)).isTrue()
        state.columns.moveBy("a", 1)
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "c", "d")
    }

    @Test
    fun `moveBy moves more than one position`() {
        val state = stateWith()
        state.columns.moveBy("a", 3)
        assertThat(state.columns.order.toList()).containsExactly("b", "c", "d", "a")
        state.columns.moveBy("a", -2)
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "c", "d")
        assertThat(state.columns.canMoveBy("a", 4)).isFalse()
    }

    @Test
    fun `moveBy on the right stays inside the pinned block`() {
        val state = stateWith(pinned = 2, side = PinnedSide.Right)
        assertThat(state.columns.canMoveBy("b", 1)).isFalse()
        assertThat(state.columns.canMoveBy("c", -1)).isFalse()
        assertThat(state.columns.canMoveBy("c", 1)).isTrue()
        state.columns.moveBy("c", 1)
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "d", "c")
        assertThat(state.columns.canMoveBy("a", 2)).isFalse()
        state.columns.moveBy("a", 1)
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "d", "c")
    }

    @Test
    fun `moveBy with a hidden column inside the pinned block`() {
        val state = stateWith(keys = listOf("a", "b", "c", "d", "e"), pinned = 1)
        state.columns.hide("b")
        state.columns.pin("c")
        state.columns.pin("d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "d", "e")
        assertThat(state.columns.canMoveBy("a", 3)).isFalse()
        assertThat(state.columns.canMoveBy("a", 2)).isTrue()
        state.columns.moveBy("a", 2)
        assertThat(state.columns.visibleKeys()).containsExactly("c", "d", "a", "e")
        assertThat(state.columns.isPinned("a")).isTrue()
        assertThat(state.columns.isPinned("e")).isFalse()
        state.columns.moveBy("a", -1)
        assertThat(state.columns.visibleKeys()).containsExactly("c", "a", "d", "e")
    }

    @Test
    fun `moveBy with a hidden column inside the pinned block on the right`() {
        val state = stateWith(keys = listOf("a", "b", "c", "d", "e"), pinned = 1, side = PinnedSide.Right)
        state.columns.hide("d")
        state.columns.pin("c")
        state.columns.pin("b")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "b", "c", "e")
        assertThat(state.columns.canMoveBy("e", -3)).isFalse()
        state.columns.moveBy("e", -2)
        assertThat(state.columns.visibleKeys()).containsExactly("a", "e", "b", "c")
        assertThat(state.columns.isPinned("e")).isTrue()
        assertThat(state.columns.isPinned("a")).isFalse()
    }
}
