package ua.wwind.table.state

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings

/** Divider drawn after [columnIndex]; a phantom one shifts every boundary after it off its paint. */
internal fun dividerWidthAfterColumn(
    columnIndex: Int,
    totalVisibleColumns: Int,
    settings: TableSettings,
    dimensions: TableDimensions,
): Dp {
    val pinnedCount =
        if (settings.pinnedColumnsCount >= totalVisibleColumns) 0 else settings.pinnedColumnsCount
    val bordersPinnedBlock =
        pinnedCount > 0 &&
            when (settings.pinnedColumnsSide) {
                PinnedSide.Left -> columnIndex == pinnedCount - 1
                PinnedSide.Right -> columnIndex == totalVisibleColumns - pinnedCount - 1
            }

    return when {
        // A pinned block always draws its own edge, whatever the vertical-divider setting says.
        bordersPinnedBlock -> dimensions.pinnedColumnDividerThickness
        settings.showVerticalDividers -> dimensions.dividerThickness
        else -> 0.dp
    }
}
