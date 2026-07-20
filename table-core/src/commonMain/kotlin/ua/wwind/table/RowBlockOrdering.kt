package ua.wwind.table

/**
 * Sorts rows within each block while keeping every block whole — a plain `sortedBy` fragments
 * blocks into duplicate bands. Each unit — a block or a standalone row — takes its position from
 * its minimal member under [comparator], so the list still reads as sorted at unit granularity.
 * The sort is stable both within a block and between units: ties keep their existing order, and
 * re-applying the same sort changes nothing.
 *
 * With drag enabled, apply this one-shot to the SOURCE list — rewriting it — never as a persistent
 * render-time projection. Unit order here follows [comparator], not drag history (ties excepted),
 * so a live projection re-pins unit order after every committed block move, making the move
 * invisible in the rendered list — the same theorem that rules out composing a free sort with drag,
 * replayed at unit granularity.
 */
public fun <T> List<T>.sortedWithinRowBlocks(
    blockOf: (T) -> Any?,
    comparator: Comparator<in T>,
): List<T> =
    rowBlockRuns(blockOf)
        .map { run -> run.sortedWith(comparator) }
        .sortedWith(compareBy(comparator) { it.first() })
        .flatten()

/**
 * Filters at block granularity: a block survives in full when ANY member matches, so the rendered
 * blocks never show partially. The plain row-level filter stays first-class — hidden members simply
 * travel with their block on a drag — this helper exists for consumers who want match-whole-block
 * semantics instead.
 */
public fun <T> List<T>.filteredWholeRowBlocks(
    blockOf: (T) -> Any?,
    predicate: (T) -> Boolean,
): List<T> =
    rowBlockRuns(blockOf)
        .filter { run -> run.any(predicate) }
        .flatten()

/**
 * Splits the list into drag units by the same rule the table renders with: a maximal run of
 * adjacent rows sharing a non-null block id is one unit, every other row is a singleton. Adjacency,
 * not global grouping — a fragmented id (out-of-contract input) stays fragmented rather than being
 * silently reordered into one place.
 */
private fun <T> List<T>.rowBlockRuns(blockOf: (T) -> Any?): List<List<T>> {
    val runs = mutableListOf<MutableList<T>>()
    var runId: Any? = null
    for (item in this) {
        val id = blockOf(item)
        if (id != null && id == runId) {
            runs.last() += item
        } else {
            runs += mutableListOf(item)
            runId = id
        }
    }
    return runs
}
