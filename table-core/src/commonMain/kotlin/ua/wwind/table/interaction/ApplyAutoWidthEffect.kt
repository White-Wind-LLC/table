package ua.wwind.table.interaction

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.ColumnSpec
import ua.wwind.table.computeAutoWidths
import ua.wwind.table.state.TableState

private val logger = Logger.withTag("TableAutoWidth")

@Composable
internal fun <C, T : Any, E> ApplyAutoWidthEffect(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    itemsCount: Int,
    verticalState: LazyListState,
    state: TableState<C>,
) {
    LaunchedEffect(visibleColumns, itemsCount, state) {
        // Wait a frame to ensure all cells are measured
        withFrameNanos { /* NoOp */ }
        snapshotFlow {
            Triple(
                itemsCount,
                verticalState.layoutInfo.visibleItemsInfo.isNotEmpty(),
                Triple(
                    state.autoWidthAppliedForEmpty,
                    state.autoWidthAppliedForData,
                    state.columnContentMaxWidths.size,
                ),
            )
        }.collectLatest { (count, hasVisibleItems, appliedFlags) ->
            val (emptyApplied, dataApplied, _) = appliedFlags
            val autoColumns = visibleColumns.filter { it.autoWidth }
            val hasAnyMeasured = autoColumns.any { state.columnContentMaxWidths.containsKey(it.key) }

            // Phase 1: empty table
            if (!emptyApplied && count == 0 && hasAnyMeasured) {
                logger.v { "AutoWidth Phase1: empty table, computing widths" }
                val widths = computeAutoWidths(visibleColumns, state)
                if (widths.isNotEmpty()) {
                    logger.v { "AutoWidth Phase1: applying widths=$widths" }
                    state.setColumnWidths(widths)
                }
                state.autoWidthAppliedForEmpty = true
            }

            // Phase 2: first visible data
            if (!dataApplied && hasVisibleItems && hasAnyMeasured) {
                logger.v { "AutoWidth Phase2: first data visible, computing widths (count=$count)" }
                val widths = computeAutoWidths(visibleColumns, state)
                if (widths.isNotEmpty()) {
                    logger.v { "AutoWidth Phase2: applying widths=$widths" }
                    state.setColumnWidths(widths)
                }
                state.autoWidthAppliedForEmpty = true
                state.autoWidthAppliedForData = true
            }
        }
    }
}

/**
 * For embedded tables all rows are rendered immediately, so we can apply auto-widths
 * as soon as content measurements are available.
 */
@Composable
internal fun <C, T : Any, E> ApplyAutoWidthEmbeddedEffect(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    itemsCount: Int,
    state: TableState<C>,
) {
    LaunchedEffect(visibleColumns, itemsCount, state) {
        // Wait a frame to ensure all cells are measured
        withFrameNanos { /* NoOp */ }

        snapshotFlow {
            Pair(
                itemsCount,
                Triple(
                    state.autoWidthAppliedForEmpty,
                    state.autoWidthAppliedForData,
                    state.columnContentMaxWidths.size,
                ),
            )
        }.collectLatest { (count, appliedFlags) ->
            val (emptyApplied, dataApplied, _) = appliedFlags
            val autoColumns = visibleColumns.filter { it.autoWidth }
            val hasAnyMeasured = autoColumns.any { state.columnContentMaxWidths.containsKey(it.key) }

            // Phase 1: empty table
            if (!emptyApplied && count == 0 && hasAnyMeasured) {
                logger.v { "AutoWidth Embedded Phase1: empty table, computing widths" }
                val widths = computeAutoWidths(visibleColumns, state)
                if (widths.isNotEmpty()) {
                    logger.v { "AutoWidth Embedded Phase1: applying widths=$widths" }
                    state.setColumnWidths(widths)
                }
                state.autoWidthAppliedForEmpty = true
            }

            // Phase 2: embedded table with data - apply as soon as measurements are available
            // For embedded tables, wait until all auto-width columns have measurements
            if (!dataApplied && count > 0) {
                val allAutoColumnsHaveMeasurements =
                    autoColumns.all { state.columnContentMaxWidths.containsKey(it.key) }

                if (allAutoColumnsHaveMeasurements && autoColumns.isNotEmpty()) {
                    logger.v { "AutoWidth Embedded Phase2: all columns measured, computing widths (count=$count)" }
                    val widths = computeAutoWidths(visibleColumns, state)
                    if (widths.isNotEmpty()) {
                        logger.v { "AutoWidth Embedded Phase2: applying widths=$widths" }
                        state.setColumnWidths(widths)
                    }
                    state.autoWidthAppliedForEmpty = true
                    state.autoWidthAppliedForData = true
                }
            }
        }
    }
}
