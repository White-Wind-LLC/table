package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
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

    /** The restore recipe from docs/content/guides/column-menu.md, applied to a fresh state. */
    private fun restore(
        saved: TableState<String>,
        side: PinnedSide,
        specHidden: Set<String> = emptySet(),
    ): TableState<String> {
        val savedOrder = saved.columns.order.toList()
        val savedHidden = saved.columns.hidden.toSet()
        val savedPinned = saved.columns.pinnedCount
        val restored =
            TableState(
                initialColumns = savedOrder,
                initialSort = null,
                initialOrder = savedOrder,
                initialWidths = emptyMap(),
                // pinnedColumnsCount stays 0: the recipe pins through the state.
                settings = TableSettings(pinnedColumnsSide = side),
                dimensions = TableDefaults.standardDimensions(),
            )
        // Spec-hidden columns are not rendered, so they are not in the rendered list either.
        restored.columns.specVisibleKeys = savedOrder.filterNot { it in specHidden }.toSet()
        savedHidden.forEach { restored.columns.hide(it) }
        val visible = savedOrder.filter { it !in specHidden && it !in savedHidden }
        when (side) {
            PinnedSide.Left -> visible.take(savedPinned)
            PinnedSide.Right -> visible.takeLast(savedPinned).asReversed()
        }.forEach { restored.columns.pin(it) }
        return restored
    }

    private fun assertRestored(
        original: TableState<String>,
        restored: TableState<String>,
    ) {
        assertThat(restored.columns.order.toList()).containsExactly(*original.columns.order.toTypedArray())
        assertThat(restored.columns.hidden.toList()).containsExactly(*original.columns.hidden.toTypedArray())
        assertThat(restored.columns.pinnedCount).isEqualTo(original.columns.pinnedCount)
        assertThat(restored.pinnedKeys()).containsExactly(*original.pinnedKeys().toTypedArray())
    }

    @Test
    fun `the documented restore recipe brings back order, hidden and pins on the left`() {
        val original = stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e"))
        original.columns.hide("b")
        original.columns.pin("d")
        original.columns.moveBy("e", -1)
        assertThat(original.pinnedKeys()).containsExactly("a", "d")
        assertRestored(original, restore(original, PinnedSide.Left))
    }

    @Test
    fun `the documented restore recipe brings back order, hidden and pins on the right`() {
        val original = stateWith(pinned = 1, side = PinnedSide.Right, keys = listOf("a", "b", "c", "d", "e"))
        original.columns.hide("d")
        original.columns.pin("b")
        original.columns.moveBy("a", 1)
        assertThat(original.pinnedKeys()).containsExactly("b", "e")
        assertRestored(original, restore(original, PinnedSide.Right))
    }

    @Test
    fun `the restore recipe counts only rendered columns when the spec hides one`() {
        val original = stateWith(pinned = 0, side = PinnedSide.Right, keys = listOf("a", "b", "c", "d"))
        original.columns.specVisibleKeys = setOf("a", "c", "d")
        original.columns.pin("d")
        original.columns.pin("c")
        val restored = restore(original, PinnedSide.Right, specHidden = setOf("b"))
        assertThat(restored.pinnedKeys()).containsExactly("c", "d")
        assertThat(restored.columns.pinnedCount).isEqualTo(2)
        assertThat(restored.columns.hidden.toList()).isEmpty()
    }

    @Test
    fun `show works with the spec-visible keys set`() {
        val state = stateWith(pinned = 1)
        state.columns.specVisibleKeys = setOf("a", "b", "c", "d")
        state.columns.hide("b")
        state.columns.pin("c")
        state.columns.show("b")
        assertThat(state.columns.order.toList()).containsExactly("a", "c", "b", "d")
        assertThat(state.columns.isPinned("c")).isTrue()
    }

    @Test
    fun `showAll keeps the relative order of columns that sat inside the block`() {
        val state = stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e"))
        state.columns.hide("b")
        state.columns.hide("c")
        state.columns.pin("d")
        state.columns.showAll()
        assertThat(state.columns.visibleKeys()).containsExactly("a", "d", "b", "c", "e")
        assertThat(state.pinnedKeys()).containsExactly("a", "d")
    }

    @Test
    fun `on the right showAll keeps the relative order of columns that sat inside the block`() {
        val state = stateWith(pinned = 1, side = PinnedSide.Right, keys = listOf("a", "b", "c", "d", "e"))
        state.columns.hide("c")
        state.columns.hide("d")
        state.columns.pin("b")
        assertThat(state.pinnedKeys()).containsExactly("b", "e")
        state.columns.showAll()
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "d", "b", "e")
        assertThat(state.pinnedKeys()).containsExactly("b", "e")
    }

    @Test
    fun `two shows back to back place columns as two separate shows do`() {
        fun prepared() =
            stateWith(pinned = 1, keys = listOf("a", "b", "c", "d", "e")).also {
                it.columns.specVisibleKeys = setOf("a", "b", "c", "d", "e")
                it.columns.hide("b")
                it.columns.hide("c")
                it.columns.pin("d")
            }
        val backToBack = prepared()
        backToBack.columns.show("b")
        backToBack.columns.show("c")

        val separate = prepared()
        separate.columns.show("b")
        assertThat(separate.columns.visibleKeys()).containsExactly("a", "d", "b", "e")
        separate.columns.show("c")

        assertThat(backToBack.columns.order.toList()).containsExactly(*separate.columns.order.toTypedArray())
        assertThat(backToBack.columns.order.toList()).containsExactly("a", "d", "c", "b", "e")
        assertThat(backToBack.columns.pinnedCount).isEqualTo(2)
    }

    @Test
    fun `a shown column the spec later hides is not counted as visible`() {
        val state = stateWith(pinned = 0)
        state.columns.specVisibleKeys = setOf("a", "b", "c", "d")
        state.columns.hide("b")
        state.columns.show("b")
        // The consumer then marks "b" invisible in its spec.
        state.columns.specVisibleKeys = setOf("a", "c", "d")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "d")
        state.columns.pin("c")
        assertThat(state.pinnedKeys()).containsExactly("c")
        assertThat(state.columns.canMoveBy("c", 1)).isFalse()
    }

    @Test
    fun `showing a spec-hidden column does not make it visible`() {
        val state = stateWith(pinned = 0)
        // "b" is hidden at runtime and by its spec.
        state.columns.hidden.add("b")
        state.columns.specVisibleKeys = setOf("a", "c", "d")
        state.columns.showAll()
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c", "d")
        assertThat(state.columns.hidden.toList()).isEmpty()
        assertThat(state.columns.effectivePinnedCount()).isEqualTo(0)
    }
}
