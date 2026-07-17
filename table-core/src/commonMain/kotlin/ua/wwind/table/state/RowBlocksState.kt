package ua.wwind.table.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowBlocks

/** A maximal run of adjacent rows sharing one non-null block id; [rows] is in view (rendered) space. */
internal class RowBlockRun(
    val id: Any,
    val rows: IntRange,
)

/**
 * The managed side of [RowBlocks]: owns the view permutation a drag gesture mutates, so the reorder
 * engine's synchronous-layout expectation is satisfied inside the library instead of being imposed
 * on the consumer (whose pipeline is typically asynchronous and cannot apply moves mid-gesture).
 *
 * The upstream list is what the consumer renders; the view order is a permutation over it that is
 * identity except between a drag's first swap and the moment the consumer's applied move flows back
 * through [reconcile]. Block runs are re-derived from the permuted order on every change, so unit
 * boundaries can never disagree with what is on screen.
 *
 * Backed by snapshot state throughout — the order and the gesture fields alike — so composition
 * that reads [runs], [units], [itemAt] or [isDragActive] recomposes on change, and a rolled-back
 * snapshot cannot leave a gesture half-cancelled.
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

    /** [viewOrder] at gesture start; [settle] emits nothing when the gesture ends back on it. */
    private var preGestureOrder: List<Int>? by mutableStateOf(null)

    /** Set when [reconcile] cancels an in-flight gesture; the engine's late swaps for that dead
     *  gesture are dropped until the pointer goes up and [settle] releases the latch. */
    private var cancelledUntilSettle: Boolean by mutableStateOf(false)

    /**
     * Bumps once per refused drop (paged policy in [settle]). A refusal restores [viewOrder], so
     * over any list derived from it the whole gesture nets to zero — invisible to the embedded
     * engine, which clears the drag offsets left over from a drop only when its input list
     * compares unequal. Folding this count into that list's equality is what turns a refusal into
     * the rebuild that snaps the view back with the model.
     */
    var refusedDropCount: Int by mutableStateOf(0)
        private set

    /** Ids already reported as fragmented — the warning names each offender once, not per snapshot. */
    private val warnedFragmentedIds = mutableSetOf<Any>()

    private val derived: Derived by derivedStateOf { derive() }

    val itemsCount: Int get() = viewOrder.size

    val isDragActive: Boolean get() = draggedRows != null

    /** Block runs over the current view order, for band rendering and leader detection. */
    val runs: List<RowBlockRun> get() = derived.runs

    /** Unit index over the current view order; drives the lazy items and the engine's unit space. */
    val units: RowUnitIndex get() = derived.units

    /**
     * The current derived view as one atomic object. A drag's lazy items must read their unit
     * boundaries, item, block id and key all from this single value; mixing it with the live
     * [itemAt]/[units] getters reintroduces the mid-gesture key churn it exists to prevent.
     */
    val snapshot: Derived get() = derived

    /**
     * Whether the current order holds at least one block. Derived separately from [runs] so a
     * reader that only cares about presence (the column menu's group-by lock) invalidates when
     * presence flips, not on every permutation that rebuilds the run list.
     */
    val hasBlocks: Boolean by derivedStateOf { derived.runs.isNotEmpty() }

    /**
     * Out-of-range probes answer with a placeholder instead of throwing: effects and prefetchers
     * can run one frame behind a reconcile that shrank the list, and crashing on that lag is the
     * v1 failure mode this design exists to close.
     */
    fun itemAt(viewIndex: Int): T? {
        val order = viewOrder
        if (viewIndex !in order.indices) return null
        return upstream[order[viewIndex]]
    }

    /** Upstream index rendered at [viewIndex]; stale probes fall back to the raw index — the same
     *  argument the consumer's rowKey would receive from a table without blocks. */
    fun upstreamIndexOf(viewIndex: Int): Int = viewOrder.getOrElse(viewIndex) { viewIndex }

    /** Block id of the row at [viewIndex]; null for standalone rows, placeholders and stale probes. */
    fun blockIdAt(viewIndex: Int): Any? {
        val order = viewOrder
        if (viewIndex !in order.indices) return null
        return upstream[order[viewIndex]]?.let(config.blockOf)
    }

    /**
     * How the active gesture has displaced view positions so far: pre-gesture position -> current
     * position; null when no gesture is active or nothing moved. Must be read BEFORE [settle] —
     * settling forgets the pre-gesture order — and applied only when the settle actually commits:
     * a refused drop snaps the view back, so the displacement read here no longer describes it.
     *
     * This is what lets positional runtime state (selection, editing, cached row heights) follow
     * the rows it pointed at across the drop. Total over any [Int]: stale positions that survived
     * from an older list pass through unmapped rather than crash the commit.
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
     * Feeds the current snapshot of the consumer's data. A changed list resets the permutation to
     * identity — cancelling an in-flight gesture, because swap geometry computed against the old
     * list is meaningless over the new one; the engine's remaining swaps for that gesture are then
     * dropped until [settle] — while an unchanged list keeps the optimistic post-drop order until
     * the consumer's applied move (or any other change) arrives.
     *
     * One change survives a gesture: a placeholder fill-in (rows appear where nulls sat, nothing
     * else touched). That is a paged source loading pages under the held pointer — positions do not
     * move, so the permutation and the engine's swap geometry stay exactly as valid as before, and
     * cancelling would make every drop near an unloaded region impossible: the anchors there only
     * resolve BECAUSE the hold lets their page arrive. An eviction (a loaded row turning back into
     * a placeholder) is not symmetric — the row's key and block membership vanish under the engine
     * — so it cancels like any other change.
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

    /** True when [newUpstream] only fills previously unloaded positions: same size, and every row
     *  that was already loaded is unchanged — the one list change that moves nothing. */
    private fun isPlaceholderFillIn(newUpstream: List<T?>): Boolean {
        val old = upstream
        if (newUpstream.size != old.size) return false
        for (index in old.indices) {
            val loaded = old[index] ?: continue
            if (loaded != newUpstream[index]) return false
        }
        return true
    }

    /**
     * Applies one engine swap to the view permutation, synchronously — by the time the engine
     * re-reads layout the order already matches its prediction. [fromUnit] is always the dragged
     * unit: the engine moves nothing else, so the first call pins down what [settle] will report.
     *
     * Swaps that cannot apply are dropped, not rejected: after [reconcile] cancels a gesture, the
     * engine — whose geometry was computed against the previous frame's layout — can still deliver
     * swaps for it, out of bounds over the new order or in bounds but belonging to the dead
     * gesture. Throwing here would crash mid-drag on any consumer whose pipeline updates the list
     * asynchronously; dropping is what the documented cancel-on-external-change policy means.
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
     * Ends the gesture and emits its net result — at most one [RowBlockMove] per gesture, none when
     * the unit ended where it started. After a commit the permuted order is kept (no snap-back):
     * the consumer's applied move arrives through [reconcile] and takes over seamlessly.
     *
     * Paged drop policy: a placeholder cannot anchor a move — its key does not exist yet, and a
     * made-up key would place the block against a row the user never saw. When a needed landing
     * neighbour is still unloaded the gesture cancels and the view snaps back to its pre-gesture
     * order; nothing is emitted. Holding the drag over the landing spot is the natural retry —
     * rendering the placeholders there is what makes their page load.
     */
    fun settle(): RowBlockMove? {
        val dragged = draggedRows
        val before = preGestureOrder
        cancelledUntilSettle = false
        draggedRows = null
        preGestureOrder = null
        if (dragged == null || before == null || viewOrder == before) return null
        val order = viewOrder
        // The dragged unit's extent NOW, not at gesture start: a page loaded mid-gesture can
        // reveal members that merged into the dragged run and landed with it — reporting the
        // stale extent would anchor the move on a row inside its own block.
        val leaderView = order.indexOf(dragged.first())
        if (leaderView < 0) return null
        val units = derived.units
        val rows = units.rowsOf(units.unitOf(leaderView))
        val first = rows.first
        val last = rows.last
        val afterAnchorLoaded = first == 0 || upstream[order[first - 1]] != null
        val beforeAnchorLoaded = last == order.lastIndex || upstream[order[last + 1]] != null
        if (!afterAnchorLoaded || !beforeAnchorLoaded) {
            viewOrder = before
            refusedDropCount++
            return null
        }
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

    private fun keyAt(upstreamIndex: Int): Any = rowKey(upstream[upstreamIndex], upstreamIndex)

    private fun derive(): Derived {
        val order = viewOrder
        // Freeze the item at every view position from the SAME order/upstream read the runs and
        // units are built over. This list is what makes [Derived] one atomic snapshot: a lazy item's
        // key, item and unit all resolve against it, never against a viewOrder that moved on.
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
     * A block id split across disjoint runs means something upstream reordered block members apart —
     * a foreign sort or a member-splitting filter. Detection only: the library cannot see the source
     * list to prevent it.
     *
     * Checked at [reconcile], not inside [derive]: the derived computation must stay free of side
     * effects (it can run speculatively in a snapshot that is never applied, or concurrently from
     * several readers), and a unit move can only merge runs — never split them — so the consumer's
     * own order is the only place fragmentation can appear.
     *
     * Placeholders (null items) are skipped, unlike in [derive]: a not-yet-loaded row breaks the
     * rendered band but proves nothing about the upstream order — it may well be an unloaded member
     * of the surrounding block, a state paging reaches routinely — so it neither continues nor
     * finishes a run here. Once the row loads, a real foreign member warns on the next reconcile.
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
     * One atomic snapshot of the permuted view: [runs] and [units] for the layout, plus the per-view-
     * position [items] frozen from the same [viewOrder]/[upstream] read. Every lookup a lazy item
     * needs — its item, block id and key — resolves against this one object, so a reader holding a
     * [snapshot] can never see [units] from one permutation and an item from the next. That temporal
     * split is what tore the dragged block's key — and the drag handle inside it — out of composition
     * mid-gesture, which the reorder engine reads as the drag ending.
     */
    internal inner class Derived(
        val runs: List<RowBlockRun>,
        val units: RowUnitIndex,
        /** Item at each view position, frozen from the order this was derived over. */
        val items: List<T?>,
        /** View position -> upstream index, frozen — supplies the key's index argument. */
        private val order: List<Int>,
    ) {
        fun itemAt(viewIndex: Int): T? = items.getOrNull(viewIndex)

        fun blockIdAt(viewIndex: Int): Any? = items.getOrNull(viewIndex)?.let(config.blockOf)

        /**
         * The same key [Table]'s effectiveRowKey produces — rowKey(item, upstreamIndex) — but read
         * off this frozen snapshot instead of live state, so a unit's boundary and its key are always
         * drawn from one permutation. Out-of-range probes fall back to the raw index, matching the
         * consumer's rowKey contract for a table without blocks.
         */
        fun keyAt(viewIndex: Int): Any =
            rowKey(items.getOrNull(viewIndex), order.getOrElse(viewIndex) { viewIndex })
    }
}

/**
 * Moves the rows in [from] so that the block swaps with [to]: moving down lands the block after
 * [to], moving up lands it before [to] — mirroring the reorder engine's swap semantics so a unit
 * move translates to one call.
 *
 * Both ranges are interpreted against the list's current contents. Overlapping ranges have no
 * meaningful swap semantics and are rejected; identical ranges are a no-op.
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
