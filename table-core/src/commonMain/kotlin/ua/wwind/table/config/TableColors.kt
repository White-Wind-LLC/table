package ua.wwind.table.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
/**
 * Color palette used by the table header and rows.
 */
public data class TableColors(
    val headerContainerColor: Color,
    val headerContentColor: Color,
    val rowContainerColor: Color,
    val rowSelectedContainerColor: Color,
    val stripedRowContainerColor: Color,
    val groupContainerColor: Color,
)

public object TableDefaults {
    @Composable
    /** Convenience factory for default [TableColors] derived from [MaterialTheme]. */
    public fun colors(
        headerContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        headerContentColor: Color = MaterialTheme.colorScheme.contentColorFor(headerContainerColor),
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
}
