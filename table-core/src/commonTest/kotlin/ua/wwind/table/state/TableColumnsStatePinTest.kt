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

class TableColumnsStatePinTest {
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

    @Test
    fun `pinnedCount starts from settings`() {
        assertThat(stateWith(pinned = 2).columns.pinnedCount).isEqualTo(2)
    }

    @Test
    fun `pin on the left moves the column to the end of the pinned block`() {
        val state = stateWith(pinned = 1)
        state.columns.pin("c")
        assertThat(state.columns.order.toList()).containsExactly("a", "c", "b", "d")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
        assertThat(state.columns.isPinned("c")).isTrue()
    }

    @Test
    fun `unpin on the left moves the column right after the pinned block`() {
        val state = stateWith(pinned = 2)
        state.columns.unpin("a")
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "c", "d")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
        assertThat(state.columns.isPinned("a")).isFalse()
    }

    @Test
    fun `pin on the right moves the column to the start of the pinned block`() {
        val state = stateWith(pinned = 1, side = PinnedSide.Right)
        state.columns.pin("a")
        assertThat(state.columns.order.toList()).containsExactly("b", "c", "a", "d")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
    }

    @Test
    fun `unpin on the right moves the column right before the pinned block`() {
        val state = stateWith(pinned = 2, side = PinnedSide.Right)
        state.columns.unpin("d")
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "d", "c")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
    }

    @Test
    fun `the last unpinned column cannot be pinned`() {
        val state = stateWith(pinned = 3)
        assertThat(state.columns.canPin("d")).isFalse()
        state.columns.pin("d")
        assertThat(state.columns.pinnedCount).isEqualTo(3)
    }

    @Test
    fun `pin and unpin ignore columns already in the requested state`() {
        val state = stateWith(pinned = 1)
        state.columns.pin("a")
        state.columns.unpin("b")
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "c", "d")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
    }
}
