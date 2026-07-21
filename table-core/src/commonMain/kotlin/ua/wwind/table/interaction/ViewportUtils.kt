package ua.wwind.table.interaction

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

/**
 * Ensure that the given [index] row becomes fully visible in the viewport (supports dynamic row heights).
 *
 * [index] is a row index; the backing `LazyColumn` is indexed by *units* (a unit is a single row or a
 * declared group of adjacent rows), so it is translated once via [TableState.rowUnits] and the unit
 * index is used for every `layoutInfo` lookup and scroll call. Height estimates stay keyed by row
 * (`state.rowHeightsPx`): they under-count a group's internal gap, which is acceptable because every
 * branch fine-tunes the final scroll from the measured `info.offset` once the target unit is visible.
 */
public suspend fun <C> ensureRowFullyVisible(
    index: Int,
    verticalState: LazyListState,
    state: TableState<C>,
    density: Density,
    movement: Int = 0,
) {
    val unitIndex = state.rowUnits.unitOf(index)
    val visible = verticalState.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) {
        verticalState.animateScrollToItem(unitIndex)
        return
    }

    val targetInfo = visible.firstOrNull { it.index == unitIndex }
    val firstVisibleIndex = visible.first().index
    val lastVisibleIndex = visible.last().index
    when {
        targetInfo != null -> {
            verticalState.nudgeVisibleUnitIntoView(targetInfo)
        }

        // Moving upward onto the unit immediately above: scroll just enough to reveal it at the top.
        unitIndex < firstVisibleIndex -> {
            if (movement < 0 && unitIndex == firstVisibleIndex - 1) {
                verticalState.revealUnitAbove(index, unitIndex, state, density)
            } else {
                verticalState.animateScrollToItem(unitIndex) // Fallback: align to top
            }
        }

        // Moving downward onto the unit right after the last visible one: reveal just that unit.
        unitIndex > lastVisibleIndex -> {
            if (movement > 0 && unitIndex == lastVisibleIndex + 1) {
                verticalState.revealUnitBelow(index, unitIndex, state, density)
            } else {
                verticalState.jumpDownToUnit(index, unitIndex, state, density)
            }
        }
    }
}

/** Pulls an already visible [info] fully inside the viewport, correcting a clipped top or bottom. */
private suspend fun LazyListState.nudgeVisibleUnitIntoView(info: LazyListItemInfo) {
    val viewportHeight = layoutInfo.viewportHeightPx()
    val top = info.offset
    val bottom = info.offset + info.size
    val epsilon = 1 // px to avoid off-by-one clipping
    when {
        top < 0 -> animateScrollBy(top.toFloat())
        bottom > viewportHeight - epsilon -> animateScrollBy((bottom - viewportHeight).toFloat())
    }
}

/** Scrolls up by one estimated row height so that [unitIndex] lands at the top of the viewport. */
private suspend fun <C> LazyListState.revealUnitAbove(
    index: Int,
    unitIndex: Int,
    state: TableState<C>,
    density: Density,
) {
    val firstTop = layoutInfo.visibleItemsInfo.first().offset
    val delta = firstTop - state.estimatedRowHeight(index, density)
    if (delta != 0) animateScrollBy(delta.toFloat())
    // Fine-tune once it becomes visible
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == unitIndex }
    if (info != null && info.offset < 0) animateScrollBy(info.offset.toFloat())
}

/** Scrolls down by one estimated row height so that [unitIndex] lands at the bottom of the viewport. */
private suspend fun <C> LazyListState.revealUnitBelow(
    index: Int,
    unitIndex: Int,
    state: TableState<C>,
    density: Density,
) {
    val viewportHeight = layoutInfo.viewportHeightPx()
    val last = layoutInfo.visibleItemsInfo.last()
    val lastBottom = last.offset + last.size
    val desiredTop = (viewportHeight - state.estimatedRowHeight(index, density)).coerceAtLeast(0)
    val delta = (lastBottom - desiredTop).coerceAtLeast(0)
    if (delta != 0) animateScrollBy(delta.toFloat())
    // Fine-tune now that it's visible
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == unitIndex }
    val overflow = info?.let { it.offset + it.size - viewportHeight } ?: 0
    if (overflow > 0) animateScrollBy(overflow.toFloat())
}

/**
 * Fallback for multi-row downward jumps (e.g. PageDown): computes the exact delta from the
 * measured/estimated heights of every row between the last visible one and [index].
 */
private suspend fun <C> LazyListState.jumpDownToUnit(
    index: Int,
    unitIndex: Int,
    state: TableState<C>,
    density: Density,
) {
    val viewportHeight = layoutInfo.viewportHeightPx()
    val last = layoutInfo.visibleItemsInfo.last()

    // Sum heights between the last visible row and the target (excluding the target).
    // `last.index` is a unit index, so translate it back to the last row it renders before
    // walking rows; with no groups this is exactly `last.index + 1`.
    var betweenSum = 0
    var i = state.rowUnits.rowsOf(last.index).last + 1
    while (i < index) {
        betweenSum += state.estimatedRowHeight(i, density)
        i++
    }

    val lastBottom = last.offset + last.size
    val desiredTop = (viewportHeight - state.estimatedRowHeight(index, density)).coerceAtLeast(0)
    val delta = (lastBottom + betweenSum) - desiredTop
    if (delta != 0) animateScrollBy(delta.toFloat())

    // Fine-tune once it becomes visible (avoid off-by-one and dynamic height adjustments)
    val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == unitIndex } ?: return
    val overflow = info.offset + info.size - viewportHeight
    when {
        overflow > 0 -> animateScrollBy(overflow.toFloat())
        info.offset < 0 -> animateScrollBy(info.offset.toFloat())
    }
}

/** Measured height of row [index], falling back to the configured default when it is not laid out yet. */
private fun <C> TableState<C>.estimatedRowHeight(
    index: Int,
    density: Density,
): Int = rowHeightsPx[index] ?: with(density) { dimensions.rowHeight.toPx() }.toInt()

internal fun LazyListLayoutInfo.viewportHeightPx(): Int = (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)

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
        val width = state.resolveColumnWidth(spec.key, spec)
        x += width + dimensions.dividerThickness
    }

    val columnWidth = state.resolveColumnWidth(targetColKey, visibleColumns[targetColIndex])

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
