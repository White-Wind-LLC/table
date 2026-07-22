package ua.wwind.table.interaction

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

internal fun <T : Any, C> Modifier.tableKeyboardNavigation(
    focusRequester: FocusRequester,
    itemsCount: Int,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    verticalState: LazyListState,
): Modifier =
    this
        .focusRequester(focusRequester)
        .focusTarget()
        .onPreviewKeyEvent { event ->
            when {
                event.type != KeyEventType.KeyDown -> false

                // While editing, the edit field owns cursor movement — only end the edit here.
                state.editingRow != null -> handleEditingKey(event, state, visibleColumns)

                else -> handleNavigationKey(event, itemsCount, state, visibleColumns, verticalState)
            }
        }

private fun <C> handleEditingKey(
    event: KeyEvent,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<*, C, *>>,
): Boolean =
    when (event.key) {
        Key.Escape -> {
            state.cancelEditing()
            true
        }

        Key.Tab -> {
            state.completeCurrentCellEdit(visibleColumns)
            true
        }

        else -> {
            false
        }
    }

private fun <T : Any, C> handleNavigationKey(
    event: KeyEvent,
    itemsCount: Int,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C, *>>,
    verticalState: LazyListState,
): Boolean {
    val colKeys = visibleColumns.map { it.key }
    val cell = state.selectedCell
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
    selectCell(targetRow, targetColKey)
    // If selection is enabled, keep selected row in sync with focused row
    focusRow(targetRow)
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
