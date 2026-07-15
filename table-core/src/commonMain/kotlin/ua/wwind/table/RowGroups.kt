package ua.wwind.table

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Derives drag-unit ranges from a flat row list: every run of **adjacent** rows sharing the same
 * non-null group id becomes one [IntRange]. Rows whose id is `null` are never grouped.
 *
 * Two disjoint runs of the same id produce two ranges — a group is defined by adjacency, because a
 * drag unit must be contiguous on screen.
 *
 * The result must be recomputed from the same data snapshot the table renders, e.g.
 * `remember(rows) { rows.rowGroupsOf { it.groupId } }`. Group ranges are derived state, not state to
 * maintain by hand.
 */
public fun <T> List<T>.rowGroupsOf(groupIdOf: (T) -> Any?): ImmutableList<IntRange> {
    val groups = mutableListOf<IntRange>()
    var runStart = -1
    var runId: Any? = null
    for (index in indices) {
        val id = groupIdOf(this[index])
        if (id != null && id == runId) continue
        if (runStart >= 0) groups += runStart..index - 1
        if (id != null) {
            runStart = index
            runId = id
        } else {
            runStart = -1
            runId = null
        }
    }
    if (runStart >= 0) groups += runStart..lastIndex
    return groups.toImmutableList()
}

/**
 * Moves the rows in [from] so that they swap with [to], matching the semantics of the table's
 * [TableRowGroups.onMove] callback: moving down lands the block after [to], moving up lands it
 * before [to].
 *
 * Both ranges are interpreted against the list's current contents.
 */
public fun <T> MutableList<T>.moveRowGroup(
    from: IntRange,
    to: IntRange,
) {
    require(!from.isEmpty() && !to.isEmpty()) { "moveRowGroup ranges must not be empty" }
    require(from.first >= 0 && from.last < size) { "from range $from is out of bounds for size $size" }
    require(to.first >= 0 && to.last < size) { "to range $to is out of bounds for size $size" }
    if (from.first == to.first) return

    val blockSize = from.last - from.first + 1
    val block = ArrayList<T>(blockSize)
    repeat(blockSize) { block += removeAt(from.first) }
    val insertAt = if (to.first > from.first) to.last - blockSize + 1 else to.first
    addAll(insertAt.coerceIn(0, size), block)
}
