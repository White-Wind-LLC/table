package ua.wwind.table.config

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Line height Compose uses for a style that only sets a font size. */
private const val FALLBACK_LINE_HEIGHT_RATIO = 1.2f

/**
 * Grows each height by how much one line of its text grew above 1.0× font scale; `this` when nothing
 * grows. Converting through [Density.toDp] rather than multiplying by `fontScale` keeps it right under
 * Android's non-linear font scaling.
 */
internal fun TableDimensions.scaledForFont(
    typography: TableTypography,
    density: Density,
): TableDimensions {
    if (!scaleWithFontSize) return this
    val body = density.lineGrowth(typography.body)
    val row = maxOf(body, density.lineGrowth(typography.groupHeader))
    val header = density.lineGrowth(typography.header)
    val footer = density.lineGrowth(typography.footer)
    if (row == 0.dp && header == 0.dp && footer == 0.dp) return this
    return copy(
        rowHeight = rowHeight + row,
        headerHeight = headerHeight + header,
        footerHeight = footerHeight + footer,
        fastFilterRowHeight = fastFilterRowHeight + body,
    )
}

private fun Density.lineGrowth(style: TextStyle): Dp {
    val line =
        when {
            style.lineHeight.isSp -> style.lineHeight
            style.fontSize.isSp -> (style.fontSize.value * FALLBACK_LINE_HEIGHT_RATIO).sp
            else -> return 0.dp
        }
    return (line.toDp() - line.value.dp).coerceAtLeast(0.dp)
}
