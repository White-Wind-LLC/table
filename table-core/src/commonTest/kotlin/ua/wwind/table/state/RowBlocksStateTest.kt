package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowBlocks
import kotlin.test.Test

private data class Item(
    val id: Int,
    val block: String?,
)

/**
 * The rendered fixture, in view order:
 *
 * ```
 * row 0..1  block "a"   (unit 0)
 * row 2..4  block "b"   (unit 1)
 * row 5     standalone  (unit 2)
 * row 6     block "c"   (unit 3)
 * ```
 */
private val fixture =
    listOf(
        Item(0, "a"),
        Item(1, "a"),
        Item(2, "b"),
        Item(3, "b"),
        Item(4, "b"),
        Item(5, null),
        Item(6, "c"),
    )

private class Harness(
    items: List<Item> = fixture,
) {
    val commits = mutableListOf<RowBlockMove>()
    val warnings = mutableListOf<String>()
    val state =
        RowBlocksState(
            config = RowBlocks<Item>(blockOf = { it.block }, onCommit = { commits += it }),
            rowKey = { item, _ -> requireNotNull(item).id },
            warn = { warnings += it },
        )

    init {
        state.reconcile(items.size) { items[it] }
    }

    fun reconcile(items: List<Item>) = state.reconcile(items.size) { items[it] }

    /** Item ids in the rendered (permuted) order — the observable result of every move. */
    fun viewIds(): List<Int> = (0 until state.itemsCount).map { requireNotNull(state.itemAt(it)).id }
}

class RowBlocksStateTest {
    // region derivation

    @Test
    fun `runs derive from blockOf over the rendered order`() {
        val h = Harness()
        assertThat(h.state.runs.map { it.id }).isEqualTo(listOf<Any>("a", "b", "c"))
        assertThat(h.state.runs.map { it.rows }).isEqualTo(listOf(0..1, 2..4, 6..6))
        assertThat(h.state.units.unitCount).isEqualTo(4)
        assertThat(h.state.units.isGroup(2)).isFalse()
        assertThat(h.state.units.isGroup(3)).isTrue()
    }

    @Test
    fun `rows with null id never form runs`() {
        val h = Harness(listOf(Item(0, null), Item(1, null), Item(2, null)))
        assertThat(h.state.runs).isEqualTo(emptyList<RowBlockRun>())
        assertThat(h.state.units.unitCount).isEqualTo(3)
    }

    @Test
    fun `placeholder rows break runs without a fragmentation warning`() {
        val items = listOf(Item(0, "a"), Item(1, "a"), Item(2, "a"))
        val h = Harness()
        // A not-yet-loaded row cannot prove block membership, so it must split the band — but the
        // upstream order is not accused: the placeholder may be an unloaded member of the block.
        h.state.reconcile(3) { index -> if (index == 1) null else items[index] }
        assertThat(h.state.itemAt(1)).isNull()
        assertThat(h.state.runs.map { it.rows }).isEqualTo(listOf(0..0, 2..2))
        assertThat(h.warnings).isEqualTo(emptyList<String>())
    }

    @Test
    fun `fragmented id warns once naming the id`() {
        val h = Harness(listOf(Item(0, "block-7"), Item(1, "block-7"), Item(2, null), Item(3, "block-7")))
        assertThat(h.state.runs.map { it.rows }).isEqualTo(listOf(0..1, 3..3))
        assertThat(h.state.runs.map { it.rows }).isEqualTo(listOf(0..1, 3..3))
        assertThat(h.warnings.size).isEqualTo(1)
        assertThat(h.warnings.single().contains("block-7")).isTrue()
    }

    @Test
    fun `placeholder gap does not mask real fragmentation`() {
        val items = listOf(Item(0, "a"), Item(1, "a"), Item(2, "b"), Item(3, "a"))
        val h = Harness()
        // The gap between the runs of "a" holds a LOADED foreign row, so this is real upstream
        // fragmentation; the extra placeholder must not launder it into a paging state.
        h.state.reconcile(4) { index -> if (index == 1) null else items[index] }
        assertThat(h.warnings.size).isEqualTo(1)
    }

    @Test
    fun `fragmented id does not warn again after upstream change`() {
        val h = Harness(listOf(Item(0, "a"), Item(1, null), Item(2, "a")))
        h.state.runs
        h.reconcile(listOf(Item(0, "a"), Item(1, null), Item(2, "a"), Item(3, "b")))
        h.state.runs
        assertThat(h.warnings.size).isEqualTo(1)
    }

    @Test
    fun `distinct adjacent ids do not warn`() {
        val h = Harness(listOf(Item(0, "a"), Item(1, "b")))
        assertThat(h.state.runs.map { it.id }).isEqualTo(listOf<Any>("a", "b"))
        assertThat(h.warnings).isEqualTo(emptyList<String>())
    }

    // endregion

    // region applyUnitMove

