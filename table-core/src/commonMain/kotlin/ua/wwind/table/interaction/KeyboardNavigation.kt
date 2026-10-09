package ua.wwind.table.interaction

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import ua.wwind.table.ColumnSpec
import ua.wwind.table.canStartEditAt
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.state.TableState

@Suppress("LongParameterList")
internal fun <T : Any, C> Modifier.tableKeyboardNavigation(
    focusRequester: FocusRequester,
    itemsCount: Int,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    verticalState: LazyListState,
    onRowClick: ((T) -> Unit)?,
    onExitToHeader: (column: C?) -> Unit,
    onOpenColumnMenu: (column: C?) -> Unit,
): Modifier =
    this
        .focusRequester(focusRequester)
        .onFocusChanged { state.isFocused = it.isFocused }
        .focusTarget()
        .onPreviewKeyEvent { event ->
            when {
                event.type != KeyEventType.KeyDown -> {
                    false
                }

                // While editing, the edit field owns cursor movement — only end the edit here.
                state.editing.rowIndex != null -> {
                    handleEditingKey(event, state, visibleColumns) {
                        commitAndMoveDown(state, itemsCount, visibleColumns, focusRequester)
                    }
                }

                event.isColumnMenuKey() -> {
                    onOpenColumnMenu(state.selection.selectedCell?.column ?: visibleColumns.firstOrNull()?.key)
                    true
                }

                event.exitsToHeader(state) -> {
                    onExitToHeader(state.selection.selectedCell?.column)
                    true
                }

                else -> {
                    handleCellActionKey(event, itemAt, state, visibleColumns, onRowClick) ||
                        handleNavigationKey(event, itemsCount, state, visibleColumns, verticalState)
                }
            }
        }.onKeyEvent { event -> state.handleBubbledBodyKey(event, itemsCount, visibleColumns, focusRequester) }

/**
 * Keys the body's content left unhandled, delivered either to the table's own focus target or, from
 * an edit cell, through [LocalBubbledBodyKeyHandler] — the cell's clickable would take them otherwise.
 */
internal val LocalBubbledBodyKeyHandler: ProvidableCompositionLocal<(KeyEvent) -> Boolean> =
    compositionLocalOf { { false } }

/**
 * Handles a key the editor did not use. Plain Enter belongs to the editor first — a line break, a
 * picked option, an IME action — and only when the editor leaves it does it commit the row.
 */
internal fun <C> TableState<C>.handleBubbledBodyKey(
    event: KeyEvent,
    itemsCount: Int,
    visibleColumns: List<ColumnSpec<*, C, *>>,
    focusRequester: FocusRequester,
): Boolean =
    if (event.type == KeyEventType.KeyDown && event.isPlainEnter() && editing.rowIndex != null) {
        commitAndMoveDown(this, itemsCount, visibleColumns, focusRequester)
    } else {
        false
    }

/** A plain ↑ with no selection, or on the first row, leaves the body for the header. */
private fun KeyEvent.exitsToHeader(state: TableState<*>): Boolean {
    val cell = state.selection.selectedCell
    return key == Key.DirectionUp && !isCtrlPressed && !isMetaPressed && (cell == null || cell.rowIndex == 0)
}

private fun <C> handleEditingKey(
    event: KeyEvent,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<*, C, *>>,
    commitAndMoveDown: () -> Boolean,
): Boolean =
    when {
        event.key == Key.Escape -> {
            state.editing.cancel()
            true
        }

        event.key == Key.Tab && event.isShiftPressed -> {
            state.editing.moveToPreviousCell(visibleColumns)
            true
        }

        event.key == Key.Tab -> {
            state.editing.completeCurrentCell(visibleColumns)
            true
        }

        // Ctrl/Cmd+Enter commits from any editor, a multi-line one included.
        event.isEnter() && (event.isCtrlPressed || event.isMetaPressed) -> {
            commitAndMoveDown()
        }

        else -> {
            false
        }
    }

/**
 * Completes the row edit and moves the selection one row down, keeping the column. A refused
 * completion keeps the row in edit mode and the selection where it is.
 *
 * The edit field that held focus leaves the composition, so the table takes focus back for the
 * keys that follow.
 */
private fun <C> commitAndMoveDown(
    state: TableState<C>,
    itemsCount: Int,
    visibleColumns: List<ColumnSpec<*, C, *>>,
    focusRequester: FocusRequester,
): Boolean {
    val row = state.editing.rowIndex ?: return false
    val column = state.selection.selectedCell?.column ?: state.editing.column
    if (!state.editing.tryComplete()) return true
    val colKeys = visibleColumns.map { it.key }
    state.moveSelectionTo(row + 1, colKeys.indexOf(column).coerceAtLeast(0), itemsCount, colKeys)
    focusRequester.requestFocus()
    return true
}

/**
 * Keys that act on the selected cell rather than move it: Enter and F2 start editing it, Enter opens
 * a row that cannot be edited there, Space toggles the row's selection.
 */
private fun <T : Any, C> handleCellActionKey(
    event: KeyEvent,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    onRowClick: ((T) -> Unit)?,
): Boolean {
    if (event.hasModifiers()) return false
    val cell = state.selection.selectedCell ?: return false
    return when {
        event.isEnter() || event.key == Key.F2 -> activateCell(event, cell, itemAt, state, visibleColumns, onRowClick)
        event.key == Key.Spacebar -> state.toggleRowSelection(cell.rowIndex)
        else -> false
    }
}

