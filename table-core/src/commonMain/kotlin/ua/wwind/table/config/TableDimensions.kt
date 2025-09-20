package ua.wwind.table.config

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common size constants and defaults used by the table layout.
 */
public data class TableDimensions(
    val defaultColumnWidth: Dp = 200.dp,
    val defaultRowHeight: Dp = 48.dp,
    val checkBoxColumnWidth: Dp = 80.dp,
    val verticalDividerThickness: Dp = 1.dp,
    val verticalDividerPaddingHorizontal: Dp = 4.dp,
)
