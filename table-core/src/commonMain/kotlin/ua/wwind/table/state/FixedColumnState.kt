package ua.wwind.table.state

import androidx.compose.foundation.ScrollState
import ua.wwind.table.config.FixedSide

/**
 * Information about the fixed column state
 */
internal data class FixedColumnState(
    /** Whether the column is fixed */
    val isFixed: Boolean,
    /** Whether the column is the last unfixed one before right-fixed columns */
    val isLastBeforeRightFixed: Boolean,
    /** Whether the column is the last fixed one on the left */
    val isLastLeftFixed: Boolean,
    /** Whether the column is the first fixed one on the right */
    val isFirstRightFixed: Boolean,
    /** Z-index for rendering */
    val zIndex: Float,
    /** Horizontal translation for fixing */
    val translationX: Float,
)

/**
 * Calculates the fixed column state based on its index and table settings
 *
 * @param columnIndex index of the column in the visible columns list
 * @param totalVisibleColumns total number of visible columns
 * @param fixedColumnsCount number of fixed columns
 * @param fixedColumnsSide side of fixation (left or right)
 * @param horizontalState horizontal scroll state
 */
internal fun calculateFixedColumnState(
    columnIndex: Int,
    totalVisibleColumns: Int,
    fixedColumnsCount: Int,
    fixedColumnsSide: FixedSide,
    horizontalState: ScrollState,
): FixedColumnState {
    // If all columns are fixed, then effectively none are fixed
    val effectiveFixedCount = if (fixedColumnsCount >= totalVisibleColumns) 0 else fixedColumnsCount

    val isFixed =
        effectiveFixedCount > 0 &&
            when (fixedColumnsSide) {
                FixedSide.Left -> columnIndex < effectiveFixedCount
                FixedSide.Right -> columnIndex >= totalVisibleColumns - effectiveFixedCount
            }

    val isLastBeforeRightFixed =
        fixedColumnsSide == FixedSide.Right &&
            !isFixed &&
            columnIndex == totalVisibleColumns - effectiveFixedCount - 1

    val isLastLeftFixed =
        fixedColumnsSide == FixedSide.Left &&
            isFixed &&
            columnIndex == effectiveFixedCount - 1

    val isFirstRightFixed =
        fixedColumnsSide == FixedSide.Right &&
            isFixed &&
            columnIndex == totalVisibleColumns - effectiveFixedCount

    val zIndex = if (isFixed) 1f else 0f

    val translationX =
        if (isFixed) {
            when (fixedColumnsSide) {
                FixedSide.Left -> horizontalState.value.toFloat()
                FixedSide.Right -> {
                    // For the right side use a simplified formula
                    // translationX = scroll - maxValue
                    horizontalState.value.toFloat() - horizontalState.maxValue
                }
            }
        } else {
            0f
        }

    return FixedColumnState(
        isFixed = isFixed,
        isLastBeforeRightFixed = isLastBeforeRightFixed,
        isLastLeftFixed = isLastLeftFixed,
        isFirstRightFixed = isFirstRightFixed,
        zIndex = zIndex,
        translationX = translationX,
    )
}
