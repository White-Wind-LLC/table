package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
)
