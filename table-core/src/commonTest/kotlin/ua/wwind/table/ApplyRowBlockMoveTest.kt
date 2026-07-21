package ua.wwind.table

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import ua.wwind.table.state.RowBlocksState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

private data class Row(
    val key: Int,
    val block: String?,
)

private fun row(
    key: Int,
    block: String? = null,
) = Row(key, block)

private fun MutableList<Row>.applyMove(move: RowBlockMove) =
    applyRowBlockMove(move, keyOf = {
        it.key
    }, blockOf = { it.block })

/** Fails when any non-null block id occupies non-adjacent positions — the P1 invariant. */
private fun assertBlocksContiguous(rows: List<Row>) {
    val finished = mutableSetOf<String>()
    var runId: String? = null
    for (item in rows) {
        val id = item.block
        if (id == runId) continue
        runId?.let { finished += it }
        runId = id
        if (id != null && id in finished) fail("block $id is fragmented in ${rows.map { it.key to it.block }}")
    }
}

class ApplyRowBlockMoveTest {
    // region deterministic lifts

    @Test
    fun `block move relocates hidden members with the block`() {
        // Row 3 of block "a" is hidden by the filter: the commit names only rows 1..2, yet the
        // whole block must travel — leaving row 3 behind would fragment "a" in the source.
        val source = mutableListOf(row(1, "a"), row(2, "a"), row(3, "a"), row(4, "b"), row(5, "b"), row(6))
        source.applyMove(RowBlockMove(blockId = "a", movedKeys = listOf(1, 2), afterKey = 5, beforeKey = 6))
        assertThat(source.map { it.key }).isEqualTo(listOf(4, 5, 1, 2, 3, 6))
    }

    @Test
    fun `insertion expands past the hidden tail of the anchor block`() {
        // View is [1, 2, 5]: rows 3..4 of block "a" hide behind their visible leader. Inserting
        // right after the anchor would drop row 1 inside the hidden span and split "a".
        val source = mutableListOf(row(1), row(2, "a"), row(3, "a"), row(4, "a"), row(5))
        source.applyMove(RowBlockMove(blockId = null, movedKeys = listOf(1), afterKey = 2, beforeKey = 5))
        assertThat(source.map { it.key }).isEqualTo(listOf(2, 3, 4, 1, 5))
    }

