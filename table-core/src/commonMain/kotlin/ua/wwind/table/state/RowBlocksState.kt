package ua.wwind.table.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowBlocks
import ua.wwind.table.RowWithinBlockMove

/** A maximal run of adjacent rows sharing one non-null block id; [rows] is in view space. */
internal class RowBlockRun(
    val id: Any,
    val rows: IntRange,
)

/**
 * Owns the view permutation a drag mutates, so the reorder engine's synchronous-layout expectation
 * is met inside the library rather than imposed on the consumer's asynchronous pipeline.
 * Snapshot-backed throughout, so a rolled-back snapshot cannot leave a gesture half-cancelled.
 */
internal class RowBlocksState<T : Any>(
    val config: RowBlocks<T>,
    private val rowKey: (item: T?, index: Int) -> Any,
    private val warn: (String) -> Unit = { Logger.w { it } },
) {
    /** The consumer's rendered list, in its own order. */
    private var upstream: List<T?> by mutableStateOf(emptyList())

    /** View position -> upstream index. Identity outside a gesture / optimistic hold. */
    private var viewOrder: List<Int> by mutableStateOf(emptyList())

    /** Upstream indices of the dragged unit; non-null exactly while a gesture is in progress. */
    private var draggedRows: List<Int>? by mutableStateOf(null)

    /** [viewOrder] at gesture start; a gesture ending back on it emits nothing. */
    private var preGestureOrder: List<Int>? by mutableStateOf(null)

    /** Drops the engine's late swaps for a gesture [reconcile] already cancelled. */
    private var cancelledUntilSettle: Boolean by mutableStateOf(false)

    /**
     * Folded into the embedded engine's list equality: a refused drop nets to zero over that list,
     * so only this count forces the rebuild that clears its leftover drag offsets.
     */
    var refusedDropCount: Int by mutableIntStateOf(0)
        private set

    /** Each offending id warns once, not per snapshot. */
    private val warnedFragmentedIds = mutableSetOf<Any>()

    private val derived: Derived by derivedStateOf { derive() }

    val itemsCount: Int get() = viewOrder.size

    val isDragActive: Boolean get() = draggedRows != null

    val runs: List<RowBlockRun> get() = derived.runs

    val units: RowUnitIndex get() = derived.units

    /** The derived view as one atomic value; a drag reads unit, item, block id and key from it. */
    val snapshot: Derived get() = derived

    /** Separate from [runs] so presence-only readers invalidate on flips, not on every permutation. */
    val hasBlocks: Boolean by derivedStateOf { derived.runs.isNotEmpty() }

    /** Null for stale probes: effects can run a frame behind a reconcile that shrank the list. */
    fun itemAt(viewIndex: Int): T? {
        val order = viewOrder
        if (viewIndex !in order.indices) return null
        return upstream[order[viewIndex]]
    }

    /** Stale probes fall back to the raw index — what a table without blocks would pass to rowKey. */
    fun upstreamIndexOf(viewIndex: Int): Int = viewOrder.getOrElse(viewIndex) { viewIndex }

    /** Null for standalone rows, placeholders and stale probes. */
    fun blockIdAt(viewIndex: Int): Any? {
        val order = viewOrder
        if (viewIndex !in order.indices) return null
        return upstream[order[viewIndex]]?.let(config.blockOf)
    }

    /**
     * Pre-gesture position -> current position, so positional state (selection, editing, cached row
     * heights) follows its rows across the drop. Read BEFORE settling, and apply only on a commit:
     * a refused drop snaps the view back and this no longer describes it.
     */
    fun gestureRemap(): ((Int) -> Int)? {
        val before = preGestureOrder ?: return null
        val order = viewOrder
        if (before == order) return null
        val currentPositions = HashMap<Int, Int>(order.size)
        order.forEachIndexed { viewIndex, upstreamIndex -> currentPositions[upstreamIndex] = viewIndex }
        return remap@{ position ->
            val upstreamIndex = before.getOrNull(position) ?: return@remap position
            currentPositions[upstreamIndex] ?: position
        }
    }

    /**
     * Feeds the consumer's current data. A changed list resets the permutation and cancels an
     * in-flight gesture — swap geometry computed against the old list is meaningless over the new
     * one. A placeholder fill-in is the exception: it moves nothing, and cancelling on it would make
     * every drop near an unloaded region impossible.
     */
    fun reconcile(
        itemsCount: Int,
        itemAt: (Int) -> T?,
    ) {
        val newUpstream = List(itemsCount.coerceAtLeast(0)) { itemAt(it) }
        if (newUpstream == upstream) return
        if (isPlaceholderFillIn(newUpstream)) {
            upstream = newUpstream
            warnAboutFragmentedIds(newUpstream)
            return
        }
        if (draggedRows != null) cancelledUntilSettle = true
        upstream = newUpstream
        viewOrder = List(newUpstream.size) { it }
        draggedRows = null
        preGestureOrder = null
        warnAboutFragmentedIds(newUpstream)
    }

    /** Same size and every already-loaded row unchanged — the one list change that moves nothing. */
    private fun isPlaceholderFillIn(newUpstream: List<T?>): Boolean {
        val old = upstream
        if (newUpstream.size != old.size) return false
        return old.indices.all { index -> old[index] == null || old[index] == newUpstream[index] }
    }

    /**
     * Applies one engine swap synchronously, so the order already matches the engine's prediction by
     * the time it re-reads layout. Swaps that cannot apply are dropped rather than thrown: a
     * cancelled gesture can still deliver swaps computed against the previous frame.
     */
    fun applyUnitMove(
        fromUnit: Int,
        toUnit: Int,
    ) {
        if (cancelledUntilSettle) return
        val units = derived.units
        if (fromUnit !in 0 until units.unitCount || toUnit !in 0 until units.unitCount) return
        if (draggedRows == null) {
            preGestureOrder = viewOrder
            draggedRows = units.rowsOf(fromUnit).map { viewOrder[it] }
        }
        val fromRows = units.rowsOf(fromUnit)
        val toRows = units.rowsOf(toUnit)
        viewOrder = viewOrder.toMutableList().also { it.moveRowGroup(fromRows, toRows) }
    }

    /**
     * Ends the gesture and emits at most one [RowBlockMove]; the permuted order is kept so the
     * consumer's applied move takes over seamlessly. A placeholder cannot anchor a move, so a drop
     * whose landing neighbour is unloaded is refused and the view snaps back.
     */
    fun settle(): RowBlockMove? {
        val (dragged, before) = endGesture() ?: return null
        // The extent NOW, not at gesture start: a page loaded mid-gesture can merge members into the
        // dragged run, and the stale extent would anchor the move inside the block itself.
        val leaderView = viewOrder.indexOf(dragged.first()).takeIf { it >= 0 } ?: return null
        val order = viewOrder
        val units = derived.units
        val rows = units.rowsOf(units.unitOf(leaderView))
        val first = rows.first
        val last = rows.last
        val afterAnchorLoaded = isAnchorLoaded(if (first == 0) null else first - 1, order)
        val beforeAnchorLoaded = isAnchorLoaded(if (last == order.lastIndex) null else last + 1, order)
        if (!afterAnchorLoaded || !beforeAnchorLoaded) return refuseDrop(before)
        val move =
            RowBlockMove(
                blockId = upstream[dragged.first()]?.let(config.blockOf),
                movedKeys = (first..last).map { keyAt(order[it]) },
                afterKey = if (first == 0) null else keyAt(order[first - 1]),
                beforeKey = if (last == order.lastIndex) null else keyAt(order[last + 1]),
            )
        config.onCommit?.invoke(move)
        return move
    }

    /**
     * Moves the dragged row between two view positions inside the SAME run. Endpoints outside one
     * run are dropped — a defensive guard, since the nested engine cannot generate them. The moved
     * row keeps its block id, so [derive] recomputes an identical run and the block never fragments.
     */
    fun applyRowMoveWithinBlock(
        fromView: Int,
        toView: Int,
    ) {
        if (cancelledUntilSettle) return
        val order = viewOrder
        if (fromView !in order.indices || toView !in order.indices) return
        val units = derived.units
        val fromUnit = units.unitOf(fromView)
        if (!units.isGroup(fromUnit) || units.unitOf(toView) != fromUnit) return
        if (draggedRows == null) {
            preGestureOrder = viewOrder
            draggedRows = listOf(order[fromView])
        }
        viewOrder = viewOrder.toMutableList().also { it.moveRowGroup(fromView..fromView, toView..toView) }
    }

    /**
     * Ends a within-block gesture and emits at most one [RowWithinBlockMove], anchored on the moved
     * row's neighbours inside its run. Same paged refusal as [settle].
     *
     * Five of the exits are the ways a within-block drop can fail to name a move — stale probe, unit
     * that is no longer a group, unloaded anchor, row without a block id — and each is read on its
     * own, not as a step in a chain. [settle] fits under the limit only because whole-block anchoring
     * has fewer failure modes; nesting these would bury the emit under four levels of indentation.
     */
    @Suppress("ReturnCount")
    fun settleWithinBlock(): RowWithinBlockMove? {
        val (dragged, before) = endGesture() ?: return null
        val order = viewOrder
        val movedUpstream = dragged.first()
        val movedView = order.indexOf(movedUpstream)
        if (movedView < 0) return null
        val units = derived.units
        val unit = units.unitOf(movedView)
        if (!units.isGroup(unit)) return null
        val runRows = units.rowsOf(unit)
        val afterView = if (movedView > runRows.first) movedView - 1 else null
        val beforeView = if (movedView < runRows.last) movedView + 1 else null
        if (!isAnchorLoaded(afterView, order) || !isAnchorLoaded(beforeView, order)) {
            return refuseDrop(before)
        }
        val blockId = upstream[movedUpstream]?.let(config.blockOf) ?: return null
        val move =
            RowWithinBlockMove(
                blockId = blockId,
                movedKey = keyAt(movedUpstream),
                afterKey = afterView?.let { keyAt(order[it]) },
                beforeKey = beforeView?.let { keyAt(order[it]) },
            )
        config.onRowReorderWithinBlock?.invoke(move)
        return move
    }

    /**
     * Tears the gesture down and reports the rows it dragged together with the order it started
     * from, or null when there was no effective gesture to settle.
     *
     * The teardown happens before the null check on purpose: a gesture that produced no move still
     * has to release its state, or the next drag would inherit it.
     */
    private fun endGesture(): Pair<List<Int>, List<Int>>? {
        val dragged = draggedRows
        val before = preGestureOrder
        cancelledUntilSettle = false
        draggedRows = null
        preGestureOrder = null
        if (dragged == null || before == null || viewOrder == before) return null
        return dragged to before
    }

    /**
     * Whether the row a move would anchor on is loaded. A null [view] means there is no neighbour
     * on that side — the run touches the end of the list, which anchors fine.
     */
    private fun isAnchorLoaded(
        view: Int?,
        order: List<Int>,
    ): Boolean = view == null || upstream[order[view]] != null

    /**
     * Snaps the view back to [before] and counts the refusal. A placeholder cannot anchor a move, so
     * a drop landing next to an unloaded row is rejected outright rather than committed blind.
     */
    private fun refuseDrop(before: List<Int>): Nothing? {
        viewOrder = before
        refusedDropCount++
        return null
    }

    private fun keyAt(upstreamIndex: Int): Any = rowKey(upstream[upstreamIndex], upstreamIndex)

    private fun derive(): Derived {
        val order = viewOrder
        // Frozen from the same order/upstream read as the runs below — that is what makes [Derived]
        // one atomic snapshot.
        val items = List(order.size) { upstream[order[it]] }
        val runs = mutableListOf<RowBlockRun>()
        var runStart = -1
        var runId: Any? = null
        for (viewIndex in order.indices) {
            val id = items[viewIndex]?.let(config.blockOf)
            if (id != null && id == runId) continue
            if (runStart >= 0) runs += RowBlockRun(requireNotNull(runId), runStart..viewIndex - 1)
            if (id != null) {
                runStart = viewIndex
                runId = id
            } else {
                runStart = -1
                runId = null
            }
        }
        if (runStart >= 0) runs += RowBlockRun(requireNotNull(runId), runStart..order.lastIndex)
        return Derived(runs, buildRowUnitIndex(order.size, runs.map { it.rows }), items, order)
    }

    /**
     * Warns when one block id occupies disjoint runs — evidence of a foreign sort or a
     * member-splitting filter. Runs at [reconcile], not in [derive], which must stay side-effect
     * free; placeholders are skipped because an unloaded row proves nothing about upstream order.
     */
    private fun warnAboutFragmentedIds(items: List<T?>) {
        val finishedRuns = mutableSetOf<Any>()
        var runId: Any? = null
        for (item in items) {
            if (item == null) continue
            val id = config.blockOf(item)
            if (id == runId) continue
            runId?.let { finishedRuns += it }
            runId = id
            if (id != null && id in finishedRuns && warnedFragmentedIds.add(id)) {
                warn(
                    "Row block id $id appears in non-adjacent runs; blocks are defined by " +
                        "adjacency — check for a foreign sort or a filter splitting block members",
                )
            }
        }
    }

    /**
     * One atomic snapshot of the permuted view. Every lookup a lazy item needs resolves against this
     * object, so no reader can mix units from one permutation with an item from the next — the split
     * that used to tear the dragged block's key out of composition mid-gesture.
     */
    internal inner class Derived(
        val runs: List<RowBlockRun>,
        val units: RowUnitIndex,
        val items: List<T?>,
        /** View position -> upstream index, frozen — supplies the key's index argument. */
        private val order: List<Int>,
    ) {
        fun itemAt(viewIndex: Int): T? = items.getOrNull(viewIndex)

        fun blockIdAt(viewIndex: Int): Any? = items.getOrNull(viewIndex)?.let(config.blockOf)

        /** The same key the table's effectiveRowKey produces, but read off this frozen snapshot. */
        fun keyAt(viewIndex: Int): Any = rowKey(items.getOrNull(viewIndex), order.getOrElse(viewIndex) { viewIndex })
    }
}

/**
 * Moves [from] so the block swaps with [to]: down lands it after [to], up lands it before — the
 * reorder engine's own swap semantics. Overlapping ranges are rejected; identical ranges are a no-op.
 */
internal fun <T> MutableList<T>.moveRowGroup(
    from: IntRange,
    to: IntRange,
) {
    require(!from.isEmpty() && !to.isEmpty()) { "moveRowGroup ranges must not be empty" }
    require(from.first >= 0 && from.last < size) { "from range $from is out of bounds for size $size" }
    require(to.first >= 0 && to.last < size) { "to range $to is out of bounds for size $size" }
    if (from == to) return
    require(from.last < to.first || to.last < from.first) {
        "moveRowGroup ranges must not overlap, got from=$from and to=$to"
    }

    val blockSize = from.last - from.first + 1
    val block = ArrayList<T>(blockSize)
    repeat(blockSize) { block += removeAt(from.first) }
    val insertAt = if (to.first > from.first) to.last - blockSize + 1 else to.first
    addAll(insertAt.coerceIn(0, size), block)
}
