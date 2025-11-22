package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
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
                state.rowHeightsPx[index] ?: with(density) { state.dimensions.rowHeight.toPx() }.toInt()
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
                state.rowHeightsPx[index] ?: with(density) { state.dimensions.rowHeight.toPx() }.toInt()
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
            // Fallback for multi-row downward jumps (e.g., PageDown):
            // compute exact delta using measured/estimated heights
            val defaultHeight = with(density) { state.dimensions.rowHeight.toPx() }.toInt()

            fun heightOf(i: Int): Int = state.rowHeightsPx[i] ?: defaultHeight

            // Sum heights between the last visible row and the target (excluding the target)
            var betweenSum = 0
            var i = lastVisibleIndex + 1
            while (i < index) {
                betweenSum += heightOf(i)
                i++
            }
            val targetHeight = heightOf(index)

            val last = visible.last()
            val lastBottom = last.offset + last.size
            val desiredTop = (viewportHeight - targetHeight).coerceAtLeast(0)
            val delta = (lastBottom + betweenSum) - desiredTop
            if (delta != 0) verticalState.animateScrollBy(delta.toFloat())

            // Fine-tune once it becomes visible (avoid off-by-one and dynamic height adjustments)
            val info = verticalState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (info != null) {
                val bottom = info.offset + info.size
                val overflow = bottom - viewportHeight
                if (overflow > 0) {
                    verticalState.animateScrollBy(overflow.toFloat())
                } else if (info.offset < 0) {
                    verticalState.animateScrollBy(info.offset.toFloat())
                }
            }
        }
        return
    }
}

/** Ensure that the given column becomes fully visible horizontally. */
public suspend fun <T : Any, C, E> ensureColumnFullyVisible(
    targetColIndex: Int,
    targetColKey: C,
    visibleColumns: List<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    horizontalState: ScrollState,
    density: Density,
) {
    val dimensions = state.dimensions

    // Compute absolute left position of the target column within the content (px)
    var x = 0.dp
    visibleColumns.take(targetColIndex).forEach { spec ->
        val width = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
        x += width + dimensions.dividerThickness
    }

    val columnWidth =
        state.columnWidths[targetColKey]
            ?: visibleColumns[targetColIndex].width
            ?: dimensions.defaultColumnWidth

    // 1) Viewport coordinates relative to full content width
    val viewportX1Px = horizontalState.value
    val viewportX2Px = viewportX1Px + horizontalState.viewportSize

    // 2) Selected cell coordinates
    val cellX1Px = with(density) { x.toPx() }.toInt()
    val cellX2Px = cellX1Px + with(density) { columnWidth.toPx() }.toInt()

    // 3) Decide scroll delta (px) using when
    val deltaPx =
        when {
            // a) Cell's left is left of viewport -> scroll left by delta
            cellX1Px < viewportX1Px -> cellX1Px - viewportX1Px
            // b) Cell's right is right of viewport -> scroll right by delta
            cellX2Px > viewportX2Px -> cellX2Px - viewportX2Px
            // c) Otherwise do nothing
            else -> 0
        }

    if (deltaPx != 0) {
        horizontalState.animateScrollBy(deltaPx.toFloat())
    }
}

/** Ensure that the specified cell (row + column) becomes fully visible. */
public suspend fun <T : Any, C, E> ensureCellFullyVisible(
    rowIndex: Int,
    targetColIndex: Int,
    targetColKey: C,
    visibleColumns: List<ColumnSpec<T, C, E>>,
    state: TableState<C>,
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
        horizontalState = horizontalState,
        density = density,
    )
}
