package ua.wwind.table

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.state.TableState

internal fun <T : Any, C> computeTableWidth(
    visibleColumns: List<ColumnSpec<T, C, *>>,
    state: TableState<C>,
): Dp {
    val dimensions = state.dimensions
    val settings = state.settings
    val effectiveFixedCount = if (settings.fixedColumnsCount >= visibleColumns.size) 0 else settings.fixedColumnsCount

    // Sum column widths (use stored widths, spec width or default)
    val columnsTotal: Dp =
        visibleColumns.fold(0.dp) { acc, spec ->
            val w = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
            acc + w
        }

    // Calculate divider contribution:
    // - If there are no fixed columns: each column has its regular divider (count = columns)
    // - If there are fixed columns: all but one divider are regular, and one between fixed and scrollable is thicker
    val dividerTotal: Dp =
        if (effectiveFixedCount == 0) {
            dimensions.dividerThickness * visibleColumns.size
        } else {
            // total dividers = columns count, but one of them uses fixedColumnDividerThickness
            dimensions.dividerThickness * (visibleColumns.size - 1) + dimensions.fixedColumnDividerThickness
        }

    return columnsTotal + dividerTotal
}

internal fun <C> computeAutoWidths(
    visibleColumns: List<ColumnSpec<*, C, *>>,
    state: TableState<C>,
): Map<C, Dp> =
    buildMap {
        visibleColumns.forEach { spec ->
            if (spec.autoWidth) {
                val measured = state.columnContentMaxWidths[spec.key]
                val fallback = spec.width ?: state.dimensions.defaultColumnWidth
                val base = measured ?: fallback
                val minClamped = maxOf(base, spec.minWidth)
                val finalWidth =
                    spec.autoMaxWidth?.let { maxCap ->
                        if (minClamped > maxCap) maxCap else minClamped
                    } ?: minClamped
                put(spec.key, finalWidth)
            }
        }
    }
