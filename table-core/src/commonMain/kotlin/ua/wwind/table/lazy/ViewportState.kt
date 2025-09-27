package ua.wwind.table.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * Viewport state for 2D virtualized grid.
 *
 * Holds current scroll offsets and content/viewport sizes and provides clamping helpers.
 */
@Stable
public class ViewportState(
    overscanPx: Int = 300,
) {
    /** Horizontal scroll offset in pixels. */
    public var offsetX: Float by mutableStateOf(0f)

    /** Vertical scroll offset in pixels. */
    public var offsetY: Float by mutableStateOf(0f)

    /** Current viewport size in pixels. Updated by layout on size changes. */
    public var viewportSize: IntSize by mutableStateOf(IntSize.Zero)
        internal set

    /** Total content width in pixels. Updated by the grid. */
    public var contentWidth: Int by mutableStateOf(0)
        internal set

    /** Total content height in pixels. Updated by the grid. */
    public var contentHeight: Int by mutableStateOf(0)
        internal set

    /** Overscan in pixels (renders extra cells beyond viewport to prevent flashes). */
    public var overscanPx: Int by mutableStateOf(overscanPx)

    /** Max permissible X offset considering content and viewport sizes. */
    public val maxOffsetX: Int
        get() = max(0, contentWidth - viewportSize.width)

    /** Max permissible Y offset considering content and viewport sizes. */
    public val maxOffsetY: Int
        get() = max(0, contentHeight - viewportSize.height)

    /**
     * Scroll by specified delta in pixels, applying clamping to keep offsets within bounds.
     * Returns actually applied delta (may differ if clamped at the edge).
     */
    public fun scrollBy(deltaX: Float, deltaY: Float): Pair<Float, Float> {
        val beforeX = offsetX
        val beforeY = offsetY
        offsetX = (offsetX + deltaX).coerceIn(0f, maxOffsetX.toFloat())
        offsetY = (offsetY + deltaY).coerceIn(0f, maxOffsetY.toFloat())
        return Pair(offsetX - beforeX, offsetY - beforeY)
    }

    /** Clamp current offsets to valid bounds. Call when sizes change. */
    public fun clamp() {
        offsetX = offsetX.coerceIn(0f, maxOffsetX.toFloat())
        offsetY = offsetY.coerceIn(0f, maxOffsetY.toFloat())
    }

    internal fun updateSizes(viewport: IntSize, contentW: Int, contentH: Int) {
        viewportSize = viewport
        contentWidth = contentW
        contentHeight = contentH
        clamp()
    }
}

@Composable
public fun rememberViewportState(overscanPx: Int = 300): ViewportState = remember { ViewportState(overscanPx) }

@Immutable
public data class VisibleRange(
    val firstRow: Int,
    val lastRow: Int,
    val firstCol: Int,
    val lastCol: Int,
)

/** Compute visible grid range for fixed-size rows/columns. */
internal fun computeVisibleRangeFixed(
    offsetX: Float,
    offsetY: Float,
    viewport: IntSize,
    overscanPx: Int,
    totalRows: Int,
    totalCols: Int,
    rowHeightPx: Int,
    colWidthPx: Int,
): VisibleRange {
    val left = (offsetX - overscanPx).coerceAtMost(offsetX)
    val top = (offsetY - overscanPx).coerceAtMost(offsetY)
    val right = offsetX + viewport.width + overscanPx
    val bottom = offsetY + viewport.height + overscanPx

    val firstCol = (left / colWidthPx).toInt().coerceAtLeast(0)
    val lastCol = (right / colWidthPx).toInt().coerceAtLeast(0).coerceAtMost(totalCols - 1)
    val firstRow = (top / rowHeightPx).toInt().coerceAtLeast(0)
    val lastRow = (bottom / rowHeightPx).toInt().coerceAtLeast(0).coerceAtMost(totalRows - 1)

    return VisibleRange(
        firstRow = firstRow.coerceAtMost(totalRows - 1).coerceAtLeast(0),
        lastRow = lastRow.coerceAtLeast(firstRow).coerceAtMost(totalRows - 1),
        firstCol = firstCol.coerceAtMost(totalCols - 1).coerceAtLeast(0),
        lastCol = lastCol.coerceAtLeast(firstCol).coerceAtMost(totalCols - 1),
    )
}

/** Compute visible range when columns have variable pixel widths defined by cumulative sums [colsCum]. */
internal fun computeVisibleRangeVariable(
    offsetX: Float,
    offsetY: Float,
    viewport: IntSize,
    overscanPx: Int,
    totalRows: Int,
    rowHeightPx: Int,
    colsCum: IntArray,
): VisibleRange {
    val nCols = colsCum.size - 1
    val left = (offsetX - overscanPx).coerceAtMost(offsetX)
    val top = (offsetY - overscanPx).coerceAtMost(offsetY)
    val right = offsetX + viewport.width + overscanPx
    val bottom = offsetY + viewport.height + overscanPx

    val firstCol = (upperBound(colsCum, left.toInt()) - 1).coerceAtLeast(0)
    val lastCol = (lowerBound(colsCum, right.toInt()) - 1).coerceAtLeast(firstCol).coerceAtMost(nCols - 1)

    val firstRow = (top / rowHeightPx).toInt().coerceAtLeast(0)
    val lastRow = (bottom / rowHeightPx).toInt().coerceAtLeast(0).coerceAtMost(totalRows - 1)

    return VisibleRange(
        firstRow = firstRow.coerceAtMost(totalRows - 1).coerceAtLeast(0),
        lastRow = lastRow.coerceAtLeast(firstRow).coerceAtMost(totalRows - 1),
        firstCol = firstCol.coerceAtMost(nCols - 1).coerceAtLeast(0),
        lastCol = lastCol.coerceAtLeast(firstCol).coerceAtMost(nCols - 1),
    )
}

// First index i such that arr[i] > value; arr must be non-decreasing
private fun upperBound(arr: IntArray, value: Int): Int {
    var l = 0
    var r = arr.size
    while (l < r) {
        val m = (l + r) ushr 1
        if (arr[m] <= value) l = m + 1 else r = m
    }
    return l
}

// First index i such that arr[i] >= value
private fun lowerBound(arr: IntArray, value: Int): Int {
    var l = 0
    var r = arr.size
    while (l < r) {
        val m = (l + r) ushr 1
        if (arr[m] < value) l = m + 1 else r = m
    }
    return l
}
