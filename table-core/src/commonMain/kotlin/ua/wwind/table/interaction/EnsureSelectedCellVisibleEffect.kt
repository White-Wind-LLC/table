package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState

@Composable
@Suppress("LongParameterList")
@ExperimentalTableApi
internal fun <T : Any, C, E> EnsureSelectedCellVisibleEffect(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    verticalState: LazyListState,
    horizontalState: ScrollState,
) {
    @Suppress("UNCHECKED_CAST")
    val state = currentTableState() as TableState<C>
    val density = LocalDensity.current
    var previousSelectedRowIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state) {
        snapshotFlow { state.selectedCell }.collectLatest { cell ->
            if (cell == null) return@collectLatest
            val colIndex = visibleColumns.indexOfFirst { it.key == cell.column }
            if (colIndex >= 0) {
                val prevRow = previousSelectedRowIndex
                val movement =
                    if (prevRow != null && cell.rowIndex != prevRow) {
                        if (cell.rowIndex > prevRow) 1 else -1
                    } else {
                        0
                    }
                ensureCellFullyVisible(
                    rowIndex = cell.rowIndex,
                    targetColIndex = colIndex,
                    targetColKey = cell.column,
                    visibleColumns = visibleColumns,
                    state = state,
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    density = density,
                    movement = movement,
                )
                previousSelectedRowIndex = cell.rowIndex
            }
        }
    }
}
