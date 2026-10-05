package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

class TableColumnsStateMoveTest {
    private fun stateWith(pinned: Int = 0) =
        TableState(
            initialColumns = listOf("a", "b", "c", "d"),
            initialSort = null,
            initialOrder = listOf("a", "b", "c", "d"),
            initialWidths = emptyMap(),
            settings = TableSettings(pinnedColumnsCount = pinned),
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
}
