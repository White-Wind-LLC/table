package ua.wwind.table.interaction

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.ColumnSpec
import ua.wwind.table.computeAutoWidths
import ua.wwind.table.state.TableState

@Composable
internal fun <C, T : Any> ApplyAutoWidthEffect(
    visibleColumns: ImmutableList<ColumnSpec<T, C>>,
    itemsCount: Int,
    verticalState: LazyListState,
    state: TableState<C>,
) {
    LaunchedEffect(visibleColumns, itemsCount, state) {
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
                val widths = computeAutoWidths(visibleColumns, state)
                if (widths.isNotEmpty()) state.setColumnWidths(widths)
                state.autoWidthAppliedForEmpty = true
            }

            // Phase 2: first visible data
            if (!dataApplied && hasVisibleItems && hasAnyMeasured) {
                val widths = computeAutoWidths(visibleColumns, state)
                if (widths.isNotEmpty()) state.setColumnWidths(widths)
                state.autoWidthAppliedForEmpty = true
                state.autoWidthAppliedForData = true
            }
        }
    }
}
