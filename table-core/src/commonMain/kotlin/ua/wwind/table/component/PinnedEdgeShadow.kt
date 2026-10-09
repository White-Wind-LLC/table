package ua.wwind.table.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import ua.wwind.table.physicalLeft
import ua.wwind.table.state.PinnedEdge

/** Peak alpha of the black (Material shadow) pinned-edge shadow at the edge; it fades out across its width. */
private const val PINNED_SHADOW_ALPHA = 0.16f

/**
 * Draws a shadow just outside the logical [edge] of a pinned cell, over the content scrolling under it. The
 * cell sits above its neighbours (`zIndex`) and nothing clips it, so the gradient lands on them.
 * [alpha] is read at draw time, so a fade does not recompose the cell.
 */
internal fun Modifier.pinnedEdgeShadow(
    edge: PinnedEdge,
    width: Dp,
    alpha: () -> Float,
): Modifier =
    if (edge == PinnedEdge.None || width.value <= 0f) {
        this
    } else {
        drawWithContent {
            drawContent()
            val a = alpha()
            if (a <= 0f) return@drawWithContent
            val w = width.toPx()
            val shade = Color.Black.copy(alpha = PINNED_SHADOW_ALPHA * a)
            val left = pinnedShadowLeft(edge, layoutDirection, size.width, w)
            val colors =
                if (shadowTouchesCellOnItsLeft(left)) {
                    listOf(shade, Color.Transparent)
                } else {
                    listOf(Color.Transparent, shade)
                }
            drawRect(
                brush = Brush.horizontalGradient(colors, startX = left, endX = left + w),
                topLeft = Offset(left, 0f),
                size = Size(w, size.height),
            )
        }
    }

/** Physical left of the shadow outside [edge] of a cell [cellWidth] px wide. */
internal fun pinnedShadowLeft(
    edge: PinnedEdge,
    layoutDirection: LayoutDirection,
    cellWidth: Float,
    shadowWidth: Float,
): Float {
    // Measured from the cell's start edge: after the cell for its end edge, before it for its start edge.
    val start = if (edge == PinnedEdge.End) cellWidth else -shadowWidth
    return layoutDirection.physicalLeft(start, shadowWidth, cellWidth)
}

/** A shadow starting at [left] (cell-local px) lies right of the cell, so it touches the cell on its left end. */
internal fun shadowTouchesCellOnItsLeft(left: Float): Boolean = left >= 0f
