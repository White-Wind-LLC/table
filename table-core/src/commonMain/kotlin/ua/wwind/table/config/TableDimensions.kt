package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/** Common size constants and defaults used by the table layout. */
@Immutable
public data class TableDimensions(
    val defaultColumnWidth: Dp,
    val rowHeight: Dp,
    val headerHeight: Dp,
    val dividerThickness: Dp,
    val fixedColumnDividerThickness: Dp,
)
