package ua.wwind.table

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.state.TableState

internal fun <T : Any, C> computeTableWidth(
    visibleColumns: List<ColumnSpec<T, C>>,
    hasLeading: Boolean,
    state: TableState<C>,
): Dp {
    val dimensions = state.dimensions
    var sum = 0.dp
    if (hasLeading) {
        sum += dimensions.rowHeight + dimensions.dividerThickness
    }
    visibleColumns.forEach { spec ->
        val w = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
        sum += w + dimensions.dividerThickness
    }
    return sum
}

internal fun <C> computeAutoWidths(
    visibleColumns: List<ColumnSpec<*, C>>,
    state: TableState<C>,
): Map<C, Dp> {
    return buildMap {
        visibleColumns.forEach { spec ->
            if (spec.autoWidth) {
                val measured = state.columnContentMaxWidths[spec.key]
                val fallback = spec.width ?: state.dimensions.defaultColumnWidth
                val base = measured ?: fallback
                val minClamped = maxOf(base, spec.minWidth)
                val finalWidth = spec.autoMaxWidth?.let { maxCap ->
                    if (minClamped > maxCap) maxCap else minClamped
                } ?: minClamped
                put(spec.key, finalWidth)
            }
        }
    }
}
