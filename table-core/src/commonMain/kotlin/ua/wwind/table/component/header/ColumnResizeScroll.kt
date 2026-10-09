package ua.wwind.table.component.header

/** How far a resize boundary overshot the end edge of the viewport; 0 while it is still visible. */
internal fun resizeScrollPullback(
    boundaryPx: Float,
    scrollValue: Int,
    viewportPx: Int,
): Float = (boundaryPx - scrollValue - viewportPx).coerceAtLeast(0f)

/** Whether the pointer is pushing toward the end within [zonePx] of the viewport's end edge, out of travel. */
internal fun shouldGrowAtResizeEdge(
    dragAmount: Float,
    pointerViewportPx: Float,
    viewportPx: Int,
    zonePx: Float,
): Boolean = dragAmount >= 0f && viewportPx > 0 && pointerViewportPx >= viewportPx - zonePx
