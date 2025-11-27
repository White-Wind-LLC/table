package ua.wwind.table

import androidx.compose.ui.unit.Dp
import ua.wwind.table.state.TableState

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
