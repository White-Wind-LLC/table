package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ua.wwind.table.RowBlocks
import kotlin.test.Test

private data class RemapItem(
    val id: Int,
    val block: String?,
)

/**
 * The commit-side companion of `settle()`: the remap is what carries positional runtime state
 * (selection, editing, cached heights) across a drop, so it must mirror the permutation exactly
 * and be read before settling forgets the pre-gesture order.
 */
class RowBlocksGestureRemapTest {
    private val items =
        listOf(
            RemapItem(0, "a"),
            RemapItem(1, "a"),
            RemapItem(2, "b"),
            RemapItem(3, "b"),
            RemapItem(4, null),
        )

    private fun state(): RowBlocksState<RemapItem> =
        RowBlocksState(
            config = RowBlocks<RemapItem>(blockOf = { it.block }),
            rowKey = { item, _ -> requireNotNull(item).id },
            warn = {},
        ).also { s -> s.reconcile(items.size) { items[it] } }

    @Test
    fun `remap follows rows across a block move and passes stale positions through`() {
        val s = state()
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        // view is now [2, 3, 0, 1, 4]
        val remap = requireNotNull(s.gestureRemap())
        assertThat(remap(0)).isEqualTo(2)
        assertThat(remap(1)).isEqualTo(3)
        assertThat(remap(2)).isEqualTo(0)
        assertThat(remap(3)).isEqualTo(1)
        assertThat(remap(4)).isEqualTo(4)
        // Selection kept across older data updates may point anywhere; it must pass through.
        assertThat(remap(99)).isEqualTo(99)
        assertThat(remap(-1)).isEqualTo(-1)
    }

    @Test
    fun `remap positions whose content changed are exactly the shifted ones`() {
        val s = state()
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        val remap = requireNotNull(s.gestureRemap())
        // Height-cache clearing iterates this predicate; position 4 stayed put and must be spared.
        val shifted = (0 until s.itemsCount).filter { remap(it) != it }
        assertThat(shifted).isEqualTo(listOf(0, 1, 2, 3))
    }

    @Test
    fun `remap is null without a gesture and after settle`() {
        val s = state()
        assertThat(s.gestureRemap()).isNull()
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        s.settle()
        assertThat(s.gestureRemap()).isNull()
    }

    @Test
    fun `remap is null when the gesture returned to its origin`() {
        val s = state()
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        s.applyUnitMove(fromUnit = 1, toUnit = 0)
        assertThat(s.gestureRemap()).isNull()
    }

    @Test
    fun `blockIdAt reports ids in view order and null out of range`() {
        val s = state()
        assertThat(s.blockIdAt(0)).isEqualTo("a")
        assertThat(s.blockIdAt(4)).isNull()
        assertThat(s.blockIdAt(5)).isNull()
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(s.blockIdAt(0)).isEqualTo("b")
    }

    @Test
    fun `itemAt answers out-of-range probes with a placeholder`() {
        val s = state()
        assertThat(s.itemAt(items.size)).isNull()
        assertThat(s.itemAt(-1)).isNull()
    }

    @Test
    fun `upstreamIndexOf answers out-of-range probes with the raw index`() {
        val s = state()
        // The raw index is what a table without blocks would hand the consumer's rowKey; stale
        // probes must degrade to that, not throw, exactly like itemAt and blockIdAt.
        assertThat(s.upstreamIndexOf(items.size)).isEqualTo(items.size)
        assertThat(s.upstreamIndexOf(-1)).isEqualTo(-1)
        s.applyUnitMove(fromUnit = 0, toUnit = 1)
        // view is now [2, 3, 0, 1, 4]
        assertThat(s.upstreamIndexOf(0)).isEqualTo(2)
    }
}
