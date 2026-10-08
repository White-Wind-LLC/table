package ua.wwind.table

import androidx.compose.ui.unit.Dp
import co.touchlab.kermit.Logger
import ua.wwind.table.state.TableColumnsState
import ua.wwind.table.state.TableState

private val logger = Logger.withTag("TableAutoWidth")

/**
 * Auto-fit widths for the `autoWidth` columns among [visibleColumns]. Columns with an explicit width
 * are skipped unless [TableColumnsState.autoFitOverridesExplicit] is set.
 */
internal fun <C> computeAutoWidths(
    visibleColumns: List<ColumnSpec<*, C, *>>,
    state: TableState<C>,
): Map<C, Dp> =
    buildMap {
        visibleColumns.forEach { spec ->
            if (spec.autoWidth && state.columns.needsAutoFit(spec.key)) {
                val measured = state.columns.contentMaxWidths[spec.key]
                val fallback = spec.width ?: state.dimensions.defaultColumnWidth
                val base = measured ?: fallback
                val minClamped = maxOf(base, spec.minWidth)
                val finalWidth =
                    spec.autoMaxWidth?.let { maxCap ->
                        if (minClamped > maxCap) maxCap else minClamped
                    } ?: minClamped
                logger.v { "AutoWidth: computed column=${spec.key} measured=$measured base=$base final=$finalWidth" }
                put(spec.key, finalWidth)
            }
        }
    }
