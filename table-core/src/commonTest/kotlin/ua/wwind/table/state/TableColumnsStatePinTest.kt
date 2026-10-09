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
        side: PinnedSide = PinnedSide.Start,
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
        val state = stateWith(pinned = 1, side = PinnedSide.End)
        state.columns.pin("a")
        assertThat(state.columns.order.toList()).containsExactly("b", "c", "a", "d")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
    }

    @Test
    fun `unpin on the right moves the column right before the pinned block`() {
        val state = stateWith(pinned = 2, side = PinnedSide.End)
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

    @Test
    fun `pin with a hidden column inside the pinned block keeps the block intact`() {
        val state = stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e"))
        state.columns.hide("b")
        state.columns.pin("d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "d", "c", "e")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
        assertThat(state.columns.isPinned("d")).isTrue()
        assertThat(state.columns.isPinned("c")).isFalse()
    }

    @Test
    fun `unpin with a hidden column inside the pinned block moves the column past the block`() {
        val state = stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e"))
        state.columns.hide("b")
        state.columns.pin("c")
        state.columns.pin("d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "d", "e")
        state.columns.unpin("a")
        assertThat(state.columns.visibleKeys()).containsExactly("c", "d", "a", "e")
        assertThat(state.columns.pinnedCount).isEqualTo(2)
        assertThat(state.columns.isPinned("a")).isFalse()
    }

    @Test
    fun `pin with a hidden column inside the block on the right`() {
        val state = stateWith(pinned = 1, side = PinnedSide.End, keys = listOf("a", "b", "c", "d", "e"))
        state.columns.hide("d")
        state.columns.pin("b")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "b", "e")
        assertThat(state.columns.isPinned("b")).isTrue()
        assertThat(state.columns.isPinned("c")).isFalse()
        state.columns.unpin("e")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "e", "b")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
    }

    @Test
    fun `pin when the stored count covers every visible column restarts the block at one`() {
        val state = stateWith(pinned = 4)
        assertThat(state.columns.isPinned("a")).isFalse()
        state.columns.pin("c")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
        assertThat(state.columns.order.toList()).containsExactly("c", "a", "b", "d")
        assertThat(state.columns.isPinned("c")).isTrue()
        assertThat(state.columns.isPinned("a")).isFalse()
    }

    @Test
    fun `pin when the stored count exceeds the visible count restarts the block at one`() {
        val state = stateWith(pinned = 9)
        state.columns.pin("b")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "c", "d")
    }
}
