package ua.wwind.table.component.header

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.config.TableDimensions

/** Glyph size shared by the sort, filter and column menu icons in a header. */
internal val HeaderIconSize = 24.dp

@Immutable
internal data class TableHeaderStyle(
    val headerColor: Color,
    val headerContentColor: Color,
    val dimensions: TableDimensions,
    val icons: TableHeaderIcons,
)
