package ua.wwind.table.state

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.unit.LayoutDirection
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.sign

/**
 * Information about the pinned column state
 */
internal data class PinnedColumnState(
    /** Whether the column is pinned */
    val isPinned: Boolean,
    /** Whether the column is the last unpinned one before end-pinned columns */
    val isLastBeforeEndPinned: Boolean,
    /** Whether the column is the last pinned one at the start */
    val isLastStartPinned: Boolean,
    /** Whether the column is the first pinned one at the end */
    val isFirstEndPinned: Boolean,
    /** Z-index for rendering */
    val zIndex: Float,
    /** Horizontal translation for pinning */
    val translationX: Float,
) {
    /** The side of this column that borders the scrolling columns, if it is the outermost pinned one. */
    val edge: PinnedEdge
        get() =
            when {
                isLastStartPinned -> PinnedEdge.End
                isFirstEndPinned -> PinnedEdge.Start
                else -> PinnedEdge.None
            }
}

/** Logical side of a pinned column that the scrolling columns pass under, matching [PinnedSide]. */
internal enum class PinnedEdge { None, Start, End }

/**
 * Whether scrolled content currently sits under [this] edge: before the viewport for a start-pinned
 * run, after it for an end-pinned one. The edge casts a shadow only then.
 */
internal fun PinnedEdge.hasContentUnder(horizontalState: ScrollState): Boolean =
    when (this) {
        PinnedEdge.None -> false
        PinnedEdge.End -> horizontalState.value > 0
        PinnedEdge.Start -> horizontalState.value < horizontalState.maxValue
    }

/**
 * Calculates the pinned column state based on its index and table settings
 *
 * @param columnIndex index of the column in the visible columns list
 * @param totalVisibleColumns total number of visible columns
 * @param pinnedColumnsCount number of pinned columns
 * @param pinnedColumnsSide side of pinning (start or end)
 * @param horizontalState horizontal scroll state
 * @param layoutDirection the table's layout direction; translations are physical
 */
internal fun calculatePinnedColumnState(
    columnIndex: Int,
    totalVisibleColumns: Int,
    pinnedColumnsCount: Int,
    pinnedColumnsSide: PinnedSide,
    horizontalState: ScrollState,
    layoutDirection: LayoutDirection,
): PinnedColumnState {
    val effectivePinnedCount = effectivePinnedCount(pinnedColumnsCount, totalVisibleColumns)
    val isPinned =
        isColumnPinned(columnIndex, totalVisibleColumns, effectivePinnedCount, pinnedColumnsSide)

    return PinnedColumnState(
        isPinned = isPinned,
        isLastBeforeEndPinned =
            pinnedColumnsSide == PinnedSide.End &&
                !isPinned &&
                columnIndex == totalVisibleColumns - effectivePinnedCount - 1,
        isLastStartPinned =
            pinnedColumnsSide == PinnedSide.Start &&
                isPinned &&
                columnIndex == effectivePinnedCount - 1,
        isFirstEndPinned =
            pinnedColumnsSide == PinnedSide.End &&
                isPinned &&
                columnIndex == totalVisibleColumns - effectivePinnedCount,
        zIndex = if (isPinned) 1f else 0f,
        translationX =
            if (isPinned) {
                pinnedTranslationX(pinnedColumnsSide, horizontalState.value, horizontalState.maxValue, layoutDirection)
            } else {
                0f
            },
    )
}

/** Pinning every column pins none: there would be nothing left to scroll underneath them. */
private fun effectivePinnedCount(
    pinnedColumnsCount: Int,
    totalVisibleColumns: Int,
): Int = if (pinnedColumnsCount >= totalVisibleColumns) 0 else pinnedColumnsCount

/** Pinned columns are the leading [effectivePinnedCount] at the start, the trailing ones at the end. */
private fun isColumnPinned(
    columnIndex: Int,
    totalVisibleColumns: Int,
    effectivePinnedCount: Int,
    side: PinnedSide,
): Boolean =
    effectivePinnedCount > 0 &&
        when (side) {
            PinnedSide.Start -> columnIndex < effectivePinnedCount
            PinnedSide.End -> columnIndex >= totalVisibleColumns - effectivePinnedCount
        }

/** Offset that holds a pinned column still while the rest of the row scrolls under it. */
internal fun pinnedTranslationX(
    side: PinnedSide,
    scrollValue: Int,
    maxScroll: Int,
    layoutDirection: LayoutDirection,
): Float =
    layoutDirection.sign(
        when (side) {
            PinnedSide.Start -> scrollValue.toFloat()

            // scroll - maxScroll keeps an end-pinned run at the viewport's end edge.
            PinnedSide.End -> (scrollValue - maxScroll).toFloat()
        },
    )
