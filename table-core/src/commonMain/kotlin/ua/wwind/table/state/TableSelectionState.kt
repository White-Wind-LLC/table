package ua.wwind.table.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings

/**
 * Row and cell selection of one table: the focused row, the checked rows and the selected cell.
 *
 * Reached as [TableState.selection]. Every member here has a deprecated forwarder on [TableState]
 * itself, kept for one release so existing call sites compile with a warning that names the
 * replacement; the forwarders go away in the next major.
 *
 * The selected cell keeps its declared type [TableState.SelectedCell]: it is public API pinned by
 * 2.0.0, and a nested class cannot be re-exported under a new name the way a function can, so
 * moving it here would break every consumer that names the type.
 */
@Stable
public class TableSelectionState<C>
    internal constructor(
        private val settings: TableSettings,
    ) {
        /** Focused row index, or null when nothing is selected. */
        public var selectedIndex: Int? by mutableStateOf(null)
            private set

        /** Checked row indices in [SelectionMode.Multiple]. */
        public val checkedIndices: SnapshotStateList<Int> = mutableStateListOf<Int>()

        /** Selected cell, or null when no cell is selected. */
        public var selectedCell: TableState.SelectedCell<C>? by mutableStateOf(null)
            private set

        /** Toggle row selection for [index] according to [TableSettings.selectionMode]. */
        public fun toggleRow(index: Int) {
            when (settings.selectionMode) {
                SelectionMode.None -> {
                    Unit
                }

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
            selectedCell = TableState.SelectedCell(rowIndex, column)
        }

        /**
         * Selects a cell from contexts where the column type is erased. Used by
         * [ua.wwind.table.component.TableCellTextField] when updating the selected cell on focus change.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun selectCellUnchecked(
            rowIndex: Int,
            column: Any,
        ) {
            selectedCell = TableState.SelectedCell(rowIndex, column as C)
        }

        /**
         * Follows a committed block drag: positional selection state must keep pointing at the rows it
         * pointed at before the permutation. See [TableState.remapRowPositions].
         */
        internal fun remapPositions(remap: (Int) -> Int) {
            selectedIndex = selectedIndex?.let(remap)
            selectedCell = selectedCell?.let { it.copy(rowIndex = remap(it.rowIndex)) }
            if (checkedIndices.isNotEmpty()) {
                val remapped = checkedIndices.map(remap)
                checkedIndices.clear()
                checkedIndices.addAll(remapped)
            }
        }
    }
