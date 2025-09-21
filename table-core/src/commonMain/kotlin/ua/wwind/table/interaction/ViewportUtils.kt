package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

/** Ensure that the given [index] row becomes fully visible in the viewport. */
public suspend fun ensureRowFullyVisible(
    index: Int,
    verticalState: LazyListState,
    rowHeight: Dp,
    density: Density,
) {
    val layout = verticalState.layoutInfo
    val visible = layout.visibleItemsInfo
    val firstVisibleIndex = visible.firstOrNull()?.index
    val lastVisibleIndex = visible.lastOrNull()?.index
    if (firstVisibleIndex == null || lastVisibleIndex == null) {
        verticalState.animateScrollToItem(index)
        return
    }

    val targetInfo = visible.firstOrNull { it.index == index }
    val viewportHeight = (layout.viewportEndOffset - layout.viewportStartOffset).coerceAtLeast(0)
    val rowHeightPx = with(density) { rowHeight.toPx() }.toInt().coerceAtLeast(1)

    val scrollToBottomAlign: suspend () -> Unit = {
        val fullyVisibleCount = (viewportHeight / rowHeightPx).coerceAtLeast(1)
        val desiredFirstIndex = (index - (fullyVisibleCount - 1)).coerceAtLeast(0)
        val maxFirstIndex = (layout.totalItemsCount - fullyVisibleCount).coerceAtLeast(0)
        val clampedFirstIndex = desiredFirstIndex.coerceAtMost(maxFirstIndex)
        verticalState.animateScrollToItem(clampedFirstIndex)
    }

    when {
        index < firstVisibleIndex -> {
            verticalState.animateScrollToItem(index)
        }

        index > lastVisibleIndex -> {
            scrollToBottomAlign()
        }

        targetInfo != null -> {
            val top = targetInfo.offset
            val bottom = targetInfo.offset + targetInfo.size
            val epsilon = 1 // px to avoid off-by-one clipping
            if (top < 0) {
                verticalState.animateScrollToItem(index)
            } else if (bottom > viewportHeight - epsilon) {
                scrollToBottomAlign()
            }
        }

        else -> {
            verticalState.animateScrollToItem(index)
        }
    }
}

/** Ensure that the given column becomes fully visible horizontally. */
public suspend fun <T : Any, C> ensureColumnFullyVisible(
    targetColIndex: Int,
    targetColKey: C,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    hasLeading: Boolean,
    tableWidth: Dp,
    horizontalState: ScrollState,
    density: Density,
) {
    val dimensions = state.dimensions
    var x = 0.dp
    if (hasLeading) {
        x += dimensions.defaultRowHeight + dimensions.verticalDividerThickness
    }
    visibleColumns.take(targetColIndex).forEach { spec ->
        val w = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
        x += w + dimensions.verticalDividerThickness
    }

    val colWidth =
        state.columnWidths[targetColKey]
            ?: visibleColumns[targetColIndex].width
            ?: dimensions.defaultColumnWidth

    val contentWidthPx = with(density) { tableWidth.toPx() }.toInt()
    val viewportWidthPx = (contentWidthPx - horizontalState.maxValue).coerceAtLeast(0)
    val columnLeftPx = with(density) { x.toPx() }.toInt()
    val columnRightPx = columnLeftPx + with(density) { colWidth.toPx() }.toInt()
    val viewportLeft = horizontalState.value
    val viewportRight = viewportLeft + viewportWidthPx

    val targetScrollPx =
        if (columnLeftPx >= viewportLeft && columnRightPx <= viewportRight) {
            null // fully visible, no scroll
        } else if (columnRightPx > viewportRight) {
            // Move so that column becomes the rightmost fully visible
            (columnRightPx - viewportWidthPx).coerceIn(0, horizontalState.maxValue)
        } else {
            // Move so that column becomes the first visible on the left
            columnLeftPx.coerceIn(0, horizontalState.maxValue)
        }

    if (targetScrollPx != null && targetScrollPx != horizontalState.value) {
        horizontalState.animateScrollTo(targetScrollPx)
    }
}

/** Ensure that the specified cell (row + column) becomes fully visible. */
public suspend fun <T : Any, C> ensureCellFullyVisible(
    rowIndex: Int,
    targetColIndex: Int,
    targetColKey: C,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    hasLeading: Boolean,
    tableWidth: Dp,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    density: Density,
) {
    ensureRowFullyVisible(
        index = rowIndex,
        verticalState = verticalState,
        rowHeight = state.dimensions.defaultRowHeight,
        density = density,
    )
    ensureColumnFullyVisible(
        targetColIndex = targetColIndex,
        targetColKey = targetColKey,
        visibleColumns = visibleColumns,
        state = state,
        hasLeading = hasLeading,
        tableWidth = tableWidth,
        horizontalState = horizontalState,
        density = density,
    )
}
