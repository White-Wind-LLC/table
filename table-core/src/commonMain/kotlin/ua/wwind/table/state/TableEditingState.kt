package ua.wwind.table.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ua.wwind.table.ColumnSpec

/**
 * Row editing of one table: which row and column are being edited, and the consumer's edit
 * callbacks.
 *
 * Reached as [TableState.editing]. Every member here has a deprecated forwarder on [TableState]
 * itself, kept for one release so existing call sites compile with a warning that names the
 * replacement; the forwarders go away in the next major.
 *
 * Editing drives [selection]: starting an edit selects the cell it starts in, and a refused
 * completion re-selects the cell the consumer must return to. The dependency runs one way — the
 * selection holder knows nothing about editing — so the pair cannot deadlock into mutual updates.
 */
@Stable
public class TableEditingState<C>
    internal constructor(
        private val columns: TableColumnsState<C>,
        private val selection: TableSelectionState<C>,
    ) {
        /** Currently editing row index, or null if not editing. */
        public var rowIndex: Int? by mutableStateOf(null)
            private set

        /** Currently focused editable column, or null if not editing. */
        public var column: C? by mutableStateOf(null)
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

        /** Callback when editing is cancelled. */
        public var onEditCancel: ((rowIndex: Int) -> Unit)? = null
            private set

        /**
         * Set edit mode callbacks.
         *
         * @param onStart callback when row editing starts
         * @param onComplete callback to validate row edit completion (returns true to allow, false to
         * block)
         * @param onCancel callback when editing is cancelled
         */
        internal fun setCallbacks(
            onStart: ((rowIndex: Int) -> Unit)?,
            onComplete: ((rowIndex: Int) -> Boolean)?,
            onCancel: ((rowIndex: Int) -> Unit)?,
        ) {
            onRowEditStart = onStart
            onRowEditComplete = onComplete
            onEditCancel = onCancel
        }

        /**
         * Start editing a specific row and column.
         *
         * If another row is currently being edited, attempts to complete it first. If completion is
         * blocked, updates the selected cell to scroll to the editing row and returns false.
         *
         * @param item the item to edit (must be non-null)
         * @param rowIndex the row to edit
         * @param column the column to start editing in
         * @return true if edit mode was activated, false if blocked by existing edit or item is null
         */
        public fun start(
            item: Any?,
            rowIndex: Int,
            column: C,
        ): Boolean {
            // Verify that item exists before allowing edit
            if (item == null) {
                return false // Cannot edit null item
            }

            val currentEditRow = this.rowIndex

            // If editing a different row, try to complete that first
            if (currentEditRow != null && currentEditRow != rowIndex) {
                if (!tryComplete()) {
                    // Completion blocked - scroll to the editing row
                    val editingCol = this.column
                    if (editingCol != null) {
                        selection.selectCell(currentEditRow, editingCol)
                    }
                    return false
                }
            }

            // Start new edit
            this.rowIndex = rowIndex
            this.column = column
            selection.selectCell(rowIndex, column)

            // Call onRowEditStart callback (item is guaranteed non-null here)
            onRowEditStart?.invoke(rowIndex)

            return true
        }

        /**
         * Attempt to complete the current row edit. Calls [onRowEditComplete] to validate.
         *
         * @return true if edit was completed (or no edit was active), false if blocked by callback
         */
        public fun tryComplete(): Boolean {
            val currentRow = rowIndex ?: return true

            val callback = onRowEditComplete
            val allowed = callback?.invoke(currentRow) ?: true

            if (allowed) {
                rowIndex = null
                column = null
            }

            return allowed
        }

        /**
         * Complete editing the current cell and move to the next editable column. If no more editable
         * columns in the row, attempts to complete row edit.
         */
        public fun completeCurrentCell(visibleColumns: List<ColumnSpec<*, C, *>>) {
            val currentRow = rowIndex ?: return
            val currentCol = column ?: return

            // Find current column index
            val currentIndex = columns.order.indexOf(currentCol)
            if (currentIndex == -1) return

            // Find next editable column in order
            val nextEditableColumn =
                columns.order.drop(currentIndex + 1).firstOrNull { colKey ->
                    visibleColumns.any { it.key == colKey && it.editable }
                }

            if (nextEditableColumn != null) {
                // Move to next editable column
                column = nextEditableColumn
                selection.selectCell(currentRow, nextEditableColumn)
            } else {
                // No more editable columns - try to complete row
                tryComplete()
            }
        }

        /** Cancel editing without validation. Calls [onEditCancel] and clears edit state. */
        public fun cancel() {
            val currentRow = rowIndex
            if (currentRow != null) {
                onEditCancel?.invoke(currentRow)
            }
            rowIndex = null
            column = null
        }

        /**
         * Keeps the edited column pointing at the cell the selection just moved to. A no-op outside an
         * active edit. See [TableState.selectCellUnchecked].
         */
        internal fun followSelectedColumn() {
            if (rowIndex != null) column = selection.selectedCell?.column
        }

        /**
         * Follows a committed block drag: the edited row must keep pointing at the row it pointed at
         * before the permutation. See [TableState.remapRowPositions].
         */
        internal fun remapRow(remap: (Int) -> Int) {
            rowIndex = rowIndex?.let(remap)
        }
    }
