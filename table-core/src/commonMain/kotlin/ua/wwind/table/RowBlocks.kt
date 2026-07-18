package ua.wwind.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Row blocks: runs of adjacent rows that render and drag as one unit, declared by identity rather
 * than by position — the library derives block extents from the same snapshot it renders, so they
 * cannot go stale against an asynchronously filtered list. The consumer's data stays untouched
 * during a gesture; the move arrives as one event at drop.
 *
 * Passing a `RowBlocks` supersedes the per-row `onRowMove`: drag units are blocks, so every unit
 * gesture reports through [onCommit] instead (a standalone move carries a null
 * [RowBlockMove.blockId]). Dragging is split in two — the whole block moves from a handle in
 * [blockHeader], while each row moves within its own block from its cell handle.
 *
 * Hold the instance in `remember`: it compares by identity, so a fresh instance per recomposition
 * stops the table from skipping. A stable `rowKey` is required — move anchors are row keys.
 */
@Stable
public class RowBlocks<T : Any>(
    /** Block identity; null = standalone row. Adjacent equal ids form one block. */
    public val blockOf: (T) -> Any?,
    /** One event per completed whole-block drag; null disables whole-block and standalone drag. */
    public val onCommit: ((RowBlockMove) -> Unit)? = null,
    /** Band content above a block; a `draggableHandle()` in its [RowBlockHeaderScope] drags the
     *  whole block. A block with no header cannot be dragged as a whole. */
    public val blockHeader: (
        @Composable context(RowBlockHeaderScope) (blockId: Any, rows: IntRange) -> Unit
    )? = null,
    /** One event per within-block row reorder; null disables it. The row never leaves its block. */
    public val onRowReorderWithinBlock: ((RowWithinBlockMove) -> Unit)? = null,
)

/**
 * The result of one completed block-drag gesture, expressed in stable row keys so it stays
 * meaningful however the consumer's pipeline reorders or filters the source afterwards.
 *
 * Anchors are keys of the rendered (possibly filtered) list. [beforeKey] is deliberately redundant:
 * when [afterKey] is hidden or gone by the time the consumer applies the move, the second anchor
 * still pins the destination.
 */
public class RowBlockMove(
    /** Block id of the dragged unit; null when a standalone row moved. */
    public val blockId: Any?,
    /** Keys of the VISIBLE moved rows, in order. */
    public val movedKeys: List<Any>,
    /** Key of the row the unit now sits after, in the rendered order; null = start. */
    public val afterKey: Any?,
    /** Key of the row the unit now sits before; null = end. Redundant anchor for edge cases. */
    public val beforeKey: Any?,
)

/**
 * The result of one completed within-block drag gesture: a single row moved among its block-mates,
 * expressed in stable keys. Anchors are the moved row's visible neighbours inside the same block; a
 * null anchor means the block's edge (first/last visible row).
 */
public class RowWithinBlockMove(
    /** Block the row belongs to (unchanged by the move). */
    public val blockId: Any,
    /** Key of the moved row. */
    public val movedKey: Any,
    /** Key of the visible block-mate the row now sits after; null = block start. */
    public val afterKey: Any?,
    /** Key of the visible block-mate the row now sits before; null = block end. Redundant anchor. */
    public val beforeKey: Any?,
)

/**
 * Applies [move] to the SOURCE list — the lift from the rendered, possibly filtered view back to the
 * consumer's source of truth.
 *
 * Relocates ALL rows whose [blockOf] equals [RowBlockMove.blockId], including rows the filter hides
 * and [RowBlockMove.movedKeys] cannot name, preserving their relative order; the insertion point
 * expands to whole-block boundaries so no other block is split. Standalone moves relocate just the
 * moved keys. [RowBlockMove.afterKey] is the primary anchor and [RowBlockMove.beforeKey] the
 * fallback; with neither resolvable the list is left untouched rather than guessing a position.
 */
