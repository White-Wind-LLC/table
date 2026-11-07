package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

/** Start and end pinned blocks at the same time. */
class TableColumnsStateTwoSidedPinTest {
    private fun stateWith(
        start: Int,
        end: Int,
        keys: List<String> = listOf("a", "b", "c", "d", "e"),
    ) = TableState(
        initialColumns = keys,
        initialSort = null,
        initialOrder = keys,
        initialWidths = emptyMap(),
        settings = TableSettings(pinnedStartCount = start, pinnedEndCount = end),
        dimensions = TableDefaults.standardDimensions(),
    )

    private fun TableState<String>.sides() = columns.visibleKeys().map { columns.pinnedSide(it) }

    @Test
    fun `both counts start from settings`() {
        val state = stateWith(start = 1, end = 2)
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(2)
        assertThat(state.sides())
            .containsExactly(PinnedSide.Start, null, null, PinnedSide.End, PinnedSide.End)
        assertThat(state.columns.isPinned("a")).isTrue()
        assertThat(state.columns.isPinned("c")).isFalse()
    }

    @Test
    fun `pin to the end while a start block exists`() {
        val state = stateWith(start = 1, end = 0)
        state.columns.pin("b", PinnedSide.End)
        assertThat(state.columns.order.toList()).containsExactly("a", "c", "d", "e", "b")
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
        assertThat(state.columns.pinnedSide("b")).isEqualTo(PinnedSide.End)
    }

    @Test
    fun `pin to the start while an end block exists`() {
        val state = stateWith(start = 0, end = 1)
        state.columns.pin("c", PinnedSide.Start)
        assertThat(state.columns.order.toList()).containsExactly("c", "a", "b", "d", "e")
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
    }

    @Test
    fun `unpin an end column leaves the start block alone`() {
        val state = stateWith(start = 1, end = 2)
        state.columns.unpin("e")
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "c", "e", "d")
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
    }

    @Test
    fun `unpin a start column leaves the end block alone`() {
        val state = stateWith(start = 2, end = 1)
        state.columns.unpin("a")
        assertThat(state.columns.order.toList()).containsExactly("b", "a", "c", "d", "e")
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
    }

    @Test
    fun `counts covering every visible column pin none`() {
        val state = stateWith(start = 2, end = 3)
        assertThat(state.sides()).containsExactly(null, null, null, null, null)
    }

    @Test
    fun `pin when both counts cover every column restarts with one block of one`() {
        val state = stateWith(start = 2, end = 3)
        state.columns.pin("c", PinnedSide.End)
        assertThat(state.columns.pinnedStartCount).isEqualTo(0)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
        assertThat(state.columns.order.toList()).containsExactly("a", "b", "d", "e", "c")
    }

    @Test
    fun `the last unpinned column cannot be pinned to either side`() {
        val state = stateWith(start = 2, end = 2)
        assertThat(state.columns.canPin("c", PinnedSide.Start)).isFalse()
        assertThat(state.columns.canPin("c", PinnedSide.End)).isFalse()
        state.columns.pin("c", PinnedSide.End)
        assertThat(state.columns.pinnedEndCount).isEqualTo(2)
    }

    @Test
    fun `a pinned column cannot be pinned again to the other side`() {
        val state = stateWith(start = 1, end = 0)
        assertThat(state.columns.canPin("a", PinnedSide.End)).isFalse()
    }

    @Test
    fun `hiding an end-pinned column shrinks only the end block`() {
        val state = stateWith(start = 1, end = 2)
        state.columns.hide("e")
        assertThat(state.columns.pinnedStartCount).isEqualTo(1)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
        assertThat(state.columns.visibleKeys()).containsExactly("a", "b", "c", "d")
        assertThat(state.sides()).containsExactly(PinnedSide.Start, null, null, PinnedSide.End)
    }

    @Test
    fun `moveBy stays inside its block on both sides`() {
        val state = stateWith(start = 1, end = 1)
        assertThat(state.columns.canMoveBy("d", 1)).isFalse()
        assertThat(state.columns.canMoveBy("b", -1)).isFalse()
        assertThat(state.columns.canMoveBy("c", 1)).isTrue()
    }

    @Test
    fun `a column shown inside the end block lands before it while a start block exists`() {
        val state = stateWith(start = 1, end = 1)
        state.columns.hide("d")
        state.columns.pin("c", PinnedSide.End)
        assertThat(state.columns.visibleKeys()).containsExactly("a", "b", "c", "e")
        state.columns.show("d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "b", "d", "c", "e")
        assertThat(state.columns.pinnedSide("d")).isNull()
        assertThat(state.columns.pinnedEndCount).isEqualTo(2)
    }

    @Test
    fun `showAll keeps relative order at both edges`() {
        val keys = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i")
        val state = stateWith(start = 1, end = 1, keys = keys)
        listOf("b", "c", "g", "h").forEach { state.columns.hide(it) }
        state.columns.pin("d", PinnedSide.Start)
        state.columns.pin("f", PinnedSide.End)
        assertThat(state.columns.visibleKeys()).containsExactly("a", "d", "e", "f", "i")
        state.columns.showAll()
        assertThat(state.columns.visibleKeys()).containsExactly("a", "d", "b", "c", "e", "g", "h", "f", "i")
        assertThat(state.columns.pinnedStartCount).isEqualTo(2)
        assertThat(state.columns.pinnedEndCount).isEqualTo(2)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy end settings map to the end block`() {
        val keys = listOf("a", "b", "c", "d")
        val state =
            TableState(
                initialColumns = keys,
                initialSort = null,
                initialOrder = keys,
                initialWidths = emptyMap(),
                settings = TableSettings(pinnedColumnsCount = 2, pinnedColumnsSide = PinnedSide.End),
                dimensions = TableDefaults.standardDimensions(),
            )
        assertThat(state.columns.pinnedStartCount).isEqualTo(0)
        assertThat(state.columns.pinnedEndCount).isEqualTo(2)
        assertThat(state.columns.pinnedCount).isEqualTo(2)
        state.columns.pin("a")
        assertThat(state.columns.pinnedSide("a")).isEqualTo(PinnedSide.End)
        assertThat(state.columns.pinnedCount).isEqualTo(3)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy count adds to the new count on its side`() {
        val state =
            TableState(
                initialColumns = listOf("a", "b", "c", "d", "e"),
                initialSort = null,
                initialOrder = listOf("a", "b", "c", "d", "e"),
                initialWidths = emptyMap(),
                settings = TableSettings(pinnedColumnsCount = 1, pinnedStartCount = 1, pinnedEndCount = 1),
                dimensions = TableDefaults.standardDimensions(),
            )
        assertThat(state.columns.pinnedStartCount).isEqualTo(2)
        assertThat(state.columns.pinnedEndCount).isEqualTo(1)
    }
}
