package ua.wwind.table.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState

/** Current sort state: which [column] is sorted and in which [order]. */
@Immutable
public data class SortState<C>(
    val column: C,
    val order: SortOrder,
)

/** Column width change request used by resizing logic. */
public sealed interface ColumnWidthAction {
    public data class Set(
        val width: Dp,
    ) : ColumnWidthAction

    public data object Reset : ColumnWidthAction
}

/**
 * Mutable state holder for a table instance. Manages column order/widths, sorting, filters and
 * selection.
 */
@Stable
public class TableState<C>
    internal constructor(
        initialColumns: List<C>,
        initialSort: SortState<C>?,
        initialOrder: List<C>,
        initialWidths: Map<C, Dp>,
        public val settings: TableSettings,
        public val dimensions: TableDimensions,
    ) {
        // Columns order and widths
        public val columnOrder: SnapshotStateList<C> =
            mutableStateListOf<C>().apply { addAll(initialOrder.ifEmpty { initialColumns }) }
        public val columnWidths: SnapshotStateMap<C, Dp> =
            mutableStateMapOf<C, Dp>().apply { putAll(initialWidths) }

        /**
         * Current visible columns list. Must be set externally before tableWidth is accessed.
         */
        internal var visibleColumns: List<ColumnSpec<*, C, *>> = emptyList()

        /**
         * Current table width computed from visible columns and their widths.
         * Automatically recalculates when columnOrder, columnWidths, or visibleColumns change.
         */
        public val tableWidth: Dp by derivedStateOf {
            computeTableWidth(visibleColumns)
        }

        /**
         * Resolves the effective width for a column given its key and optional spec.
         *
         * Resolution priority:
         * 1. User-resized width from [columnWidths]
         * 2. Spec-defined width from [spec]
         * 3. Default width from [dimensions.defaultColumnWidth]
         *
         * @param key column key
         * @param spec optional column spec; if null, only [columnWidths] and default are considered
         * @return effective column width
         */
        public fun resolveColumnWidth(
            key: C,
            spec: ColumnSpec<*, C, *>? = null,
        ): Dp = columnWidths[key] ?: spec?.width ?: dimensions.defaultColumnWidth

        /**
         * Computes the total table width from visible columns and dividers.
         */
        private fun computeTableWidth(columns: List<ColumnSpec<*, C, *>>): Dp {
            val effectivePinnedCount =
                if (settings.pinnedColumnsCount >= columns.size) 0 else settings.pinnedColumnsCount

            // Sum column widths (use stored widths, spec width or default)
            val columnsTotal: Dp =
                columns.fold(0.dp) { acc, spec ->
                    val w = resolveColumnWidth(spec.key, spec)
                    acc + w
                }

            // Calculate divider contribution:
            // - If there are no pinned columns: each column has its regular divider (count = columns)
            // - If there are pinned columns: all but one divider are regular, and one between pinned and scrollable is
            //   thicker
            val dividerTotal: Dp =
                if (effectivePinnedCount == 0) {
                    dimensions.dividerThickness * columns.size
                } else {
                    // total dividers = columns count, but one of them uses pinnedColumnDividerThickness
                    dimensions.dividerThickness * (columns.size - 1) + dimensions.pinnedColumnDividerThickness
                }

            return columnsTotal + dividerTotal
        }

        /**
         * Tracks the maximum measured minimal content width per column across visible rows. Used to
         * auto-fit columns on demand.
         */
        public val columnContentMaxWidths: SnapshotStateMap<C, Dp> = mutableStateMapOf()

        /** Whether automatic width fitting has been applied for the empty (header-only) state. */
        public var autoWidthAppliedForEmpty: Boolean by mutableStateOf(false)

        /** Whether automatic width fitting has been applied for the first data batch render. */
        public var autoWidthAppliedForData: Boolean by mutableStateOf(false)

        // Sorting
        public var sort: SortState<C>? by mutableStateOf(initialSort)
            private set

        // Grouping by column
        public var groupBy: C? by mutableStateOf(null)
            private set

        // Filters per column
        public val filters: MutableMap<C, TableFilterState<*>> =
            mutableStateMapOf<C, TableFilterState<*>>()

        // Selection (by index)
        public var selectedIndex: Int? by mutableStateOf(null)
            private set
        public val checkedIndices: SnapshotStateList<Int> = mutableStateListOf<Int>()

        // Cell selection
        public data class SelectedCell<C>(
            val rowIndex: Int,
            val column: C,
        )

        public var selectedCell: SelectedCell<C>? by mutableStateOf(null)
            private set

        /** Currently editing row index, or null if not editing */
        public var editingRow: Int? by mutableStateOf(null)
            private set

        /** Currently focused editable column, or null if not editing */
        public var editingColumn: C? by mutableStateOf(null)
            private set

        /** Callback when row editing starts. Only called when item is non-null. */
        public var onRowEditStart: ((rowIndex: Int) -> Unit)? = null
            private set

        /**
         * Callback for row edit completion validation. Returns true to allow exit, false to stay in
         * edit mode.
         */
        public var onRowEditComplete: ((rowIndex: Int) -> Boolean)? = null
            private set

        /** Callback when editing is cancelled */
        public var onEditCancelled: ((rowIndex: Int) -> Unit)? = null
            private set

        /**
         * Move a column from [fromIndex] to [toIndex] within the current order. Indices are validated;
         * dropping after the last element is supported.
         */
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

        /**
         * Replace current column order with [newOrder]. Missing keys are ignored; unknown keys
         * appended.
         */
        public fun setColumnOrder(newOrder: List<C>) {
            val current = columnOrder.toList()
            val filtered = newOrder.filter { current.contains(it) }
            val remaining = current.filterNot { filtered.contains(it) }
            columnOrder.clear()
            columnOrder.addAll(filtered + remaining)
        }

        /** Apply a width [action] for a [column] (set or reset override). */
        public fun resizeColumn(
            column: C,
            action: ColumnWidthAction,
        ) {
            when (action) {
                is ColumnWidthAction.Set -> columnWidths[column] = action.width
                ColumnWidthAction.Reset -> columnWidths.remove(column)
            }
        }

        /** Apply external [widths] in bulk. Null width removes override for that column. */
        public fun setColumnWidths(widths: Map<C, Dp?>) {
            widths.forEach { (col, width) ->
                if (width == null) columnWidths.remove(col) else columnWidths[col] = width
            }
        }

        /** Toggle or set sorting for a [column]. If [order] is null, cycles ASC -> DESC -> none. */
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

        /** Enable or disable grouping by a [column] */
        public fun groupBy(column: C?) {
            groupBy = column
        }

        /** Set or clear filter [state] for [column]. Pass null to remove. */
        @Suppress("UNCHECKED_CAST")
        public fun <T> setFilter(
            column: C,
            state: TableFilterState<T>?,
        ) {
            if (state == null) {
                filters.remove(column)
            } else {
                filters[column] = state as TableFilterState<*>
            }
        }

        /** Toggle row selection for [index] according to [settings.selectionMode]. */
        public fun toggleSelect(index: Int) {
            when (settings.selectionMode) {
                SelectionMode.None -> Unit

                SelectionMode.Single -> {
                    selectedIndex = if (selectedIndex == index) null else index
                }

                SelectionMode.Multiple -> {
                    // In multiple mode, keep selection for focus but primary is checked set
                    selectedIndex = index
                }
            }
        }

        /**
         * Set focused row to [index] without toggling selection.
         *
         * Used by keyboard navigation to keep row selection in sync with the focused cell when
         * selection mode is enabled.
         */
        public fun focusRow(index: Int) {
            if (settings.selectionMode == SelectionMode.None) return
            selectedIndex = index
        }

        /** Toggle checkmark state for [index] in Multiple selection mode. */
        public fun toggleCheck(index: Int) {
            if (settings.selectionMode != SelectionMode.Multiple) return
            if (checkedIndices.contains(index)) {
                checkedIndices.remove(index)
            } else {
                checkedIndices.add(index)
            }
        }

        /** Check/uncheck all rows based on current [count] in Multiple selection mode. */
        public fun toggleCheckAll(count: Int) {
            if (settings.selectionMode != SelectionMode.Multiple) return
            if (checkedIndices.size == count) {
                checkedIndices.clear()
            } else {
                checkedIndices.clear()
                checkedIndices.addAll(0 until count)
            }
        }

        /** Select a specific cell at [rowIndex] and [column]. */
        public fun selectCell(
            rowIndex: Int,
            column: C,
        ) {
            selectedCell = SelectedCell(rowIndex, column)
        }

        /**
         * Internal helper for selecting a cell from contexts where the column type is erased. Used by
         * [TableCellTextField] when updating selectedCell on focus change.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun selectCellUnchecked(
            rowIndex: Int,
            column: Any,
        ) {
            selectedCell = SelectedCell(rowIndex, column as C)
            if (editingRow != null) {
                editingColumn = selectedCell?.column
            }
        }

        /**
         * Update the tracked maximum minimal content width for a [column]. If the provided [width] is
         * greater than the stored value, it will be recorded.
         */
        public fun updateMaxContentWidth(
            column: C,
            width: Dp,
        ) {
            val current = columnContentMaxWidths[column]
            if (current == null || width > current) {
                columnContentMaxWidths[column] = width
            }
        }

        /**
         * Set the column width override to the tracked maximum minimal content width (if available).
         * No-op if no measured width is present for the [column].
         */
        public fun setColumnWidthToMaxContent(column: C) {
            val width = columnContentMaxWidths[column] ?: return
            columnWidths[column] = width
        }

        /**
         * Recalculate auto-widths for columns with `autoWidth` enabled.
         *
         * This method is useful for scenarios with deferred/paginated data loading where initial
         * auto-width calculation happened on empty data. After data loads and content is measured, call
         * this method to recompute column widths based on the actual content.
         */
        public fun recalculateAutoWidths() {
            // Reset flags to allow ApplyAutoWidthEffect to recompute on next frame
            autoWidthAppliedForEmpty = false
            autoWidthAppliedForData = false
            columnContentMaxWidths.clear()
        }

        /** Tracks measured row heights in raw pixels for dynamic, precise scrolling. */
        public val rowHeightsPx: SnapshotStateMap<Int, Int> = mutableStateMapOf()

        /** Record measured row height (in px) for [index]. */
        public fun updateRowHeight(
            index: Int,
            heightPx: Int,
        ) {
            val current = rowHeightsPx[index]
            if (current == null || current != heightPx) {
                rowHeightsPx[index] = heightPx
            }
        }

        /**
         * Set edit mode callbacks.
         *
         * @param onStart callback when row editing starts
         * @param onComplete callback to validate row edit completion (returns true to allow, false to
         * block)
         * @param onCancel callback when editing is cancelled
         */
        internal fun setEditCallbacks(
            onStart: ((rowIndex: Int) -> Unit)?,
            onComplete: ((rowIndex: Int) -> Boolean)?,
            onCancel: ((rowIndex: Int) -> Unit)?,
        ) {
            onRowEditStart = onStart
            onRowEditComplete = onComplete
            onEditCancelled = onCancel
        }

        /**
         * Start editing a specific row and column.
         *
         * If another row is currently being edited, attempts to complete it first. If completion is
         * blocked, updates selectedCell to scroll to the editing row and returns false.
         *
         * @param item the item to edit (must be non-null)
         * @param rowIndex the row to edit
         * @param column the column to start editing in
         * @return true if edit mode was activated, false if blocked by existing edit or item is null
         */
        public fun startEditing(
            item: Any?,
            rowIndex: Int,
            column: C,
        ): Boolean {
            // Verify that item exists before allowing edit
            if (item == null) {
                return false // Cannot edit null item
            }

            val currentEditRow = editingRow

            // If editing a different row, try to complete that first
            if (currentEditRow != null && currentEditRow != rowIndex) {
                if (!tryCompleteEditing()) {
                    // Completion blocked - scroll to the editing row
                    val editingCol = editingColumn
                    if (editingCol != null) {
                        selectedCell = SelectedCell(currentEditRow, editingCol)
                    }
                    return false
                }
            }

            // Start new edit
            editingRow = rowIndex
            editingColumn = column
            selectedCell = SelectedCell(rowIndex, column)

            // Call onRowEditStart callback (item is guaranteed non-null here)
            onRowEditStart?.invoke(rowIndex)

            return true
        }

        /**
         * Attempt to complete the current row edit. Calls onRowEditComplete callback to validate.
         *
         * @return true if edit was completed (or no edit was active), false if blocked by callback
         */
        public fun tryCompleteEditing(): Boolean {
            val currentRow = editingRow ?: return true

            val callback = onRowEditComplete
            val allowed = callback?.invoke(currentRow) ?: true

            if (allowed) {
                editingRow = null
                editingColumn = null
            }

            return allowed
        }

        /**
         * Complete editing the current cell and move to the next editable column. If no more editable
         * columns in the row, attempts to complete row edit.
         */
        public fun completeCurrentCellEdit(visibleColumns: List<ColumnSpec<*, C, *>>) {
            val currentRow = editingRow ?: return
            val currentCol = editingColumn ?: return

            // Find current column index
            val currentIndex = columnOrder.indexOf(currentCol)
            if (currentIndex == -1) return

            // Find next editable column in order
            val nextEditableColumn =
                columnOrder.drop(currentIndex + 1).firstOrNull { colKey ->
                    visibleColumns.any { it.key == colKey && it.editable }
                }

            if (nextEditableColumn != null) {
                // Move to next editable column
                editingColumn = nextEditableColumn
                selectedCell = SelectedCell(currentRow, nextEditableColumn)
            } else {
                // No more editable columns - try to complete row
                tryCompleteEditing()
            }
        }

        /** Cancel editing without validation. Calls onEditCancelled callback and clears edit state. */
        public fun cancelEditing() {
            val currentRow = editingRow
            if (currentRow != null) {
                onEditCancelled?.invoke(currentRow)
            }
            editingRow = null
            editingColumn = null
        }
    }

public inline fun <T, R : Any> Iterable<T>.mapNotNullToImmutable(transform: (T) -> R?): ImmutableList<R> =
    buildList {
        for (item in this@mapNotNullToImmutable) {
            transform(item)?.let(::add)
        }
    }.toPersistentList()

/**
 * Remember and create a [TableState] tied to the composition. Initial parameters are used only
 * once; runtime mutations will not recreate the state.
 */
@Composable
@Suppress("LongParameterList")
public fun <C> rememberTableState(
    columns: ImmutableList<C>,
    initialSort: SortState<C>? = null,
    initialOrder: ImmutableList<C> = columns,
    initialWidths: ImmutableMap<C, Dp> = persistentMapOf(),
    settings: TableSettings = TableSettings(),
    dimensions: TableDimensions = TableDefaults.standardDimensions(),
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