public fun <T> MutableList<T>.applyRowBlockMove(
    move: RowBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
) {
    val belongsToMove: (T) -> Boolean =
        when (val blockId = move.blockId) {
            null -> {
                val movedKeys = move.movedKeys.toSet()
                ({ item -> keyOf(item) in movedKeys })
            }
            else -> ({ item -> blockOf(item) == blockId })
        }
    val moved = filter(belongsToMove)
    if (moved.isEmpty()) return
    val rest = filterNot(belongsToMove)
    val insertAt = rowBlockInsertionIndex(rest, move, keyOf, blockOf) ?: return
    clear()
    addAll(rest.subList(0, insertAt))
    addAll(moved)
    addAll(rest.subList(insertAt, rest.size))
}

/**
 * Destination in [rest], or null when neither anchor resolves. Each anchor expands over its own
 * hidden block-mates — forward past `afterKey`'s, backward before `beforeKey`'s — so the filtered
 * projection shows the unit exactly where the drag put it.
 */
private fun <T> rowBlockInsertionIndex(
    rest: List<T>,
    move: RowBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
): Int? {
    val afterKey = move.afterKey ?: return 0
    val afterAnchor = rest.indexOfFirst { keyOf(it) == afterKey }
    if (afterAnchor >= 0) {
        var end = afterAnchor
        val anchorBlock = blockOf(rest[end])
        if (anchorBlock != null) {
            while (end + 1 < rest.size && blockOf(rest[end + 1]) == anchorBlock) end++
        }
        return end + 1
    }
    val beforeKey = move.beforeKey ?: return rest.size
    val beforeAnchor = rest.indexOfFirst { keyOf(it) == beforeKey }
    if (beforeAnchor < 0) return null
    var start = beforeAnchor
    val anchorBlock = blockOf(rest[start])
    if (anchorBlock != null) {
        while (start > 0 && blockOf(rest[start - 1]) == anchorBlock) start--
    }
    return start
}

/**
 * Lifts a [RowWithinBlockMove] to the SOURCE list: relocates the single moved row among its
 * block-mates, using [RowWithinBlockMove.afterKey] as the primary anchor and [beforeKey] as the
 * fallback. Block members hidden by the current filter keep their relative order; the moved row
 * lands immediately adjacent to the resolved visible anchor. The list is left untouched when the
 * moved row is gone, its block id no longer matches, or neither anchor resolves.
 */
public fun <T> MutableList<T>.applyRowReorderWithinBlock(
    move: RowWithinBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
) {
    val movedIndex = indexOfFirst { keyOf(it) == move.movedKey }
    if (movedIndex < 0) return
    if (blockOf(this[movedIndex]) != move.blockId) return
    val moving = removeAt(movedIndex)
    val insertAt = withinBlockInsertionIndex(this, move, keyOf, blockOf)
    if (insertAt == null) {
        add(movedIndex, moving)
        return
    }
    add(insertAt.coerceIn(0, size), moving)
}

/**
 * Destination in [rest] for a within-block move, or null when neither anchor resolves. A null
 * anchor means the block's edge, so the row lands at its first / after its last source member.
 */
private fun <T> withinBlockInsertionIndex(
    rest: List<T>,
    move: RowWithinBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
): Int? {
    val afterKey = move.afterKey
    if (afterKey != null) {
        val i = rest.indexOfFirst { keyOf(it) == afterKey }
        if (i >= 0) return i + 1
    } else {
        val first = rest.indexOfFirst { blockOf(it) == move.blockId }
        if (first >= 0) return first
    }
    val beforeKey = move.beforeKey
    if (beforeKey != null) {
        val i = rest.indexOfFirst { keyOf(it) == beforeKey }
        if (i >= 0) return i
    } else {
        val last = rest.indexOfLast { blockOf(it) == move.blockId }
        if (last >= 0) return last + 1
    }
    return null
}
