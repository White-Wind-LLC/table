package ua.wwind.table

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.random.Random
import kotlin.test.Test

private data class Entry(val key: Int, val block: String?, val value: Int)

/** The table's unit rule, reimplemented independently: adjacency runs, singletons for null ids. */
private fun unitsOf(rows: List<Entry>): List<List<Entry>> {
    val units = mutableListOf<MutableList<Entry>>()
    var runId: String? = null
    for (entry in rows) {
        val id = entry.block
        if (id != null && id == runId) {
            units.last() += entry
        } else {
            units += mutableListOf(entry)
            runId = id
        }
    }
    return units
}

/** Block units are identified by id, standalone units by their single row's key. */
private fun unitIdentity(unit: List<Entry>): Any = unit.first().block ?: unit.first().key

/** Units of 1..4 rows with values in 0..9, so comparator ties occur constantly. */
private fun randomEntries(random: Random): List<Entry> {
    var key = 1
    var block = 0
    return buildList {
        repeat(random.nextInt(1, 9)) {
            if (random.nextBoolean()) {
                add(Entry(key++, null, random.nextInt(10)))
            } else {
                val id = "b${block++}"
                repeat(random.nextInt(1, 5)) { add(Entry(key++, id, random.nextInt(10))) }
            }
        }
    }
}

class RowBlockOrderingTest {
    private val byValue = compareBy<Entry> { it.value }

    // region sortedWithinRowBlocks

    @Test
    fun `rows sort within their block and units order by their minimal member`() {
        val rows =
            listOf(
                Entry(1, "b", 5),
                Entry(2, "b", 2),
                Entry(3, null, 9),
                Entry(4, "a", 4),
                Entry(5, "a", 1),
            )
        val sorted = rows.sortedWithinRowBlocks({ it.block }, byValue)
        assertThat(sorted.map { it.key }).isEqualTo(listOf(5, 4, 2, 1, 3))
    }

    @Test
    fun `blocks never fragment under a comparator that interleaves them`() {
        // A plain sortedBy would render 1(a) 3(b) 4(b) 2(a): both blocks torn into duplicate bands.
        val rows =
            listOf(
                Entry(1, "a", 1),
                Entry(2, "a", 9),
                Entry(3, "b", 2),
                Entry(4, "b", 8),
            )
        val sorted = rows.sortedWithinRowBlocks({ it.block }, byValue)
        assertThat(sorted.map { it.key }).isEqualTo(listOf(1, 2, 3, 4))
    }

    @Test
    fun `ties keep their original order within a block and between units`() {
        val rows =
            listOf(
                Entry(1, "a", 3),
                Entry(2, "a", 3),
                Entry(3, null, 3),
                Entry(4, "b", 3),
            )
        assertThat(rows.sortedWithinRowBlocks({ it.block }, byValue)).isEqualTo(rows)
    }

    @Test
    fun `a list without blocks sorts like a plain stable sort`() {
        val rows = listOf(Entry(1, null, 5), Entry(2, null, 1), Entry(3, null, 5), Entry(4, null, 0))
        assertThat(rows.sortedWithinRowBlocks({ it.block }, byValue)).isEqualTo(rows.sortedWith(byValue))
    }

    @Test
    fun `empty list sorts to an empty list`() {
        assertThat(emptyList<Entry>().sortedWithinRowBlocks({ it.block }, byValue)).isEqualTo(emptyList<Entry>())
    }

    @Test
    fun `random lists hold every within-block ordering property across 1000 rounds`() {
        val random = Random(7177)
        repeat(1000) {
            val rows = randomEntries(random)
            val sorted = rows.sortedWithinRowBlocks({ it.block }, byValue)

            // Permutation: nothing lost, nothing duplicated.
            assertThat(sorted.sortedBy { it.key }).isEqualTo(rows.sortedBy { it.key })

            // Every unit survives whole — a split would surface as an extra run here.
            val originalUnits = unitsOf(rows)
            val sortedUnits = unitsOf(sorted)
            assertThat(sortedUnits.size).isEqualTo(originalUnits.size)

            // Within each unit: sorted by the comparator, ties in original order (stable).
            val expectedByIdentity = originalUnits.associate { unitIdentity(it) to it.sortedWith(byValue) }
            for (unit in sortedUnits) {
                assertThat(unit).isEqualTo(expectedByIdentity.getValue(unitIdentity(unit)))
            }

            // Between units: minimal members never decrease; equal minimals keep unit order.
            val originalPosition = originalUnits.withIndex().associate { (i, unit) -> unitIdentity(unit) to i }
            sortedUnits.zipWithNext().forEach { (left, right) ->
                val order = byValue.compare(left.first(), right.first())
                assertThat(order <= 0).isTrue()
                if (order == 0) {
                    assertThat(
                        originalPosition.getValue(unitIdentity(left)) < originalPosition.getValue(unitIdentity(right)),
                    ).isTrue()
                }
            }

            // Re-sorting the result changes nothing: the sort is a fixpoint.
            assertThat(sorted.sortedWithinRowBlocks({ it.block }, byValue)).isEqualTo(sorted)
        }
    }

    // endregion

    // region filteredWholeRowBlocks

    @Test
    fun `a single matching member keeps its whole block`() {
        val rows =
            listOf(
                Entry(1, "a", 1),
                Entry(2, "a", 7),
                Entry(3, null, 1),
                Entry(4, "b", 1),
                Entry(5, "b", 2),
            )
        val filtered = rows.filteredWholeRowBlocks({ it.block }) { it.value >= 5 }
        assertThat(filtered.map { it.key }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `standalone rows filter row by row`() {
        val rows = listOf(Entry(1, null, 5), Entry(2, null, 1), Entry(3, null, 6))
        val filtered = rows.filteredWholeRowBlocks({ it.block }) { it.value >= 5 }
        assertThat(filtered.map { it.key }).isEqualTo(listOf(1, 3))
    }

    @Test
    fun `random filters keep or drop whole blocks across 1000 rounds`() {
        val random = Random(41)
        repeat(1000) {
            val rows = randomEntries(random)
            // Thresholds 0..10 over values 0..9: keep-everything and drop-everything both occur.
            val threshold = random.nextInt(11)
            val predicate: (Entry) -> Boolean = { it.value >= threshold }
            val filtered = rows.filteredWholeRowBlocks({ it.block }, predicate)

            // Order-preserving subsequence of the input.
            val keptKeys = filtered.map { it.key }.toSet()
            assertThat(filtered).isEqualTo(rows.filter { it.key in keptKeys })

            // Block granularity: any match keeps the whole unit, none drops it whole.
            for (unit in unitsOf(rows)) {
                val expected = if (unit.any(predicate)) unit.map { it.key }.toSet() else emptySet()
                assertThat(unit.filter { it.key in keptKeys }.map { it.key }.toSet()).isEqualTo(expected)
            }
        }
    }

    // endregion
}