    @Test
    fun `move block down lands after the target unit`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 0, 1, 5, 6))
    }

    @Test
    fun `move block up lands before the target unit`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 3, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 6, 2, 3, 4, 5))
    }

    @Test
    fun `move block to the first unit`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 1, toUnit = 0)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 0, 1, 5, 6))
    }

    @Test
    fun `move block to the last unit`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 3)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 5, 6, 0, 1))
    }

    @Test
    fun `same unit move is a no-op and settles silently`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 1, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6))
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    @Test
    fun `adjacent blocks swap rederives runs`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.state.runs.map { it.id }).isEqualTo(listOf<Any>("b", "a", "c"))
        assertThat(h.state.runs.map { it.rows }).isEqualTo(listOf(0..2, 3..4, 6..6))
    }

    @Test
    fun `out of bounds unit move is dropped without starting a gesture`() {
        val h = Harness()
        // The engine can deliver a swap computed against a layout one frame older than the list
        // (async consumer pipelines); it must be ignored, never crash the drag.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 4)
        h.state.applyUnitMove(fromUnit = -1, toUnit = 0)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    @Test
    fun `stale move arriving after a mid-gesture reconcile is dropped`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 3)
        // The list shrinks under the gesture: unit indices from the old layout no longer exist.
        h.reconcile(fixture.take(2))
        h.state.applyUnitMove(fromUnit = 3, toUnit = 2)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    @Test
    fun `moves after a mid-gesture reconcile are swallowed until settle`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.reconcile(fixture.dropLast(1))
        // In bounds over the new list, but the gesture was cancelled: applying it would commit a
        // move the user never made.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
        // The pointer went up: the latch releases and the next gesture is live again.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 0, 1, 5))
        requireNotNull(h.state.settle())
        assertThat(h.commits.size).isEqualTo(1)
    }

    // endregion

    // region settle

    @Test
    fun `settle after a downward move anchors past the whole target block`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        val move = requireNotNull(h.state.settle())
        assertThat(h.commits).isEqualTo(listOf(move))
        assertThat(move.blockId).isEqualTo("a")
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
        // Regression for the v1 "collapsed to" defect: landing after block "b" must anchor on its
        // LAST row, not its leader — anchoring on the leader would drop the block inside "b".
        assertThat(move.afterKey).isEqualTo(4)
        assertThat(move.beforeKey).isEqualTo(5)
    }

    @Test
    fun `settle after an upward move anchors before the target leader`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 3, toUnit = 1)
        val move = requireNotNull(h.state.settle())
        assertThat(move.blockId).isEqualTo("c")
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(6))
        assertThat(move.afterKey).isEqualTo(1)
        assertThat(move.beforeKey).isEqualTo(2)
    }

    @Test
    fun `multi swap gesture emits exactly one event`() {
        val h = Harness()
        // Drag block "a" from the top all the way to the end: three engine swaps, one commit.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.state.applyUnitMove(fromUnit = 1, toUnit = 2)
        h.state.applyUnitMove(fromUnit = 2, toUnit = 3)
        val move = requireNotNull(h.state.settle())
        assertThat(h.commits.size).isEqualTo(1)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 5, 6, 0, 1))
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
        assertThat(move.afterKey).isEqualTo(6)
        assertThat(move.beforeKey).isNull()
    }

    @Test
    fun `standalone row move commits null block id and edge anchor`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 2, toUnit = 0)
        val move = requireNotNull(h.state.settle())
        assertThat(move.blockId).isNull()
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(5))
        assertThat(move.afterKey).isNull()
        assertThat(move.beforeKey).isEqualTo(0)
    }

    @Test
    fun `settle without a gesture emits nothing`() {
        val h = Harness()
        assertThat(h.state.settle()).isNull()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.state.settle()
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(1)
    }

    @Test
    fun `gesture returning to origin emits nothing`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.state.applyUnitMove(fromUnit = 1, toUnit = 0)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5, 6))
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    // endregion

    // region reconcile

    @Test
    fun `external change mid gesture cancels the gesture`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.state.isDragActive).isTrue()
        h.reconcile(fixture.dropLast(1))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, 3, 4, 5))
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    @Test
    fun `reconcile with unchanged items keeps the optimistic order`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.state.settle()
        // The consumer has not applied the move yet: the permuted view must hold, no snap-back.
        h.reconcile(fixture)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 0, 1, 5, 6))
        assertThat(h.commits.size).isEqualTo(1)
    }

    @Test
    fun `reconcile with the applied move resets to identity without visual change`() {
        val h = Harness()
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        h.state.settle()
        val applied = h.viewIds().map { id -> fixture.first { it.id == id } }
        h.reconcile(applied)
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 4, 0, 1, 5, 6))
        assertThat(h.state.upstreamIndexOf(0)).isEqualTo(0)
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(1)
    }

    // endregion
}
