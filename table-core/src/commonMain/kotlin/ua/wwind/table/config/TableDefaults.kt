package ua.wwind.table.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ua.wwind.table.DefaultTableEmptyContent
import ua.wwind.table.TableEmptyScope

public object TableDefaults {
    /**
     * Sentinel value to disable table border.
     * Pass this to the border parameter to hide the outer border.
     */
    public val NoBorder: BorderStroke = BorderStroke(0.dp, Color.Transparent)

    /**
     * Default body content of a table with no rows: "No data", or — while a filter is active —
     * "No results match the current filters" with a button that clears them.
     */
    public val EmptyContent: @Composable TableEmptyScope.() -> Unit = { DefaultTableEmptyContent() }

    /**
     * Convenience factory for default [TableColors] derived from [androidx.compose.material3.MaterialTheme].
     *
     * New parameters are appended, never inserted: every parameter here is a [Color], so inserting one
     * would leave positional calls compiling while silently binding to the wrong colors.
     */
    @Composable
    public fun colors(
        headerContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        headerContentColor: Color =
            MaterialTheme.colorScheme.contentColorFor(headerContainerColor),
        rowContainerColor: Color = MaterialTheme.colorScheme.surface,
        rowSelectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        stripedRowContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        groupContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        footerContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        footerContentColor: Color =
            MaterialTheme.colorScheme.contentColorFor(footerContainerColor),
        /** Kept [Color.Unspecified] here too: the band color resolves at draw for every construction path. */
        rowBlockContainerColor: Color = Color.Unspecified,
        /** Leading bar marking the selected row, so selection does not rely on the container color alone. */
        rowSelectedIndicatorColor: Color = MaterialTheme.colorScheme.primary,
        dividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
        pinnedDividerColor: Color = MaterialTheme.colorScheme.outlineVariant,
        borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
        focusIndicatorColor: Color = MaterialTheme.colorScheme.primary,
        /** State layer color of a hovered or pressed row; the ripple applies its own alpha. */
        hoverColor: Color = MaterialTheme.colorScheme.onSurface,
        groupContentColor: Color = MaterialTheme.colorScheme.contentColorFor(groupContainerColor),
        stickyGroupContainerColor: Color = groupContainerColor,
    ): TableColors =
        TableColors(
            headerContainerColor = headerContainerColor,
            headerContentColor = headerContentColor,
            rowContainerColor = rowContainerColor,
            rowSelectedContainerColor = rowSelectedContainerColor,
            stripedRowContainerColor = stripedRowContainerColor,
            groupContainerColor = groupContainerColor,
            rowBlockContainerColor = rowBlockContainerColor,
            footerContainerColor = footerContainerColor,
            footerContentColor = footerContentColor,
            rowSelectedIndicatorColor = rowSelectedIndicatorColor,
            dividerColor = dividerColor,
            pinnedDividerColor = pinnedDividerColor,
            borderColor = borderColor,
            focusIndicatorColor = focusIndicatorColor,
            hoverColor = hoverColor,
            groupContentColor = groupContentColor,
            stickyGroupContainerColor = stickyGroupContainerColor,
        )

    /** Default [TableTypography] derived from [androidx.compose.material3.MaterialTheme.typography]. */
    @Composable
    public fun typography(
        header: TextStyle = MaterialTheme.typography.titleSmall,
        body: TextStyle = MaterialTheme.typography.bodyMedium,
        footer: TextStyle = MaterialTheme.typography.labelLarge,
        groupHeader: TextStyle = MaterialTheme.typography.titleSmall,
    ): TableTypography = TableTypography(header = header, body = body, footer = footer, groupHeader = groupHeader)

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
