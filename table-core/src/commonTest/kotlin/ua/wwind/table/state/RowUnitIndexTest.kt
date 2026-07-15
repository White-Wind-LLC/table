package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RowUnitIndexTest {
    @Test
    fun `no groups is identity`() {
        val index = buildRowUnitIndex(itemsCount = 10, groups = null)
        assertThat(index.unitCount).isEqualTo(10)
        repeat(10) { row ->
            assertThat(index.unitOf(row)).isEqualTo(row)
            assertThat(index.rowsOf(row)).isEqualTo(row..row)
            assertThat(index.isGroup(row)).isFalse()
        }
    }

    @Test
    fun `empty group list is identity`() {
        val index = buildRowUnitIndex(itemsCount = 3, groups = emptyList())
        assertThat(index.unitCount).isEqualTo(3)
        assertThat(index.unitOf(2)).isEqualTo(2)
    }

    @Test
    fun `single group in the middle collapses its rows`() {
        // rows: 0 1 [2 3 4] 5 6 7 8 9  ->  units: 0 1 2 3 4 5 6 7
        val index = buildRowUnitIndex(itemsCount = 10, groups = listOf(2..4))
        assertThat(index.unitCount).isEqualTo(8)
        assertThat(index.unitOf(0)).isEqualTo(0)
        assertThat(index.unitOf(1)).isEqualTo(1)
        assertThat(index.unitOf(2)).isEqualTo(2)
        assertThat(index.unitOf(3)).isEqualTo(2)
        assertThat(index.unitOf(4)).isEqualTo(2)
        assertThat(index.unitOf(5)).isEqualTo(3)
        assertThat(index.unitOf(9)).isEqualTo(7)
        assertThat(index.rowsOf(2)).isEqualTo(2..4)
        assertThat(index.rowsOf(3)).isEqualTo(5..5)
        assertThat(index.rowsOf(7)).isEqualTo(9..9)
        assertThat(index.isGroup(2)).isTrue()
        assertThat(index.isGroup(3)).isFalse()
    }

    @Test
    fun `group at the start`() {
        val index = buildRowUnitIndex(itemsCount = 5, groups = listOf(0..1))
        assertThat(index.unitCount).isEqualTo(4)
        assertThat(index.unitOf(0)).isEqualTo(0)
        assertThat(index.unitOf(1)).isEqualTo(0)
        assertThat(index.unitOf(2)).isEqualTo(1)
        assertThat(index.rowsOf(0)).isEqualTo(0..1)
    }

    @Test
    fun `group at the end`() {
        val index = buildRowUnitIndex(itemsCount = 5, groups = listOf(3..4))
        assertThat(index.unitCount).isEqualTo(4)
        assertThat(index.unitOf(4)).isEqualTo(3)
        assertThat(index.rowsOf(3)).isEqualTo(3..4)
    }

    @Test
    fun `adjacent groups stay separate units`() {
        // rows: [0 1] [2 3] 4  ->  units: 0 1 2
        val index = buildRowUnitIndex(itemsCount = 5, groups = listOf(0..1, 2..3))
        assertThat(index.unitCount).isEqualTo(3)
        assertThat(index.unitOf(1)).isEqualTo(0)
        assertThat(index.unitOf(2)).isEqualTo(1)
        assertThat(index.unitOf(4)).isEqualTo(2)
        assertThat(index.rowsOf(0)).isEqualTo(0..1)
        assertThat(index.rowsOf(1)).isEqualTo(2..3)
        assertThat(index.rowsOf(2)).isEqualTo(4..4)
    }

    @Test
    fun `single row group is still a group`() {
        val index = buildRowUnitIndex(itemsCount = 3, groups = listOf(1..1))
        assertThat(index.unitCount).isEqualTo(3)
        assertThat(index.isGroup(1)).isTrue()
        assertThat(index.rowsOf(1)).isEqualTo(1..1)
    }

    @Test
    fun `round trip holds for every row`() {
        val index = buildRowUnitIndex(itemsCount = 20, groups = listOf(2..4, 7..7, 15..18))
        repeat(20) { row ->
            val unit = index.unitOf(row)
            assertThat(index.rowsOf(unit).contains(row)).isTrue()
        }
    }

    @Test
    fun `every unit maps back to a distinct row range covering all rows`() {
        val index = buildRowUnitIndex(itemsCount = 12, groups = listOf(1..3, 8..9))
        val covered = (0 until index.unitCount).flatMap { index.rowsOf(it) }
        assertThat(covered).isEqualTo((0 until 12).toList())
    }

    // The footer is an extra lazy item at `unitCount`, past every unit the index describes. Callers
    // that read `layoutInfo` can hand that index straight back, so both accessors must answer for it
    // rather than throw or claim a group.
    @Test
    fun `footer unit index reports no group and the row past the end`() {
        val identity = buildRowUnitIndex(itemsCount = 10, groups = null)
        assertThat(identity.isGroup(identity.unitCount)).isFalse()
        assertThat(identity.rowsOf(identity.unitCount)).isEqualTo(10..10)

        val grouped = buildRowUnitIndex(itemsCount = 10, groups = listOf(2..4))
        assertThat(grouped.unitCount).isEqualTo(8)
        assertThat(grouped.isGroup(grouped.unitCount)).isFalse()
        assertThat(grouped.rowsOf(grouped.unitCount)).isEqualTo(10..10)
    }

    @Test
    fun `no items is identity with no units`() {
        val index = buildRowUnitIndex(itemsCount = 0, groups = listOf(0..0))
        assertThat(index.unitCount).isEqualTo(0)
        assertThat(index.isGroup(0)).isFalse()
    }

    @Test
    fun `overlapping groups are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            buildRowUnitIndex(itemsCount = 10, groups = listOf(2..4, 4..6))
        }
    }

    @Test
    fun `unsorted groups are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            buildRowUnitIndex(itemsCount = 10, groups = listOf(5..6, 1..2))
        }
    }

    @Test
    fun `out of bounds group is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            buildRowUnitIndex(itemsCount = 5, groups = listOf(3..7))
        }
    }

    @Test
    fun `empty range is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            buildRowUnitIndex(itemsCount = 5, groups = listOf(3..2))
        }
    }
}
