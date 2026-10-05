package ua.wwind.table.component.header

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max

/** Horizontal extent of a column's resize grab strip, in table coordinates. */
internal data class ResizeHandleSpan(
    val left: Dp,
    val width: Dp,
)

/**
 * The grab strip for the boundary at [boundaryX]: [handleWidth] wide and centred on the divider that follows it.
 * The last strip has no room to hang over the table's end, so it ends at the divider and grows to the left.
 */
internal fun resizeHandleSpan(
    boundaryX: Dp,
    handleWidth: Dp,
    dividerThickness: Dp,
    isLast: Boolean,
): ResizeHandleSpan {
    val width = max(handleWidth, dividerThickness)
    val left = if (isLast) boundaryX + dividerThickness - width else boundaryX + (dividerThickness - width) / 2
    return ResizeHandleSpan(left = left, width = width)
}
