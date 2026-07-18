package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import ua.wwind.table.RowBlocks
import ua.wwind.table.RowWithinBlockMove
import kotlin.test.Test

private data class WbItem(val id: Int, val block: String?)

// row 0..1 block "a" (unit 0) | row 2..4 block "b" (unit 1) | row 5 standalone (unit 2) | row 6 block "c" (unit 3)
private val wbFixture =
    listOf(
        WbItem(0, "a"),
        WbItem(1, "a"),
        WbItem(2, "b"),
        WbItem(3, "b"),
        WbItem(4, "b"),
        WbItem(5, null),
        WbItem(6, "c"),
    )

private class WbHarness(
    items: List<WbItem> = wbFixture,
) {
    val within = mutableListOf<RowWithinBlockMove>()
    val state =
        RowBlocksState(
            config = RowBlocks<WbItem>(blockOf = { it.block }, onRowReorderWithinBlock = { within += it }),
            rowKey = { item, _ -> requireNotNull(item).id },
        )

    init {
        state.reconcile(items.size) { items[it] }
    }

    fun reconcile(items: List<WbItem>) = state.reconcile(items.size) { items[it] }

    fun viewIds(): List<Int> = (0 until state.itemsCount).map { requireNotNull(state.itemAt(it)).id }
}

class RowBlocksWithinBlockTest {
    @Test
    fun `move a row down within its block permutes only inside the block`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 4) // id 2 to the end of block "b"
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 3, 4, 2, 5, 6))
    }

    @Test
    fun `move a row up within its block`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 4, toView = 2) // id 4 to the start of block "b"
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 4, 2, 3, 5, 6))
    }

    @Test
    fun `a move crossing a block boundary is dropped`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 6) // block "b" -> block "c"
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6))
        assertThat(h.state.isDragActive).isFalse()
    }

    @Test
    fun `a move on a standalone unit is dropped`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 5, toView = 5)
        assertThat(h.state.isDragActive).isFalse()
    }

    @Test
    fun `settle after a down move anchors on the block-mate and the block edge`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 4)
        val move = requireNotNull(h.state.settleWithinBlock())
        assertThat(move.blockId).isEqualTo("b")
        assertThat(move.movedKey).isEqualTo(2)
        assertThat(move.afterKey).isEqualTo(4) // now the last visible row of block "b"
        assertThat(move.beforeKey).isNull() // block end
        assertThat(h.within).isEqualTo(listOf(move))
    }

    @Test
    fun `settle after an up move to the block start has a null after anchor`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 4, toView = 2)
        val move = requireNotNull(h.state.settleWithinBlock())
        assertThat(move.movedKey).isEqualTo(4)
        assertThat(move.afterKey).isNull() // block start
        assertThat(move.beforeKey).isEqualTo(2) // first block-mate below it
    }

    @Test
    fun `multi swap within a block emits exactly one event`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 3)
        h.state.applyRowMoveWithinBlock(fromView = 3, toView = 4)
        val move = requireNotNull(h.state.settleWithinBlock())
        assertThat(h.within.size).isEqualTo(1)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 3, 4, 2, 5, 6))
        assertThat(move.movedKey).isEqualTo(2)
    }

    @Test
    fun `settle returning to origin emits nothing`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 3)
        h.state.applyRowMoveWithinBlock(fromView = 3, toView = 2)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6))
        assertThat(h.state.settleWithinBlock()).isNull()
        assertThat(h.within.size).isEqualTo(0)
    }

    @Test
    fun `external change mid gesture cancels the within-block gesture`() {
        val h = WbHarness()
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 4)
        h.reconcile(wbFixture.dropLast(1))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.state.settleWithinBlock()).isNull()
        assertThat(h.within.size).isEqualTo(0)
    }

    @Test
    fun `placeholder landing neighbour refuses the drop and snaps back`() {
        // Block "b" = rows 2..4; hide row 3 (id 3) as a placeholder. Dragging id 2 to sit after the
        // placeholder cannot anchor: refuse, snap back, emit nothing.
        val h = WbHarness()
        h.state.reconcile(wbFixture.size) { index -> if (index == 3) null else wbFixture[index] }
        h.state.applyRowMoveWithinBlock(fromView = 2, toView = 3)
        assertThat(h.state.settleWithinBlock()).isNull()
        assertThat(h.within.size).isEqualTo(0)
        assertThat((0 until h.state.itemsCount).map { h.state.itemAt(it)?.id })
            .isEqualTo(listOf(0, 1, 2, null, 4, 5, 6))
    }
}
