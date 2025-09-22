package ua.wwind.table.config

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common size constants and defaults used by the table layout.
 */
public data class TableDimensions(
    val defaultColumnWidth: Dp = 200.dp,
    val rowHeight: Dp = 52.dp,
    val headerHeight: Dp = 56.dp,
    val dividerThickness: Dp = 1.dp,
)
