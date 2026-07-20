package ua.wwind.table

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

private data class WbRow(val key: Int, val block: String?)

private fun wbRow(
    key: Int,
    block: String? = null,
) = WbRow(key, block)

private fun MutableList<WbRow>.applyWb(move: RowWithinBlockMove) =
    applyRowReorderWithinBlock(move, keyOf = { it.key }, blockOf = { it.block })

class ApplyRowReorderWithinBlockTest {
    @Test
    fun `move a row to the end of its block`() {
        val source = mutableListOf(wbRow(0, "x"), wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"), wbRow(4))
        // id 1 lands after id 3 (block end)
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 1, afterKey = 3, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(0, 2, 3, 1, 4))
    }

    @Test
    fun `move a row to the start of its block`() {
        val source = mutableListOf(wbRow(0, "x"), wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"), wbRow(4))
        // id 3 lands at block start (null after anchor, before = first mate)
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 3, afterKey = null, beforeKey = 1))
        assertThat(source.map { it.key }).isEqualTo(listOf(0, 3, 1, 2, 4))
    }

    @Test
    fun `hidden block members keep their relative order`() {
        // Source block "a": 1, 2(hidden), 3. View is [1, 3]. Move id 3 above id 1.
        val source = mutableListOf(wbRow(0, "x"), wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"), wbRow(4))
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 3, afterKey = null, beforeKey = 1))
        // id 3 first, then 1, then hidden 2 keeps its position after 1
        assertThat(source.map { it.key }).isEqualTo(listOf(0, 3, 1, 2, 4))
    }

    @Test
    fun `missing after anchor falls back to before anchor`() {
        val source = mutableListOf(wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"))
        // after anchor (99) is gone; before anchor is id 3 -> land right before it
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 1, afterKey = 99, beforeKey = 3))
        assertThat(source.map { it.key }).isEqualTo(listOf(2, 1, 3))
    }

    @Test
    fun `both anchors missing leaves the list untouched`() {
        val source = mutableListOf(wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"))
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 2, afterKey = 88, beforeKey = 99))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `vanished moved key leaves the list untouched`() {
        val source = mutableListOf(wbRow(1, "a"), wbRow(2, "a"))
        source.applyWb(RowWithinBlockMove(blockId = "a", movedKey = 99, afterKey = 1, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `stale block id leaves the list untouched`() {
        val source = mutableListOf(wbRow(1, "a"), wbRow(2, "a"))
        source.applyWb(RowWithinBlockMove(blockId = "gone", movedKey = 1, afterKey = 2, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `applying the same move twice is a fixpoint`() {
        val source = mutableListOf(wbRow(0, "x"), wbRow(1, "a"), wbRow(2, "a"), wbRow(3, "a"), wbRow(4))
        val move = RowWithinBlockMove(blockId = "a", movedKey = 1, afterKey = 3, beforeKey = null)
        source.applyWb(move)
        val once = source.map { it.key }
        source.applyWb(move)
        assertThat(source.map { it.key }).isEqualTo(once)
    }
}
