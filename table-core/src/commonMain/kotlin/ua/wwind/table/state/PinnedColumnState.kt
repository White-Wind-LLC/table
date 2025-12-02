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
    // If all columns are pinned, then effectively none are pinned
    val effectivePinnedCount = if (pinnedColumnsCount >= totalVisibleColumns) 0 else pinnedColumnsCount

    val isPinned =
        effectivePinnedCount > 0 &&
            when (pinnedColumnsSide) {
                PinnedSide.Left -> columnIndex < effectivePinnedCount
                PinnedSide.Right -> columnIndex >= totalVisibleColumns - effectivePinnedCount
            }

    val isLastBeforeRightPinned =
        pinnedColumnsSide == PinnedSide.Right &&
            !isPinned &&
            columnIndex == totalVisibleColumns - effectivePinnedCount - 1

    val isLastLeftPinned =
        pinnedColumnsSide == PinnedSide.Left &&
            isPinned &&
            columnIndex == effectivePinnedCount - 1

    val isFirstRightPinned =
        pinnedColumnsSide == PinnedSide.Right &&
            isPinned &&
            columnIndex == totalVisibleColumns - effectivePinnedCount

    val zIndex = if (isPinned) 1f else 0f

    val translationX =
        if (isPinned) {
            when (pinnedColumnsSide) {
                PinnedSide.Left -> {
                    horizontalState.value.toFloat()
                }

                PinnedSide.Right -> {
                    // For the right side use a simplified formula
                    // translationX = scroll - maxValue
                    horizontalState.value.toFloat() - horizontalState.maxValue
                }
            }
        } else {
            0f
        }

    return PinnedColumnState(
        isPinned = isPinned,
        isLastBeforeRightPinned = isLastBeforeRightPinned,
        isLastLeftPinned = isLastLeftPinned,
        isFirstRightPinned = isFirstRightPinned,
        zIndex = zIndex,
        translationX = translationX,
    )
}