/** Enter or F2 on [cell]: edit it when it can be edited, otherwise Enter opens the row. */
@Suppress("LongParameterList")
private fun <T : Any, C> activateCell(
    event: KeyEvent,
    cell: TableState.SelectedCell<C>,
    itemAt: (Int) -> T?,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    onRowClick: ((T) -> Unit)?,
): Boolean {
    // A row still loading has nothing to edit or open.
    val item = itemAt(cell.rowIndex) ?: return false
    val spec = visibleColumns.firstOrNull { it.key == cell.column }
    return when {
        spec != null && spec.canStartEditAt(item, cell.rowIndex, state.settings.editingEnabled) -> {
            state.editing.start(item, cell.rowIndex, cell.column)
        }

        event.key == Key.F2 || onRowClick == null -> {
            false
        }

        else -> {
            onRowClick(item)
            true
        }
    }
}

/** Space on a row: flips its checkmark in [SelectionMode.Multiple], its selection in [SelectionMode.Single]. */
private fun TableState<*>.toggleRowSelection(row: Int): Boolean =
    when (settings.selectionMode) {
        SelectionMode.None -> {
            false
        }

        SelectionMode.Single -> {
            selection.toggleRow(row)
            true
        }

        SelectionMode.Multiple -> {
            selection.toggleCheck(row)
            true
        }
    }

private fun KeyEvent.isEnter(): Boolean = key == Key.Enter || key == Key.NumPadEnter

private fun KeyEvent.isPlainEnter(): Boolean = isEnter() && !hasModifiers()

private fun KeyEvent.hasModifiers(): Boolean = isCtrlPressed || isMetaPressed || isAltPressed || isShiftPressed

private fun <T : Any, C> handleNavigationKey(
    event: KeyEvent,
    itemsCount: Int,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    verticalState: LazyListState,
): Boolean {
    val colKeys = visibleColumns.map { it.key }
    val cell = state.selection.selectedCell
    val (targetRow, targetColIndex) =
        navigationTarget(
            event = event,
            itemsCount = itemsCount,
            currentRow = cell?.rowIndex ?: 0,
            currentColIndex = cell?.let { colKeys.indexOf(it.column) }?.takeIf { it >= 0 } ?: 0,
            lastColIndex = colKeys.lastIndex,
            // Ctrl/Cmd turns a step into a jump to the far edge.
            jumpToEdge = event.isCtrlPressed || event.isMetaPressed,
            pagedRow = { forward -> state.pagedRow(verticalState, cell?.rowIndex ?: 0, forward) },
        ) ?: return false
    state.moveSelectionTo(targetRow, targetColIndex, itemsCount, colKeys)
    return true
}

/**
 * The (row, column index) the selection moves to, or null when [event] is not a navigation key.
 *
 * Both coordinates are unclamped — [moveSelectionTo] owns the bounds.
 */
@Suppress("LongParameterList")
private fun navigationTarget(
    event: KeyEvent,
    itemsCount: Int,
    currentRow: Int,
    currentColIndex: Int,
    lastColIndex: Int,
    jumpToEdge: Boolean,
    pagedRow: (forward: Boolean) -> Int,
): Pair<Int, Int>? =
    when (event.key) {
        Key.DirectionRight -> currentRow to currentColIndex + 1
        Key.DirectionLeft -> currentRow to currentColIndex - 1
        Key.DirectionDown -> (if (jumpToEdge) itemsCount - 1 else currentRow + 1) to currentColIndex
        Key.DirectionUp -> (if (jumpToEdge) 0 else currentRow - 1) to currentColIndex
        Key.PageDown -> pagedRow(true) to currentColIndex
        Key.PageUp -> pagedRow(false) to currentColIndex
        Key.MoveHome -> if (jumpToEdge) 0 to currentColIndex else currentRow to 0
        Key.MoveEnd -> if (jumpToEdge) itemsCount - 1 to currentColIndex else currentRow to lastColIndex
        else -> null
    }

/** Clamps [row]/[colIndex] into range and moves both the selected cell and the focused row there. */
private fun <C> TableState<C>.moveSelectionTo(
    row: Int,
    colIndex: Int,
    itemsCount: Int,
    colKeys: List<C>,
) {
    val targetRow = row.coerceIn(0, itemsCount.coerceAtLeast(1) - 1)
    val targetColIndex = colIndex.coerceIn(0, (colKeys.size - 1).coerceAtLeast(0))
    val targetColKey = colKeys.getOrNull(targetColIndex) ?: return
    selection.selectCell(targetRow, targetColKey)
    // If selection is enabled, keep selected row in sync with focused row
    selection.focusRow(targetRow)
}

/**
 * Row to land on after PageDown/PageUp.
 *
 * A page is measured in fully visible lazy items, and a lazy item is a *unit* (a single row or a
 * declared group of adjacent rows). So paging happens in units and resolves back to the target
 * unit's first row; without groups this reduces to `currentRow ± fullyVisibleUnits`.
 */
private fun <C> TableState<C>.pagedRow(
    verticalState: LazyListState,
    currentRow: Int,
    forward: Boolean,
): Int {
    val units = rowUnits
    if (units.unitCount <= 0) return currentRow
    val layoutInfo = verticalState.layoutInfo
    val viewportHeight = layoutInfo.viewportHeightPx()
    val fullyVisible =
        layoutInfo.visibleItemsInfo
            .count { item -> item.offset >= 0 && item.offset + item.size <= viewportHeight }
            .coerceAtLeast(1)
    val currentUnit = units.unitOf(currentRow)
    val targetUnit =
        if (forward) {
            (currentUnit + fullyVisible).coerceAtMost(units.unitCount - 1)
        } else {
            (currentUnit - fullyVisible).coerceAtLeast(0)
        }
    return units.rowsOf(targetUnit).first
}
