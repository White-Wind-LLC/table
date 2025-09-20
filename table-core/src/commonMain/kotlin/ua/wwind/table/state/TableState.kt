package ua.wwind.table.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.Dp
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState

@Immutable
public data class SortState<C>(
    val column: C,
    val order: SortOrder,
)

public sealed interface ColumnWidthAction {
    public data class Set(
        val width: Dp,
    ) : ColumnWidthAction

    public data object Reset : ColumnWidthAction
}

@Stable
public class TableState<C> internal constructor(
    initialColumns: List<C>,
    initialSort: SortState<C>?,
    initialOrder: List<C>,
    initialWidths: Map<C, Dp>,
    internal val settings: TableSettings,
    internal val dimensions: TableDimensions,
) {
    // Columns order and widths
    public val columnOrder: SnapshotStateList<C> =
        mutableStateListOf<C>().apply { addAll(initialOrder.ifEmpty { initialColumns }) }
    public val columnWidths: SnapshotStateMap<C, Dp> = mutableStateMapOf<C, Dp>().apply { putAll(initialWidths) }

    // Sorting
    public var sort: SortState<C>? by mutableStateOf(initialSort)
        private set

    // Filters per column
    public val filters: MutableMap<C, TableFilterState<*>> = mutableStateMapOf<C, TableFilterState<*>>()

    // Selection (by index)
    public var selectedIndex: Int? by mutableStateOf(null)
        private set
    public val checkedIndices: SnapshotStateList<Int> = mutableStateListOf<Int>()

    public fun moveColumn(
        fromIndex: Int,
        toIndex: Int,
    ) {
        // Guard against invalid indices and no-op moves
        val size = columnOrder.size
        if (size < 2) return
        if (fromIndex !in 0 until size) return

        // Allow dropping after the last element (append)
        var targetIndex = toIndex.coerceIn(0, size)
        if (fromIndex == targetIndex || fromIndex == targetIndex - 1) return

        val column = columnOrder.removeAt(fromIndex)
        // After removal, adjust target when moving forward
        if (targetIndex > fromIndex) targetIndex--
        columnOrder.add(targetIndex, column)
    }

    /** Replace current column order with an externally provided order. Missing keys are ignored; unknown keys appended. */
    public fun setColumnOrder(newOrder: List<C>) {
        val current = columnOrder.toList()
        val filtered = newOrder.filter { current.contains(it) }
        val remaining = current.filterNot { filtered.contains(it) }
        columnOrder.clear()
        columnOrder.addAll(filtered + remaining)
    }

    public fun resizeColumn(
        column: C,
        action: ColumnWidthAction,
    ) {
        when (action) {
            is ColumnWidthAction.Set -> columnWidths[column] = action.width
            ColumnWidthAction.Reset -> columnWidths.remove(column)
        }
    }

    /** Apply external widths in bulk. Null width removes override for that column. */
    public fun setColumnWidths(widths: Map<C, Dp?>) {
        widths.forEach { (col, width) ->
            if (width == null) columnWidths.remove(col) else columnWidths[col] = width
        }
    }

    public fun setSort(
        column: C,
        order: SortOrder? = null,
    ) {
        sort =
            if (order != null) {
                SortState(column, order)
            } else {
                val current = sort
                if (current == null || current.column != column) {
                    SortState(column, SortOrder.ASCENDING)
                } else {
                    when (current.order) {
                        SortOrder.ASCENDING -> SortState(column, SortOrder.DESCENDING)
                        SortOrder.DESCENDING -> null
                    }
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T> setFilter(
        column: C,
        state: TableFilterState<T>?,
    ) {
        if (state == null) filters.remove(column) else filters[column] = state as TableFilterState<*>
    }

    public fun toggleSelect(index: Int) {
        when (settings.selectionMode) {
            SelectionMode.None -> Unit
            SelectionMode.Single -> selectedIndex = if (selectedIndex == index) null else index
            SelectionMode.Multiple -> {
                // In multiple mode, keep selection for focus but primary is checked set
                selectedIndex = index
            }
        }
    }

    public fun toggleCheck(index: Int) {
        if (settings.selectionMode != SelectionMode.Multiple) return
        if (checkedIndices.contains(index)) checkedIndices.remove(index) else checkedIndices.add(index)
    }

    public fun toggleCheckAll(count: Int) {
        if (settings.selectionMode != SelectionMode.Multiple) return
        if (checkedIndices.size == count) {
            checkedIndices.clear()
        } else {
            checkedIndices.clear()
            checkedIndices.addAll(0 until count)
        }
    }
}

@Composable
@Suppress("LongParameterList")
public fun <C> rememberTableState(
    columns: List<C>,
    initialSort: SortState<C>? = null,
    initialOrder: List<C> = columns,
    initialWidths: Map<C, Dp> = emptyMap(),
    settings: TableSettings = TableSettings(),
    dimensions: TableDimensions = TableDimensions(),
): TableState<C> {
    // Important: Do not include initialOrder/initialWidths/initialSort in the remember keys.
    // These parameters should only be used for initial state, not for triggering state recreation
    // on every reorder/resize/sort. Recreating the state would wipe runtime data such as filters.
    return remember(columns, settings, dimensions) {
        TableState(
            initialColumns = columns,
            initialSort = initialSort,
            initialOrder = initialOrder,
            initialWidths = initialWidths,
            settings = settings,
            dimensions = dimensions,
        )
    }
}
