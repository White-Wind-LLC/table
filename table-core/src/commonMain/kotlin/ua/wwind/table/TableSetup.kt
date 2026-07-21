package ua.wwind.table

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import ua.wwind.table.state.RowBlocksState
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState

// Setup helpers for the table entry points in Table.kt: what a caller declared, reconciled into
// what actually renders, plus the effects and diagnostics that ride alongside it.

/**
 * Developer-facing warnings for row-blocks misconfiguration.
 *
 * Diagnostics only: nothing here changes what renders. They live in one place so the table body
 * reads as layout rather than as a validation pass.
 */
@Composable
internal fun <T : Any> WarnRowBlocksMisuse(
    rowBlocks: RowBlocks<T>?,
    rowKey: (item: T?, index: Int) -> Any,
    rowBlocksSuppressed: Boolean,
) {
    LaunchedEffect(rowBlocksSuppressed) {
        if (rowBlocksSuppressed) Logger.w { "rowBlocks ignored while groupBy is active" }
    }
    LaunchedEffect(rowBlocks, rowKey) {
        if (rowBlocks != null && rowKey === DefaultRowKey) {
            Logger.w {
                "rowBlocks requires a stable rowKey: RowBlockMove anchors are row keys, " +
                    "and the default positional key cannot survive a move"
            }
        }
    }
    LaunchedEffect(rowBlocks) {
        if (rowBlocks != null && rowBlocks.onCommit != null && rowBlocks.blockHeader == null) {
            Logger.w {
                "RowBlocks.onCommit is set but blockHeader is null: a block's whole-block drag " +
                    "handle lives in its header, so blocks cannot be dragged as a unit without one. " +
                    "Provide a blockHeader, or set onCommit = null for within-block-only reordering"
            }
        }
    }
}

/** What a table actually renders once `rowBlocks` and `groupBy` have been reconciled. */
internal class EffectiveRowSource<T : Any>(
    val blocks: RowBlocksState<T>?,
    val itemAt: (Int) -> T?,
    val rowKey: (item: T?, index: Int) -> Any,
    val onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    val suppressedByGroupBy: Boolean,
)

/**
 * Reconciles the two structures a caller can declare over one list, and reports what to render.
 *
 * `groupBy` and row blocks describe incompatible structures. groupBy wins: blocks are suppressed
 * rather than both rendered, and the conflict is surfaced through [TableState] instead of thrown
 * from a menu click. The rendered list is then the blocks state's (possibly permuted) view while a
 * gesture or an optimistic post-drop hold is in flight, and the caller's list otherwise.
 */
@Composable
internal fun <T : Any, C> rememberEffectiveRowSource(
    state: TableState<C>,
    rowBlocks: RowBlocks<T>?,
    rowKey: (item: T?, index: Int) -> Any,
    onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)?,
    itemsCount: Int,
    itemAt: (Int) -> T?,
): EffectiveRowSource<T> {
    val blocksState =
        rowBlocks?.let { blocks -> remember(blocks, rowKey) { RowBlocksState(blocks, rowKey) } }
    // Feed the state the same snapshot this composition renders: block extents are derived here,
    // never declared, so they cannot lag behind an asynchronously filtered list.
    blocksState?.reconcile(itemsCount, itemAt)

    val suppressed = blocksState != null && state.groupBy != null
    val activeBlocks = if (suppressed) null else blocksState
    state.rowBlocksSuppressedByGroupBy = suppressed
    state.rowBlocksNonEmpty = blocksState != null && blocksState.hasBlocks

    return EffectiveRowSource(
        blocks = activeBlocks,
        itemAt =
            remember(activeBlocks, itemAt) {
                if (activeBlocks != null) activeBlocks::itemAt else itemAt
            },
        rowKey =
            remember(activeBlocks, rowKey) {
                if (activeBlocks == null) {
                    rowKey
                } else {
                    { item, viewIndex -> rowKey(item, activeBlocks.upstreamIndexOf(viewIndex)) }
                }
            },
        // Gate on blocksState, not activeBlocks: declaring rowBlocks retires onRowMove outright, and
        // the groupBy suppression must not resurrect it — per-row drag appearing under a grouped view
        // would silently swap move semantics. Suppressed blocks mean no row drag at all.
        onRowMove = if (blocksState == null) onRowMove else null,
        suppressedByGroupBy = suppressed,
    )
}

/**
 * Hands [state] the edit-mode callbacks, resolving row indices through the rendered order.
 *
 * The registered wrapper outlives this composition's resolver — [itemAt] is rebuilt whenever the
 * active blocks or the caller's loader change — so it is read through `rememberUpdatedState`
 * instead of re-registering (the same trampoline as the embedded settle path).
 */
@Composable
internal fun <T : Any, C> RegisterEditCallbacks(
    state: TableState<C>,
    itemAt: (Int) -> T?,
    onRowEditStart: ((item: T, rowIndex: Int) -> Unit)?,
    onRowEditComplete: ((rowIndex: Int) -> Boolean)?,
    onCancelEdit: ((rowIndex: Int) -> Unit)?,
) {
    val currentItemAt = rememberUpdatedState(itemAt)
    LaunchedEffect(state, onRowEditStart, onRowEditComplete, onCancelEdit) {
        state.setEditCallbacks(
            onStart =
                onRowEditStart?.let { callback ->
                    { rowIndex: Int ->
                        // Edit indices are view positions, so resolve through the rendered order.
                        val item = currentItemAt.value(rowIndex)
                        if (item != null) callback(item, rowIndex)
                    }
                },
            onComplete = onRowEditComplete,
            onCancel = onCancelEdit,
        )
    }
}

/**
 * Scrolls back to the first row whenever the sort actually changes.
 *
 * A re-ordered list read from wherever the user had scrolled to is disorienting, and the remembered
 * sort is saveable so a configuration change does not read as a change of sort and scroll them away
 * from where they were.
 */
@Composable
internal fun <C> ScrollToTopOnSortChange(
    state: TableState<C>,
    verticalState: LazyListState,
) {
    var rememberedSort by rememberSaveable { mutableStateOf<SortState<C>?>(null) }
    LaunchedEffect(state.sort) {
        if (state.sort == rememberedSort) return@LaunchedEffect
        rememberedSort = state.sort
        if (verticalState.canScrollBackward) {
            Logger.d { "state.sort performs scroll to top" }
            verticalState.scrollToItem(0)
        }
    }
}
