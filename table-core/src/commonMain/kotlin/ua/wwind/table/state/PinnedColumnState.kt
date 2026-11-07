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
 * Calculates the pinned column state based on its index and both pinned blocks.
 *
 * @param columnIndex index of the column in the visible columns list
 * @param totalVisibleColumns total number of visible columns
 * @param pinned sizes of the start and end pinned blocks
 * @param horizontalState horizontal scroll state
 * @param layoutDirection the table's layout direction; translations are physical
 */
internal fun calculatePinnedColumnState(
    columnIndex: Int,
    totalVisibleColumns: Int,
    pinned: PinnedCounts,
    horizontalState: ScrollState,
    layoutDirection: LayoutDirection,
): PinnedColumnState {
    val effective = pinned.effective(totalVisibleColumns)
    val side = pinnedSideAt(columnIndex, totalVisibleColumns, effective)
    return PinnedColumnState(
        isPinned = side != null,
        isLastBeforeEndPinned = effective.end > 0 && columnIndex == totalVisibleColumns - effective.end - 1,
        isLastStartPinned = side == PinnedSide.Start && columnIndex == effective.start - 1,
        isFirstEndPinned = side == PinnedSide.End && columnIndex == totalVisibleColumns - effective.end,
        zIndex = if (side != null) 1f else 0f,
        translationX =
            side?.let {
                pinnedTranslationX(it, horizontalState.value, horizontalState.maxValue, layoutDirection)
            } ?: 0f,
    )
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

/** Sizes of the start and end pinned blocks, counted among visible columns. */
internal data class PinnedCounts(
    val start: Int,
    val end: Int,
) {
    /** The blocks as rendered among [visibleCount] columns: counts covering every column pin none. */
    fun effective(visibleCount: Int): PinnedCounts = if (start + end >= visibleCount) NONE else this

    companion object {
        val NONE: PinnedCounts = PinnedCounts(0, 0)
    }
}

/** Side of the pinned block holding the visible column at [columnIndex], or `null` when it scrolls. */
internal fun pinnedSideAt(
    columnIndex: Int,
    totalVisibleColumns: Int,
    pinned: PinnedCounts,
): PinnedSide? {
    val effective = pinned.effective(totalVisibleColumns)
    return when {
        columnIndex < effective.start -> PinnedSide.Start
        columnIndex >= totalVisibleColumns - effective.end -> PinnedSide.End
        else -> null
    }
}