    @Test
    fun `hidden block leader travels and stays the leader`() {
        val source = mutableListOf(row(1, "a"), row(2, "a"), row(3, "a"), row(4, "b"))
        // The source leader (row 1) is hidden, so movedKeys cannot name it; relocation is by id.
        source.applyMove(RowBlockMove(blockId = "a", movedKeys = listOf(2, 3), afterKey = 4, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(4, 1, 2, 3))
    }

    @Test
    fun `null after anchor inserts at the start of the source`() {
        val source = mutableListOf(row(1), row(2), row(3, "b"), row(4, "b"))
        source.applyMove(RowBlockMove(blockId = "b", movedKeys = listOf(3, 4), afterKey = null, beforeKey = 1))
        assertThat(source.map { it.key }).isEqualTo(listOf(3, 4, 1, 2))
    }

    @Test
    fun `missing after anchor falls back to the before anchor expanding backward`() {
        // The primary anchor (row 9) was deleted between the commit and the apply. The fallback
        // anchor is a non-leader member of "a" (its leader hides above it), so the insertion must
        // walk back to the block start — inserting right before row 3 would split "a".
        val source = mutableListOf(row(1), row(2, "a"), row(3, "a"), row(4))
        source.applyMove(RowBlockMove(blockId = null, movedKeys = listOf(4), afterKey = 9, beforeKey = 3))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 4, 2, 3))
    }

    @Test
    fun `missing after anchor with null before anchor inserts at the end`() {
        val source = mutableListOf(row(1), row(2), row(3))
        source.applyMove(RowBlockMove(blockId = null, movedKeys = listOf(1), afterKey = 9, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(2, 3, 1))
    }

    @Test
    fun `move with both anchors missing leaves the list untouched`() {
        val source = mutableListOf(row(1), row(2), row(3))
        source.applyMove(RowBlockMove(blockId = null, movedKeys = listOf(1), afterKey = 8, beforeKey = 9))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `move of a vanished block id leaves the list untouched`() {
        val source = mutableListOf(row(1), row(2, "a"))
        source.applyMove(RowBlockMove(blockId = "gone", movedKeys = listOf(9), afterKey = 1, beforeKey = null))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2))
    }

    @Test
    fun `standalone move with vanished keys leaves the list untouched`() {
        val source = mutableListOf(row(1), row(2))
        source.applyMove(RowBlockMove(blockId = null, movedKeys = listOf(9), afterKey = 1, beforeKey = 2))
        assertThat(source.map { it.key }).isEqualTo(listOf(1, 2))
    }

    // endregion

    // region property harness

    /**
     * End-to-end property proof over random sources, random filters and random gestures: the move
     * is the REAL commit `RowBlocksState` emits for the gesture, applied to the source the state
     * never saw — exactly the consumer's situation.
     */
    @Test
    fun `random filtered gestures hold every lift property across 3000 commits`() {
        val random = Random(20260717)
        var effective = 0
        var attempts = 0
        var blockMovesWithHiddenMembers = 0
        var blockMovesWithHiddenLeader = 0
        while (effective < 3000) {
            check(++attempts <= 30_000) { "degenerate generator: $effective effective gestures in $attempts attempts" }
            val source = randomSource(random)
            // 25% of rows hidden: V is an order-preserving filter of S, the in-contract shape.
            val visibleKeys = source.filter { random.nextInt(4) > 0 }.map { it.key }.toSet()
            val view = source.filter { it.key in visibleKeys }

            val commits = mutableListOf<RowBlockMove>()
            val warnings = mutableListOf<String>()
            val state =
                RowBlocksState(
                    config = RowBlocks<Row>(blockOf = { it.block }, onCommit = { commits += it }),
                    rowKey = { item, _ -> requireNotNull(item).key },
                    warn = { warnings += it },
                )
            state.reconcile(view.size) { view[it] }
            val unitCount = state.units.unitCount
            if (unitCount < 2) continue
            val fromUnit = random.nextInt(unitCount)
            val toUnit = random.nextInt(unitCount)
            if (fromUnit == toUnit) continue
            state.applyUnitMove(fromUnit, toUnit)
            val move = requireNotNull(state.settle())
            val permutedView = (0 until state.itemsCount).map { requireNotNull(state.itemAt(it)) }
            assertThat(warnings).isEqualTo(emptyList<String>())

            val result = source.toMutableList()
            result.applyMove(move)

            val belongsToMove: (Row) -> Boolean =
                when (val blockId = move.blockId) {
                    null -> ({ it.key in move.movedKeys })
                    else -> ({ it.block == blockId })
                }
            // The result is a permutation of the source: nothing lost, nothing duplicated.
            assertThat(result.sortedBy { it.key }).isEqualTo(source.sortedBy { it.key })
            // No block — moved or foreign — is ever fragmented.
            assertBlocksContiguous(result)
            // Filtering the lifted source reproduces the drag result exactly: the lift is invisible
            // in the view the user manipulated.
            assertThat(result.filter { it.key in visibleKeys }).isEqualTo(permutedView)
            // Rows outside the moved unit keep their relative order, hidden ones included.
            assertThat(result.filterNot(belongsToMove)).isEqualTo(source.filterNot(belongsToMove))
            // Moved rows keep their relative order, hidden ones included.
            assertThat(result.filter(belongsToMove)).isEqualTo(source.filter(belongsToMove))
            // Applying the same commit twice is a fixpoint — a consumer replaying an event stream
            // must not drift.
            val reapplied = result.toMutableList()
            reapplied.applyMove(move)
            assertThat(reapplied).isEqualTo(result)

            if (move.blockId != null) {
                val members = source.filter { it.block == move.blockId }
                if (members.size > move.movedKeys.size) blockMovesWithHiddenMembers++
                if (members.first().key !in visibleKeys) blockMovesWithHiddenLeader++
            }
            effective++
        }
        // The generator must actually exercise the hidden-member semantics, not pass on trivially
        // visible moves.
        assertThat(blockMovesWithHiddenMembers).isGreaterThan(100)
        assertThat(blockMovesWithHiddenLeader).isGreaterThan(50)
    }

    /** Units of 1..4 rows; roughly half standalone, half blocks with unique ids — in-contract S. */
    private fun randomSource(random: Random): List<Row> {
        var key = 1
        var block = 0
        return buildList {
            repeat(random.nextInt(2, 9)) {
                if (random.nextBoolean()) {
                    add(row(key++))
                } else {
                    val id = "b${block++}"
                    repeat(random.nextInt(1, 5)) { add(row(key++, id)) }
                }
            }
        }
    }

    // endregion
}
