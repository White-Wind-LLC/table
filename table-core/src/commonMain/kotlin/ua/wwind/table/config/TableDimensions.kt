package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile

/** Common size constants and defaults used by the table layout. */
@Immutable
public data class TableDimensions(
    val defaultColumnWidth: Dp,
    val rowHeight: Dp,
    val headerHeight: Dp,
    val footerHeight: Dp,
    /** Thickness of dividers. Should be at least 1.dp. */
    val dividerThickness: Dp,
    /** Thickness of pinned column dividers. Should be at least 1.dp. */
    val pinnedColumnDividerThickness: Dp,
    /** Vertical gap rendered above and below a row block declared via `rowBlocks`. */
    val rowBlockSpacing: Dp = 8.dp,
    /**
     * Width of the pointer target that resizes a column, centred on the column boundary; the visible
     * line stays as thin as the divider. Defaults to 24.dp on touch platforms and 8.dp elsewhere.
     */
    val columnResizeHandleWidth: Dp = defaultColumnResizeHandleWidth(),
    /** Minimum pointer target of the header sort and filter buttons. Defaults to 48.dp on touch, 24.dp elsewhere. */
    val headerIconTargetSize: Dp = defaultHeaderIconTargetSize(),
    /** Pointer target of the column drag handle shown while the header is hovered. */
    val columnDragHandleSize: Dp = 24.dp,
    /** Width of the bar marking the selected row at its leading edge; 0.dp hides it. */
    val selectionIndicatorWidth: Dp = 3.dp,
    /** Horizontal padding around the header title and sort icon. */
    val cellHorizontalPadding: Dp = 8.dp,
    /** Gap after the header filter icon. */
    val headerIconSpacing: Dp = 6.dp,
    /** Glyph size of the column drag handle; [columnDragHandleSize] sets its pointer target. */
    val dragHandleIconSize: Dp = 16.dp,
    /** Stroke width of the keyboard focus ring and the selected cell border. */
    val focusIndicatorWidth: Dp = 2.dp,
    /** Width of the shadow cast over scrolled content at the pinned column edge; 0.dp hides it. */
    val pinnedColumnShadowWidth: Dp = 6.dp,
    /** Shadow elevation of a row or column while it is dragged. */
    val dragElevation: Dp = 8.dp,
) {
    init {
        require(dividerThickness >= 1.dp) { "dividerThickness must be at least 1.dp" }
        require(pinnedColumnDividerThickness >= 1.dp) { "pinnedColumnDividerThickness must be at least 1.dp" }
        require(rowBlockSpacing >= 0.dp) { "rowBlockSpacing must not be negative" }
        require(columnResizeHandleWidth >= 0.dp) { "columnResizeHandleWidth must not be negative" }
        require(headerIconTargetSize >= 0.dp) { "headerIconTargetSize must not be negative" }
        require(columnDragHandleSize >= 0.dp) { "columnDragHandleSize must not be negative" }
        require(selectionIndicatorWidth >= 0.dp) { "selectionIndicatorWidth must not be negative" }
        require(cellHorizontalPadding >= 0.dp) { "cellHorizontalPadding must not be negative" }
        require(headerIconSpacing >= 0.dp) { "headerIconSpacing must not be negative" }
        require(dragHandleIconSize >= 0.dp) { "dragHandleIconSize must not be negative" }
        require(focusIndicatorWidth >= 0.dp) { "focusIndicatorWidth must not be negative" }
        require(pinnedColumnShadowWidth >= 0.dp) { "pinnedColumnShadowWidth must not be negative" }
        require(dragElevation >= 0.dp) { "dragElevation must not be negative" }
    }
}

private fun defaultColumnResizeHandleWidth(): Dp = if (getPlatform().isMobile()) 24.dp else 8.dp

private fun defaultHeaderIconTargetSize(): Dp = if (getPlatform().isMobile()) 48.dp else 24.dp
