package ua.wwind.table.state

import androidx.compose.runtime.Immutable

/**
 * Maps row indices (the public index space of the table) to unit indices (the index space of the
 * backing `LazyColumn`), where a unit is either a single row or a declared group of adjacent rows.
 *
 * Memory is O(number of groups): only group bounds and prefix sums are stored, never a per-row
 * array. With no groups the index is identity and no binary search is performed.
 */
@Immutable
internal class RowUnitIndex private constructor(
    private val starts: IntArray,
    private val ends: IntArray,
    /** `collapsedBefore[i]` = number of rows absorbed by groups left of group `i`; size is groups+1. */
    private val collapsedBefore: IntArray,
    val unitCount: Int,
) {
    val isIdentity: Boolean get() = starts.isEmpty()

    /** Unit index that renders [rowIndex]. */
    fun unitOf(rowIndex: Int): Int {
        if (isIdentity) return rowIndex
        val group = groupContaining(rowIndex)
        if (group >= 0) return starts[group] - collapsedBefore[group]
        return rowIndex - collapsedBefore[groupsFullyBefore(rowIndex)]
    }

    /** Rows rendered by [unitIndex]; a single-row unit returns `i..i`. */
    fun rowsOf(unitIndex: Int): IntRange {
        if (isIdentity) return unitIndex..unitIndex
        val group = groupAtUnit(unitIndex)
        if (group >= 0) return starts[group]..ends[group]
        val row = unitIndex + collapsedBefore[groupsBeforeUnit(unitIndex)]
        return row..row
    }

    fun isGroup(unitIndex: Int): Boolean = !isIdentity && groupAtUnit(unitIndex) >= 0

    /** Index of the group containing [row], or -1. */
    private fun groupContaining(row: Int): Int {
        val insertion = upperBoundByStart(row)
        val candidate = insertion - 1
        return if (candidate >= 0 && row <= ends[candidate]) candidate else -1
    }

    /** Number of groups that end strictly before [row]. */
    private fun groupsFullyBefore(row: Int): Int = upperBoundByStart(row)

    /** Number of groups whose `starts` are <= [row]. */
    private fun upperBoundByStart(row: Int): Int {
        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (starts[mid] <= row) low = mid + 1 else high = mid
        }
        return low
    }

    /** Unit index at which group [i] starts. */
    private fun unitStartOf(i: Int): Int = starts[i] - collapsedBefore[i]

    /** Index of the group whose unit index is exactly [unit], or -1. */
    private fun groupAtUnit(unit: Int): Int {
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val midUnit = unitStartOf(mid)
            when {
                midUnit == unit -> return mid
                midUnit < unit -> low = mid + 1
                else -> high = mid - 1
            }
        }
        return -1
    }

    /** Number of groups whose unit index is < [unit]. */
    private fun groupsBeforeUnit(unit: Int): Int {
        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (unitStartOf(mid) < unit) low = mid + 1 else high = mid
        }
        return low
    }

    internal companion object {
        fun identity(itemsCount: Int): RowUnitIndex =
            RowUnitIndex(
                starts = IntArray(0),
                ends = IntArray(0),
                collapsedBefore = IntArray(1),
                unitCount = itemsCount.coerceAtLeast(0),
            )

        fun of(
            itemsCount: Int,
            groups: List<IntRange>,
        ): RowUnitIndex {
            val size = groups.size
            val starts = IntArray(size)
            val ends = IntArray(size)
            val collapsedBefore = IntArray(size + 1)
            var previousEnd = -1
            var collapsed = 0
            groups.forEachIndexed { i, range ->
                require(!range.isEmpty()) { "row block ranges must not be empty, got $range" }
                require(range.first > previousEnd) {
                    "row block ranges must be sorted and non-overlapping, got $range after row $previousEnd"
                }
                require(range.first >= 0 && range.last < itemsCount) {
                    "row block range $range is out of bounds for $itemsCount rows"
                }
                starts[i] = range.first
                ends[i] = range.last
                collapsedBefore[i] = collapsed
                collapsed += range.last - range.first
                previousEnd = range.last
            }
            collapsedBefore[size] = collapsed
            return RowUnitIndex(starts, ends, collapsedBefore, itemsCount - collapsed)
        }
    }
}

/**
 * Builds a [RowUnitIndex] for [itemsCount] rows. Returns the identity index when [groups] is null or
 * empty, so tables without groups keep their existing behavior with no extra work.
 */
internal fun buildRowUnitIndex(
    itemsCount: Int,
    groups: List<IntRange>?,
): RowUnitIndex =
    if (groups.isNullOrEmpty() || itemsCount <= 0) {
        RowUnitIndex.identity(itemsCount)
    } else {
        RowUnitIndex.of(itemsCount, groups)
    }
