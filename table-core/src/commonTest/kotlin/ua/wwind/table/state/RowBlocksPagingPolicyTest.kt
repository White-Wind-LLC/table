package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowBlocks
import kotlin.test.Test

private data class PagedItem(
    val id: Int,
    val block: String?,
)

/**
 * Paged sources render placeholders (null items) for rows whose page has not arrived. Two policies
 * hang off that fact: a placeholder fill-in mid-gesture must NOT cancel the drag (holding the drag
 * is what loads the page whose rows will anchor the drop), and a drop whose landing neighbour is
 * still a placeholder must snap back without emitting — a key that does not exist yet cannot
 * anchor a move.
 */
private class PagedHarness(
    items: List<PagedItem?>,
) {
    val commits = mutableListOf<RowBlockMove>()
    val warnings = mutableListOf<String>()
    val state =
        RowBlocksState(
            config = RowBlocks<PagedItem>(blockOf = { it.block }, onCommit = { commits += it }),
            rowKey = { item, index -> item?.id ?: "ph-$index" },
            warn = { warnings += it },
        )

    init {
        reconcile(items)
    }

    fun reconcile(items: List<PagedItem?>) = state.reconcile(items.size) { items[it] }

    /** Item ids in the rendered (permuted) order; null marks a placeholder. */
    fun viewIds(): List<Int?> = (0 until state.itemsCount).map { state.itemAt(it)?.id }
}

class RowBlocksPagingPolicyTest {
    // region reconcile: placeholder fill-in vs real change

    @Test
    fun `placeholder fill-in mid gesture keeps the gesture and the drop commits`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), null, null, PagedItem(4, null)),
            )
        // Units: block a (rows 0..1), two placeholders, one standalone.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(null, 0, 1, null, 4))
        assertThat(h.state.isDragActive).isTrue()

        // The held drag renders the placeholders, their page arrives: nothing moved, so the
        // permutation — and the gesture — must survive.
        h.reconcile(
            listOf(PagedItem(0, "a"), PagedItem(1, "a"), PagedItem(2, null), PagedItem(3, null), PagedItem(4, null)),
        )
        assertThat(h.state.isDragActive).isTrue()
        assertThat(h.viewIds()).isEqualTo(listOf(2, 0, 1, 3, 4))

        // The gesture continues over the freshly loaded rows and drops on loaded anchors.
        h.state.applyUnitMove(fromUnit = 1, toUnit = 2)
        val move = requireNotNull(h.state.settle())
        assertThat(h.commits).isEqualTo(listOf(move))
        assertThat(move.blockId).isEqualTo("a")
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
        assertThat(move.afterKey).isEqualTo(3)
        assertThat(move.beforeKey).isEqualTo(4)
    }

    @Test
    fun `eviction mid gesture cancels like any other change`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), PagedItem(2, null), PagedItem(3, null)),
            )
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        // A loaded row turning back into a placeholder loses its key and block membership under
        // the engine — the reverse of a fill-in is not gesture-safe.
        h.reconcile(listOf(PagedItem(0, "a"), PagedItem(1, "a"), null, PagedItem(3, null)))
        assertThat(h.state.isDragActive).isFalse()
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, null, 3))
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
    }

    @Test
    fun `placeholder fill-in outside a gesture keeps the optimistic hold`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), null, PagedItem(3, null)),
            )
        h.state.applyUnitMove(fromUnit = 0, toUnit = 2)
        requireNotNull(h.state.settle())
        assertThat(h.viewIds()).isEqualTo(listOf(null, 3, 0, 1))
        // The consumer has not applied the move yet when the page arrives; positions are
        // untouched by a fill-in, so the permuted view must hold exactly like an unchanged list.
        h.reconcile(listOf(PagedItem(0, "a"), PagedItem(1, "a"), PagedItem(2, null), PagedItem(3, null)))
        assertThat(h.viewIds()).isEqualTo(listOf(2, 3, 0, 1))
        assertThat(h.commits.size).isEqualTo(1)
    }

    // endregion

    // region settle: paged drop policy

    @Test
    fun `drop with loaded anchors commits despite placeholders elsewhere`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), null, PagedItem(3, null)),
            )
        // The drag crosses the placeholder unit and lands last: both anchors resolve (the row
        // above is loaded, there is no row below), so the placeholder en route is irrelevant.
        h.state.applyUnitMove(fromUnit = 0, toUnit = 2)
        val move = requireNotNull(h.state.settle())
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(0, 1))
        assertThat(move.afterKey).isEqualTo(3)
        assertThat(move.beforeKey).isNull()
        assertThat(h.viewIds()).isEqualTo(listOf(null, 3, 0, 1))
    }

    @Test
    fun `drop onto an unloaded after anchor snaps back and emits nothing`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), PagedItem(2, null), null),
            )
        h.state.applyUnitMove(fromUnit = 0, toUnit = 2)
        assertThat(h.viewIds()).isEqualTo(listOf(2, null, 0, 1))
        // The row the block would sit after is a placeholder: no key exists to anchor the move.
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 1, 2, null))
        assertThat(h.state.isDragActive).isFalse()
        // The refusal must be observable: the embedded engine rebuilds off this count, and a
        // count that stays put would strand its drag offsets on the dropped order.
        assertThat(h.state.refusedDropCount).isEqualTo(1)

        // The snapped-back state is clean: a next gesture onto loaded anchors commits normally.
        h.state.applyUnitMove(fromUnit = 1, toUnit = 0)
        val move = requireNotNull(h.state.settle())
        assertThat(move.blockId).isNull()
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(2))
        assertThat(move.afterKey).isNull()
        assertThat(move.beforeKey).isEqualTo(0)
        // A committed drop is not a refusal — the count must not bump.
        assertThat(h.state.refusedDropCount).isEqualTo(1)
    }

    @Test
    fun `drop onto an unloaded before anchor snaps back and emits nothing`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, null), null, PagedItem(2, "a"), PagedItem(3, "a")),
            )
        h.state.applyUnitMove(fromUnit = 2, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(0, 2, 3, null))
        // The row the block would sit before is a placeholder: the redundant anchor must be as
        // real as the primary one, or a consumer falling back to it would misplace the block.
        assertThat(h.state.settle()).isNull()
        assertThat(h.commits.size).isEqualTo(0)
        assertThat(h.viewIds()).isEqualTo(listOf(0, null, 2, 3))
    }

    @Test
    fun `page load merging into the dragged block widens the committed extent`() {
        val h =
            PagedHarness(
                listOf(PagedItem(0, "a"), PagedItem(1, "a"), null, PagedItem(3, null)),
            )
        h.state.applyUnitMove(fromUnit = 0, toUnit = 1)
        assertThat(h.viewIds()).isEqualTo(listOf(null, 0, 1, 3))
        // The placeholder above the dragged fragment loads as a member of the SAME block: the
        // runs merge, and the merged unit is what the user visibly drops. Reporting the stale
        // two-row extent would anchor the move on a row inside its own block.
        h.reconcile(listOf(PagedItem(0, "a"), PagedItem(1, "a"), PagedItem(2, "a"), PagedItem(3, null)))
        assertThat(h.viewIds()).isEqualTo(listOf(2, 0, 1, 3))
        val move = requireNotNull(h.state.settle())
        assertThat(move.blockId).isEqualTo("a")
        assertThat(move.movedKeys).isEqualTo(listOf<Any>(2, 0, 1))
        assertThat(move.afterKey).isNull()
        assertThat(move.beforeKey).isEqualTo(3)
    }

    // endregion
}
