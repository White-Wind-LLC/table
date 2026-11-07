package ua.wwind.table.interaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

/** Keyboard navigation scrolls a cell into the part of the viewport the pinned blocks leave free. */
class HorizontalScrollDeltaTest {
    // Six 100px columns, a 300px viewport, one column pinned at each edge.
    private fun delta(
        cellStart: Int,
        scroll: Int,
        pinned: Boolean = false,
        startBlock: Int = 100,
        endBlock: Int = 100,
    ) = horizontalScrollDelta(
        cellStartPx = cellStart,
        cellEndPx = cellStart + 100,
        viewportStartPx = scroll,
        viewportEndPx = scroll + 300,
        startBlockPx = startBlock,
        endBlockPx = endBlock,
        targetPinned = pinned,
    )

    @Test
    fun `a cell that would end under the end block scrolls clear of it`() {
        // Column 3 spans 300..400; the free area at scroll 0 is 100..200.
        assertThat(delta(cellStart = 300, scroll = 0)).isEqualTo(200)
    }

    @Test
    fun `a cell under the start block scrolls clear of it`() {
        // Column 2 spans 200..300; at scroll 200 the free area is 300..400.
        assertThat(delta(cellStart = 200, scroll = 200)).isEqualTo(-100)
    }

    @Test
    fun `a cell already in the free area stays put`() {
        assertThat(delta(cellStart = 100, scroll = 0)).isEqualTo(0)
    }

    @Test
    fun `a pinned cell never scrolls the table`() {
        assertThat(delta(cellStart = 500, scroll = 0, pinned = true)).isEqualTo(0)
        assertThat(delta(cellStart = 0, scroll = 200, pinned = true)).isEqualTo(0)
    }

    @Test
    fun `without pinned blocks the whole viewport counts`() {
        assertThat(delta(cellStart = 300, scroll = 0, startBlock = 0, endBlock = 0)).isEqualTo(100)
        assertThat(delta(cellStart = 0, scroll = 50, startBlock = 0, endBlock = 0)).isEqualTo(-50)
    }
}
