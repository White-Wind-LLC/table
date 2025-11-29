package ua.wwind.table.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

public object TableDefaults {
    /** Convenience factory for default [TableColors] derived from [androidx.compose.material3.MaterialTheme]. */
    @Composable
    public fun colors(
        headerContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        headerContentColor: Color =
            MaterialTheme.colorScheme.contentColorFor(headerContainerColor),
        rowContainerColor: Color = MaterialTheme.colorScheme.surface,
        rowSelectedContainerColor: Color = MaterialTheme.colorScheme.tertiary,
        stripedRowContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
        groupContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ): TableColors =
        TableColors(
            headerContainerColor = headerContainerColor,
            headerContentColor = headerContentColor,
            rowContainerColor = rowContainerColor,
            rowSelectedContainerColor = rowSelectedContainerColor,
            stripedRowContainerColor = stripedRowContainerColor,
            groupContainerColor = groupContainerColor,
        )

    /** Standard dimensions for table with comfortable spacing and sizes. */
    public fun standardDimensions(): TableDimensions =
        TableDimensions(
            defaultColumnWidth = 200.dp,
            rowHeight = 52.dp,
            headerHeight = 56.dp,
            dividerThickness = 1.dp,
            fixedColumnDividerThickness = 2.dp,
        )

    /** Compact dimensions for table with minimal spacing and sizes. */
    public fun compactDimensions(): TableDimensions =
        TableDimensions(
            defaultColumnWidth = 120.dp,
            rowHeight = 36.dp,
            headerHeight = 40.dp,
            dividerThickness = 1.dp,
            fixedColumnDividerThickness = 2.dp,
        )
}