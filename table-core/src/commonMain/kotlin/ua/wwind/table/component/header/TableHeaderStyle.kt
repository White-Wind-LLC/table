package ua.wwind.table.component.header

import androidx.compose.ui.graphics.Color
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.config.TableDimensions

internal data class TableHeaderStyle(
    val headerColor: Color,
    val headerContentColor: Color,
    val dimensions: TableDimensions,
    val icons: TableHeaderIcons,
)
