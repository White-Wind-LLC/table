package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
) {
    init {
        require(dividerThickness >= 1.dp) { "dividerThickness must be at least 1.dp" }
        require(pinnedColumnDividerThickness >= 1.dp) { "pinnedColumnDividerThickness must be at least 1.dp" }
        require(rowBlockSpacing >= 0.dp) { "rowBlockSpacing must not be negative" }
    }
}
