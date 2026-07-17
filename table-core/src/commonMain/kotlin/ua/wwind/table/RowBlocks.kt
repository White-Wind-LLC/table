package ua.wwind.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Row blocks: runs of adjacent rows that render and drag as one unit, declared by identity rather
 * than by position.
 *
 * The library derives block extents itself, from the same snapshot it renders — the consumer never
 * supplies index ranges, so the ranges can never go stale against an asynchronously filtered list.
 * During a drag the library permutes its own internal view; the consumer's data is untouched until
 * [onCommit] delivers exactly one [RowBlockMove] for the completed gesture.
 *
 * Passing a `RowBlocks` supersedes the per-row `onRowMove` callback: drag units are blocks, so
 * per-row move semantics cannot apply. Every gesture — standalone rows included — reports through
 * [onCommit] (a standalone move carries a null [RowBlockMove.blockId]), `onRowMove` is never
 * invoked, and a null [onCommit] disables row drag entirely rather than falling back to it.
 *
 * Hold the instance in `remember`: it compares by identity — carrying a `@Composable` slot, it has
 * no meaningful structural equality — so a fresh instance per recomposition makes the `rowBlocks`
 * argument look changed and stops the table from skipping.
 *
 * Block tables require a stable `rowKey`: [RowBlockMove] anchors are keys, and the default
 * positional key cannot survive a move.
 */
@Stable
public class RowBlocks<T : Any>(
    /** Block identity; null = standalone row. Adjacent equal ids form one block. */
    public val blockOf: (T) -> Any?,
    /** Exactly one event per completed drag gesture; null = display-only blocks (no drag). */
    public val onCommit: ((RowBlockMove) -> Unit)? = null,
    /** Band content above a block. */
    public val blockHeader: (@Composable (blockId: Any, rows: IntRange) -> Unit)? = null,
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
 * Applies [move] to the SOURCE list — the lift from the rendered, possibly filtered view back to
 * the consumer's single source of truth.
 *
 * Relocates ALL rows whose [blockOf] equals [RowBlockMove.blockId] — including rows hidden by the
 * current filter, which [RowBlockMove.movedKeys] cannot name — preserving their relative order.
 * The insertion point is expanded to the nearest whole-block boundary in the source, so no other
 * block is ever split: where hidden rows land relative to hidden neighbours of the anchor is
 * unobservable in the view, and block-boundary insertion is the one placement that keeps every
 * block contiguous. Standalone moves ([RowBlockMove.blockId] == null) relocate just the moved keys.
 *
 * [RowBlockMove.afterKey] is the primary anchor; when its row is gone from the list by the time
 * the move is applied, [RowBlockMove.beforeKey] pins the destination instead. When neither anchor
 * resolves the destination is unknowable, and the list is deliberately left untouched — guessing a
 * position would reorder data the user never dragged.
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
 * Destination in [rest] (the source minus the moved rows), or null when neither anchor resolves.
 *
 * The expansion direction follows each anchor's role in the view. [RowBlockMove.afterKey] is the
 * last visible row of the unit the drag landed behind, so hidden members of its block can only sit
 * further down in the source — the insertion walks forward past them. [RowBlockMove.beforeKey] is
 * the first visible row of the following unit, so its hidden block-mates can only sit above — the
 * insertion walks backward. Either way the filtered projection shows the moved unit exactly where
 * the drag put it.
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
