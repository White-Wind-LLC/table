package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.state.TableState

@ExperimentalTableApi
@Suppress("LongParameterList")
public fun <T : Any, C> Modifier.tableKeyboardNavigation(
    focusRequester: FocusRequester,
    itemsCount: Int,
    state: TableState<C>,
    visibleColumns: List<ColumnSpec<T, C>>,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    hasLeading: Boolean,
    tableWidth: Dp,
    density: Density,
    coroutineScope: CoroutineScope,
): Modifier {
    return this
        .focusRequester(focusRequester)
        .focusTarget()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

            val cell = state.selectedCell
            val colKeys = visibleColumns.map { it.key }
            val currentRow = cell?.rowIndex ?: 0
            val currentColIndex = cell?.let { colKeys.indexOf(it.column) }?.takeIf { it >= 0 } ?: 0

            fun clampRow(r: Int) = r.coerceIn(0, itemsCount.coerceAtLeast(1) - 1)
            fun clampCol(c: Int) = c.coerceIn(0, (colKeys.size - 1).coerceAtLeast(0))

            fun ensureFocus(row: Int, colIndex: Int) {
                val targetRow = clampRow(row)
                val targetColIndex = clampCol(colIndex)
                val targetColKey = colKeys.getOrNull(targetColIndex) ?: return
                state.selectCell(targetRow, targetColKey)
                // If selection is enabled, keep selected row in sync with focused row
                state.focusRow(targetRow)
            }

            val jumpToEdge = event.isCtrlPressed || event.isMetaPressed

            when (event.key) {
                Key.DirectionRight -> {
                    ensureFocus(currentRow, currentColIndex + 1)
                    true
                }

                Key.DirectionLeft -> {
                    ensureFocus(currentRow, currentColIndex - 1)
                    true
                }

                Key.DirectionDown -> {
                    if (jumpToEdge) ensureFocus(itemsCount - 1, currentColIndex)
                    else ensureFocus(currentRow + 1, currentColIndex)
                    true
                }

                Key.DirectionUp -> {
                    if (jumpToEdge) ensureFocus(0, currentColIndex)
                    else ensureFocus(currentRow - 1, currentColIndex)
                    true
                }

                Key.PageDown -> {
                    val layoutInfo = verticalState.layoutInfo
                    val viewportHeight =
                        (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
                    val fullyVisible = layoutInfo.visibleItemsInfo.count { item ->
                        val top = item.offset
                        val bottom = item.offset + item.size
                        top >= 0 && bottom <= viewportHeight
                    }.coerceAtLeast(1)
                    val target = currentRow + fullyVisible
                    ensureFocus(target, currentColIndex)
                    true
                }

                Key.PageUp -> {
                    val layoutInfo = verticalState.layoutInfo
                    val viewportHeight =
                        (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
                    val fullyVisible = layoutInfo.visibleItemsInfo.count { item ->
                        val top = item.offset
                        val bottom = item.offset + item.size
                        top >= 0 && bottom <= viewportHeight
                    }.coerceAtLeast(1)
                    val target = currentRow - fullyVisible
                    ensureFocus(target, currentColIndex)
                    true
                }

                Key.MoveHome -> {
                    if (jumpToEdge) ensureFocus(0, currentColIndex)
                    else ensureFocus(currentRow, 0)
                    true
                }

                Key.MoveEnd -> {
                    if (jumpToEdge) ensureFocus(itemsCount - 1, currentColIndex)
                    else ensureFocus(currentRow, colKeys.lastIndex)
                    true
                }

                else -> false
            }
        }
}
