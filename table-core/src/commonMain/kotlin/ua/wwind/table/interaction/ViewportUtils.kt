package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

/** Ensure that the given [index] row becomes fully visible in the viewport (supports dynamic row heights). */
public suspend fun <C> ensureRowFullyVisible(
    index: Int,
    verticalState: LazyListState,
    state: TableState<C>,
    density: Density,
    movement: Int = 0,
) {
    val layout = verticalState.layoutInfo
    val visible = layout.visibleItemsInfo
    if (visible.isEmpty()) {
        verticalState.animateScrollToItem(index)
        return
    }

    val firstVisibleIndex = visible.first().index
    val lastVisibleIndex = visible.last().index
    val targetInfo = visible.firstOrNull { it.index == index }
    val viewportHeight = (layout.viewportEndOffset - layout.viewportStartOffset).coerceAtLeast(0)

    if (targetInfo != null) {
        val top = targetInfo.offset
        val bottom = targetInfo.offset + targetInfo.size
        val epsilon = 1 // px to avoid off-by-one clipping
        when {
            top < 0 -> verticalState.animateScrollBy(top.toFloat())
            bottom > viewportHeight - epsilon -> verticalState.animateScrollBy((bottom - viewportHeight).toFloat())
        }
        return
    }

    if (index < firstVisibleIndex) {
        // If moving upward and the target is immediately above, scroll just enough to reveal it at the top.
        val prevIndex = firstVisibleIndex - 1
        if (movement < 0 && index == prevIndex) {
            val estimatedHeight =
                state.rowHeightsPx[index] ?: with(density) { state.dimensions.defaultRowHeight.toPx() }.toInt()
            val firstTop = visible.first().offset
            val delta = (firstTop - estimatedHeight)
            if (delta != 0) verticalState.animateScrollBy(delta.toFloat())
            // Fine-tune once it becomes visible
            val info = verticalState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (info != null && info.offset < 0) verticalState.animateScrollBy(info.offset.toFloat())
        } else {
            // Fallback: align to top
            verticalState.animateScrollToItem(index)
        }
        return
    }

    if (index > lastVisibleIndex) {
        // If moving downward and the target is the next row after the last visible, reveal just one row.
        val nextIndex = lastVisibleIndex + 1
        if (movement > 0 && index == nextIndex) {
            val estimatedHeight =
                state.rowHeightsPx[index] ?: with(density) { state.dimensions.defaultRowHeight.toPx() }.toInt()
            val last = visible.last()
            val lastBottom = last.offset + last.size
            val desiredTop = (viewportHeight - estimatedHeight).coerceAtLeast(0)
            val delta = (lastBottom - desiredTop).coerceAtLeast(0)
            if (delta != 0) verticalState.animateScrollBy(delta.toFloat())
            // Fine-tune now that it's visible
            val info = verticalState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (info != null) {
                val bottom = info.offset + info.size
                val overflow = bottom - viewportHeight
                if (overflow > 0) verticalState.animateScrollBy(overflow.toFloat())
            }
        } else {
            // Fallback: bring into view, then bottom-align precisely
            verticalState.animateScrollToItem(index)
            val layoutAfter = verticalState.layoutInfo
            val viewportHeightAfter = (layoutAfter.viewportEndOffset - layoutAfter.viewportStartOffset).coerceAtLeast(0)
            val targetInfoAfter = layoutAfter.visibleItemsInfo.firstOrNull { it.index == index }
            if (targetInfoAfter != null) {
                val desiredTop2 = (viewportHeightAfter - targetInfoAfter.size).coerceAtLeast(0)
                val delta2 = (desiredTop2 - targetInfoAfter.offset)
                if (delta2 != 0) verticalState.animateScrollBy(delta2.toFloat())
            }
        }
        return
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
    movement: Int = 0,
) {
    ensureRowFullyVisible(
        index = rowIndex,
        verticalState = verticalState,
        state = state,
        density = density,
        movement = movement,
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
