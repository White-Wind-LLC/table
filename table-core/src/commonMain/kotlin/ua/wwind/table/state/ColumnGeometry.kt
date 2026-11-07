package ua.wwind.table.state

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings

/** Divider drawn after [columnIndex]; a phantom one shifts every boundary after it off its paint. */
internal fun dividerWidthAfterColumn(
    columnIndex: Int,
    totalVisibleColumns: Int,
    pinned: PinnedCounts,
    settings: TableSettings,
    dimensions: TableDimensions,
): Dp {
    val effective = pinned.effective(totalVisibleColumns)
    val bordersPinnedBlock =
        (effective.start > 0 && columnIndex == effective.start - 1) ||
            (effective.end > 0 && columnIndex == totalVisibleColumns - effective.end - 1)

    return when {
        // A pinned block always draws its own edge, whatever the vertical-divider setting says.
        bordersPinnedBlock -> dimensions.pinnedColumnDividerThickness

        settings.showVerticalDividers -> dimensions.dividerThickness

        else -> 0.dp
    }
}
