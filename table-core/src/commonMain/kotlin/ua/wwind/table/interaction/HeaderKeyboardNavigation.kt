package ua.wwind.table.interaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.isInteractionLockByRowReorderEnabled
import ua.wwind.table.state.TableState

/** Shift+F10 or the Menu key: the platform shortcuts for a context menu. */
internal fun KeyEvent.isColumnMenuKey(): Boolean = (key == Key.F10 && isShiftPressed) || key == Key.Menu

/**
 * The header row as one Tab stop. ←/→/Home/End move between columns, Enter/Space sorts,
 * Shift+F10 / Menu / Alt+↓ open the column menu, and ↓ moves focus into the body.
 */
internal fun <C> Modifier.tableHeaderKeyboardNavigation(
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<*, C, *>>,
    onEnterBody: () -> Unit,
): Modifier =
    this
        .focusRequester(state.headerFocusRequester)
        .onFocusChanged { focus ->
            state.isHeaderFocused = focus.isFocused
            // The next arrival is a keyboard one unless a pointer says otherwise when it focuses the header.
            if (!focus.isFocused) state.isHeaderFocusFromKeyboard = true
            if (focus.isFocused) {
                val keys = visibleColumns.map { it.key }
                if (state.focusedHeaderColumn !in keys) {
                    state.focusedHeaderColumn =
                        state.selection.selectedCell
                            ?.column
                            ?.takeIf { it in keys } ?: keys.firstOrNull()
                }
            }
        }.focusTarget()
        .onPreviewKeyEvent { event ->
            val handled =
                event.type == KeyEventType.KeyDown && handleHeaderKey(event, state, visibleColumns, onEnterBody)
            if (handled) state.isHeaderFocusFromKeyboard = true
            handled
        }

/** ↑ from the body: focus the header on [column] (or the header's current column when null). */
internal fun <C> TableState<C>.focusHeaderFromBody(column: C?) {
    if (column != null) focusedHeaderColumn = column
    headerFocusRequester.requestFocus()
}

/**
 * Shift+F10 / Menu in the body: focus the header on [column] and open its menu. A [column] that is
 * no longer visible (e.g. the selected cell's column was hidden) falls back to the first visible one.
 */
internal fun <C> TableState<C>.openColumnMenuFromBody(column: C?) {
    val visible = columns.visibleKeys()
    val target = column?.takeIf { it in visible } ?: visible.firstOrNull() ?: return
    headerFocusRequester.requestFocus()
    openColumnMenuFromKeyboard(target)
}

/**
 * ↓ from the header: focus the body. With a cell already selected, the selection moves to row 0 of
 * the focused header column; otherwise only focus moves.
 */
internal fun <C> TableState<C>.enterBodyFromHeader(
    itemsCount: Int,
    bodyFocusRequester: FocusRequester,
) {
    val column = focusedHeaderColumn
    if (column != null && itemsCount > 0 && selection.selectedCell != null) {
        selection.selectCell(0, column)
        selection.focusRow(0)
    }
    bodyFocusRequester.requestFocus()
}

private fun <C> handleHeaderKey(
    event: KeyEvent,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<*, C, *>>,
    onEnterBody: () -> Unit,
): Boolean {
    val keys = visibleColumns.map { it.key }
    val current = state.focusedHeaderColumn?.takeIf { it in keys } ?: keys.firstOrNull() ?: return false
    val index = keys.indexOf(current)
    return when {
        event.opensHeaderMenu() -> {
            state.openColumnMenuFromKeyboard(current)
            true
        }

        event.key == Key.DirectionDown -> {
            onEnterBody()
            true
        }

        event.isSortKey() -> {
            val spec = visibleColumns[index]
            if (spec.sortable && !state.settings.isInteractionLockByRowReorderEnabled) state.setSort(current)
            true
        }

        else -> {
            val target = headerTarget(event.key, index, keys.lastIndex) ?: return false
            state.focusedHeaderColumn = keys[target]
            true
        }
    }
}

/** Shift+F10 / Menu, or Alt+↓ as in a combo box: open the focused column's menu. */
private fun KeyEvent.opensHeaderMenu(): Boolean = isColumnMenuKey() || (key == Key.DirectionDown && isAltPressed)

/** Enter or Space: toggle the focused column's sort. */
private fun KeyEvent.isSortKey(): Boolean = key == Key.Enter || key == Key.NumPadEnter || key == Key.Spacebar

private fun headerTarget(
    key: Key,
    index: Int,
    lastIndex: Int,
): Int? =
    when (key) {
        Key.DirectionLeft -> (index - 1).coerceAtLeast(0)
        Key.DirectionRight -> (index + 1).coerceAtMost(lastIndex)
        Key.MoveHome -> 0
        Key.MoveEnd -> lastIndex
        else -> null
    }
