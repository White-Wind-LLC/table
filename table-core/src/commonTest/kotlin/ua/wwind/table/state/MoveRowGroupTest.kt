package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MoveRowGroupTest {
    @Test
    fun `move block down lands after the target`() {
        val list = (0..9).toMutableList()
        list.moveRowGroup(from = 2..4, to = 7..8)
        assertThat(list).isEqualTo(listOf(0, 1, 5, 6, 7, 8, 2, 3, 4, 9))
    }

    @Test
    fun `move block down past a single row`() {
        val list = (0..6).toMutableList()
        list.moveRowGroup(from = 2..4, to = 5..5)
        assertThat(list).isEqualTo(listOf(0, 1, 5, 2, 3, 4, 6))
    }

    @Test
    fun `move block up lands before the target`() {
        val list = (0..9).toMutableList()
        list.moveRowGroup(from = 5..6, to = 2..2)
        assertThat(list).isEqualTo(listOf(0, 1, 5, 6, 2, 3, 4, 7, 8, 9))
    }

    @Test
    fun `move single row behaves like remove and insert`() {
        val list = (0..4).toMutableList()
        list.moveRowGroup(from = 0..0, to = 3..3)
        assertThat(list).isEqualTo(listOf(1, 2, 3, 0, 4))
    }

    @Test
    fun `move to the same position is a no-op`() {
        val list = (0..4).toMutableList()
        list.moveRowGroup(from = 1..2, to = 1..2)
        assertThat(list).isEqualTo(listOf(0, 1, 2, 3, 4))
    }

    @Test
    fun `out of bounds move is rejected`() {
        val list = (0..4).toMutableList()
        assertFailsWith<IllegalArgumentException> { list.moveRowGroup(from = 3..7, to = 0..0) }
    }

    @Test
    fun `out of bounds target is rejected`() {
        val list = (0..4).toMutableList()
        assertFailsWith<IllegalArgumentException> { list.moveRowGroup(from = 0..0, to = 3..7) }
    }

    @Test
    fun `empty range is rejected`() {
        val list = (0..4).toMutableList()
        assertFailsWith<IllegalArgumentException> { list.moveRowGroup(from = 3..2, to = 0..0) }
        assertFailsWith<IllegalArgumentException> { list.moveRowGroup(from = 0..0, to = 3..2) }
    }

    // A swap between overlapping ranges has no meaning: the "target" partially IS the moved block.
    // Reaching this is a unit-derivation bug, and it must fail loudly instead of corrupting order.
    @Test
    fun `overlapping ranges are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            (0..9).toMutableList().moveRowGroup(from = 1..3, to = 2..4)
        }
        assertFailsWith<IllegalArgumentException> {
            (0..9).toMutableList().moveRowGroup(from = 2..4, to = 1..3)
        }
        assertFailsWith<IllegalArgumentException> {
            (0..9).toMutableList().moveRowGroup(from = 1..2, to = 1..3)
        }
    }
}
