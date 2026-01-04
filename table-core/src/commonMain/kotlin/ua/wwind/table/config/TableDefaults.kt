package ua.wwind.table.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

public object TableDefaults {
    /**
     * Sentinel value to disable table border.
     * Pass this to the border parameter to hide the outer border.
     */
    public val NoBorder: BorderStroke = BorderStroke(0.dp, Color.Transparent)

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
        footerContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        footerContentColor: Color =
            MaterialTheme.colorScheme.contentColorFor(footerContainerColor),
    ): TableColors =
        TableColors(
            headerContainerColor = headerContainerColor,
            headerContentColor = headerContentColor,
            rowContainerColor = rowContainerColor,
            rowSelectedContainerColor = rowSelectedContainerColor,
            stripedRowContainerColor = stripedRowContainerColor,
            groupContainerColor = groupContainerColor,
            footerContainerColor = footerContainerColor,
            footerContentColor = footerContentColor,
        )

    /** Standard dimensions for table with comfortable spacing and sizes. */
    public fun standardDimensions(): TableDimensions =
        TableDimensions(
            defaultColumnWidth = 200.dp,
            rowHeight = 52.dp,
            headerHeight = 56.dp,
            footerHeight = 52.dp,
            dividerThickness = 1.dp,
            pinnedColumnDividerThickness = 2.dp,
        )

    /** Compact dimensions for table with minimal spacing and sizes. */
    public fun compactDimensions(): TableDimensions =
        TableDimensions(
            defaultColumnWidth = 120.dp,
            rowHeight = 36.dp,
            headerHeight = 40.dp,
            footerHeight = 36.dp,
            dividerThickness = 1.dp,
            pinnedColumnDividerThickness = 2.dp,
        )
}
