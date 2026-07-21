package ua.wwind.table.state

import androidx.compose.foundation.ScrollState
import ua.wwind.table.config.PinnedSide

/**
 * Information about the pinned column state
 */
internal data class PinnedColumnState(
    /** Whether the column is pinned */
    val isPinned: Boolean,
    /** Whether the column is the last unpinned one before right-pinned columns */
    val isLastBeforeRightPinned: Boolean,
    /** Whether the column is the last pinned one on the left */
    val isLastLeftPinned: Boolean,
    /** Whether the column is the first pinned one on the right */
    val isFirstRightPinned: Boolean,
    /** Z-index for rendering */
    val zIndex: Float,
    /** Horizontal translation for pinning */
    val translationX: Float,
)

/**
 * Calculates the pinned column state based on its index and table settings
 *
 * @param columnIndex index of the column in the visible columns list
 * @param totalVisibleColumns total number of visible columns
 * @param pinnedColumnsCount number of pinned columns
 * @param pinnedColumnsSide side of pinning (left or right)
 * @param horizontalState horizontal scroll state
 */
internal fun calculatePinnedColumnState(
    columnIndex: Int,
    totalVisibleColumns: Int,
    pinnedColumnsCount: Int,
    pinnedColumnsSide: PinnedSide,
    horizontalState: ScrollState,
): PinnedColumnState {
    val effectivePinnedCount = effectivePinnedCount(pinnedColumnsCount, totalVisibleColumns)
    val isPinned =
        isColumnPinned(columnIndex, totalVisibleColumns, effectivePinnedCount, pinnedColumnsSide)

    return PinnedColumnState(
        isPinned = isPinned,
        isLastBeforeRightPinned =
            pinnedColumnsSide == PinnedSide.Right &&
                !isPinned &&
                columnIndex == totalVisibleColumns - effectivePinnedCount - 1,
        isLastLeftPinned =
            pinnedColumnsSide == PinnedSide.Left &&
                isPinned &&
                columnIndex == effectivePinnedCount - 1,
        isFirstRightPinned =
            pinnedColumnsSide == PinnedSide.Right &&
                isPinned &&
                columnIndex == totalVisibleColumns - effectivePinnedCount,
        zIndex = if (isPinned) 1f else 0f,
        translationX = if (isPinned) pinnedTranslationX(pinnedColumnsSide, horizontalState) else 0f,
    )
}

/** Pinning every column pins none: there would be nothing left to scroll underneath them. */
private fun effectivePinnedCount(
    pinnedColumnsCount: Int,
    totalVisibleColumns: Int,
): Int = if (pinnedColumnsCount >= totalVisibleColumns) 0 else pinnedColumnsCount

/** Pinned columns are the leading [effectivePinnedCount] on the left, the trailing ones on the right. */
private fun isColumnPinned(
    columnIndex: Int,
    totalVisibleColumns: Int,
    effectivePinnedCount: Int,
    side: PinnedSide,
): Boolean =
    effectivePinnedCount > 0 &&
        when (side) {
            PinnedSide.Left -> columnIndex < effectivePinnedCount
            PinnedSide.Right -> columnIndex >= totalVisibleColumns - effectivePinnedCount
        }

/** Offset that holds a pinned column still while the rest of the row scrolls under it. */
private fun pinnedTranslationX(
    side: PinnedSide,
    horizontalState: ScrollState,
): Float =
    when (side) {
        PinnedSide.Left -> horizontalState.value.toFloat()

        // For the right side use a simplified formula: translationX = scroll - maxValue
        PinnedSide.Right -> horizontalState.value.toFloat() - horizontalState.maxValue
    }
