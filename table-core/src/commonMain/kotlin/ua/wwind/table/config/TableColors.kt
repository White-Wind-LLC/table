package ua.wwind.table.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/** Color palette used by the table header and rows. */
@Immutable
public data class TableColors(
    val headerContainerColor: Color,
    val headerContentColor: Color,
    val rowContainerColor: Color,
    val rowSelectedContainerColor: Color,
    val stripedRowContainerColor: Color,
    val groupContainerColor: Color,
    val footerContainerColor: Color,
    val footerContentColor: Color,
    /**
     * Container painted behind a row block declared via `rowBlocks`. Appended with a default —
     * never inserted as a required parameter — so pre-blocks positional constructor calls keep
     * compiling; [Color.Unspecified] resolves to `surfaceContainerHighest` at draw.
     */
    val rowBlockContainerColor: Color = Color.Unspecified,
)

/**
 * Resolved at draw rather than defaulted in [TableDefaults.colors] so that a [TableColors] built
 * directly — without the composable factory — still lands on the themed band color instead of
 * painting a transparent band.
 */
@Composable
@ReadOnlyComposable
internal fun resolveRowBlockContainerColor(colors: TableColors): Color =
    colors.rowBlockContainerColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHighest }
