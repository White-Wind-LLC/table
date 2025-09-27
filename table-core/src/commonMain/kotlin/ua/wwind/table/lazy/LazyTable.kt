package ua.wwind.table.lazy

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ua.wwind.table.ColumnSpec
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * High-performance 2D grid that renders only visible cells using SubcomposeLayout and a custom 2D viewport.
 *
 * MVP supports fixed-size rows/columns. No LazyColumn/LazyRow used.
 */
@Composable
public fun <T : Any, C> LazyTable(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    columns: List<ColumnSpec<T, C>>,
    rowHeight: Dp,
    defaultColumnWidth: Dp = 100.dp,
    modifier: Modifier = Modifier,
    state: ViewportState = rememberViewportState(),
    stickyHeader: Boolean = true,
    stickyFirstColumn: Boolean = false,
    onCellClick: ((row: Int, col: Int, item: T?) -> Unit)? = null,
    onCellDoubleClick: ((row: Int, col: Int, item: T?) -> Unit)? = null,
    onCellLongPress: ((row: Int, col: Int, item: T?) -> Unit)? = null,
) {
    val decay = remember { exponentialDecay<Float>() }
    val coroutineScope = rememberCoroutineScope()

    // Track layout size to update viewport
    var measuredViewport by remember { mutableStateOf(IntSize.Zero) }

    // SubcomposeLayout provides precise control over composing and disposing cells
    SubcomposeLayout(
        modifier =
            modifier
                .pointerInput(state, itemsCount, columns) {
                    // Drag to pan with inertial fling
                    var velocityTracker: VelocityTracker? = null
                    detectDragGestures(
                        onDragStart = {
                            velocityTracker = VelocityTracker()
                        },
                        onDrag = { change, dragAmount ->
                            velocityTracker?.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            // Invert drag to natural panning: dragging right moves content left (increase offsetX)
                            state.scrollBy(-dragAmount.x, -dragAmount.y)
                        },
                        onDragCancel = { velocityTracker = null },
                        onDragEnd = {
                            val tracker = velocityTracker
                            velocityTracker = null
                            if (tracker == null) return@detectDragGestures
                            val v = tracker.calculateVelocity()
                            launchFling(
                                initialVelocityX = -v.x,
                                initialVelocityY = -v.y,
                                decay = decay,
                                state = state,
                                scope = coroutineScope,
                            )
                        },
                    )
                }
                .pointerInput(state) {
                    // Wheel/trackpad panning; independent from drag block above
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val scroll = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                                if (scroll != Offset.Zero) {
                                    // Typical wheel: positive y means scroll up (content moves down), we invert to natural
                                    state.scrollBy(scroll.x, scroll.y)
                                }
                            }
                        }
                    }
                },
        measurePolicy = { constraints: Constraints ->
            val viewportWidth = constraints.maxWidth.coerceAtLeast(0)
            val viewportHeight = constraints.maxHeight.coerceAtLeast(0)
            measuredViewport = IntSize(viewportWidth, viewportHeight)

            // Convert to px using current density from scope
            val rowHeightPx = rowHeight.roundToPx()
            val colWidthsPx = IntArray(columns.size) { i ->
                val spec = columns[i]
                val w = (spec.width ?: defaultColumnWidth)
                val minW = spec.minWidth
                val px = w.roundToPx().coerceAtLeast(minW.roundToPx())
                px
            }
            val colsCum = IntArray(columns.size + 1)
            for (i in colWidthsPx.indices) colsCum[i + 1] = colsCum[i] + colWidthsPx[i]

            val contentWidth = colsCum.last()
            val contentHeight = itemsCount * rowHeightPx
            state.updateSizes(measuredViewport, contentWidth, contentHeight)

            if (itemsCount <= 0 || columns.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) {
                return@SubcomposeLayout layout(viewportWidth, viewportHeight) {}
            }

            val range = computeVisibleRangeVariable(
                offsetX = state.offsetX,
                offsetY = state.offsetY,
                viewport = measuredViewport,
                overscanPx = state.overscanPx,
                totalRows = itemsCount,
                rowHeightPx = rowHeightPx,
                colsCum = colsCum,
            )

            // Compose visible cells
            val placeables = ArrayList<CellPlaceable>(
                max(0, (range.lastRow - range.firstRow + 1) * (range.lastCol - range.firstCol + 1)),
            )

            fun cellModifier(r: Int, c: Int, item: T?): Modifier {
                var m: Modifier = Modifier
                if (onCellClick != null || onCellDoubleClick != null || onCellLongPress != null) {
                    m = m.pointerInput(onCellClick, onCellDoubleClick, onCellLongPress) {
                        detectTapGestures(
                            onLongPress = onCellLongPress?.let { handler -> { handler(r, c, item) } },
                            onDoubleTap = onCellDoubleClick?.let { handler -> { handler(r, c, item) } },
                            onTap = onCellClick?.let { handler -> { handler(r, c, item) } },
                        )
                    }
                }
                return m
            }

            for (row in range.firstRow..range.lastRow) {
                val item = itemAt(row)
                val y = row * rowHeightPx - state.offsetY.roundToInt()
                for (col in range.firstCol..range.lastCol) {
                    val x = colsCum[col] - state.offsetX.roundToInt()
                    val w = colWidthsPx[col]
                    val key = cellKey(row, col)
                    val placeable = subcompose(key) {
                        Box(modifier = cellModifier(row, col, item)) {
                            if (item != null) {
                                columns[col].cell(this, item)
                            }
                        }
                    }.first().measure(Constraints.fixed(w, rowHeightPx))
                    placeables += CellPlaceable(x = x, y = y, placeable = placeable)
                }
            }

            // Optional sticky headers
            val stickyPlaceables = mutableListOf<CellPlaceable>()
            if (stickyHeader) {
                val y = 0
                val firstCol = range.firstCol
                val lastCol = range.lastCol
                for (col in firstCol..lastCol) {
                    val x = colsCum[col] - state.offsetX.roundToInt()
                    val w = colWidthsPx[col]
                    val key = stickyHeaderKey(col)
                    val p = subcompose(key) {
                        Box { columns[col].header() }
                    }.first().measure(Constraints.fixed(w, rowHeightPx))
                    stickyPlaceables += CellPlaceable(x = x, y = y, placeable = p, zIndex = 1f)
                }
            }
            if (stickyFirstColumn && columns.isNotEmpty()) {
                val x = 0
                val col = 0
                val firstRow = range.firstRow
                val lastRow = range.lastRow
                val w = colWidthsPx[col]
                for (row in firstRow..lastRow) {
                    val item = itemAt(row)
                    val y = row * rowHeightPx - state.offsetY.roundToInt()
                    val key = stickyFirstColKey(row)
                    val p = subcompose(key) {
                        Box(modifier = cellModifier(row, col, item)) {
                            if (item != null) columns[col].cell(this, item)
                        }
                    }.first().measure(Constraints.fixed(w, rowHeightPx))
                    stickyPlaceables += CellPlaceable(x = x, y = y, placeable = p, zIndex = 1f)
                }
                if (stickyHeader) {
                    val key = stickyCornerKey()
                    val p = subcompose(key) {
                        Box { columns[col].header() }
                    }.first().measure(Constraints.fixed(w, rowHeightPx))
                    stickyPlaceables += CellPlaceable(x = 0, y = 0, placeable = p, zIndex = 2f)
                }
            }

            return@SubcomposeLayout layout(viewportWidth, viewportHeight) {
                for (cp in placeables) {
                    cp.placeable.placeRelative(cp.x, cp.y)
                }
                for (cp in stickyPlaceables) {
                    cp.placeable.placeRelative(cp.x, cp.y, cp.zIndex)
                }
            }
        },
    )
}

private data class CellPlaceable(
    val x: Int,
    val y: Int,
    val placeable: androidx.compose.ui.layout.Placeable,
    val zIndex: Float = 0f,
)

private fun cellKey(row: Int, col: Int): Any = "cell_" + row + "_" + col
private fun stickyHeaderKey(col: Int): Any = "sticky_header_" + col
private fun stickyFirstColKey(row: Int): Any = "sticky_first_col_" + row
private fun stickyCornerKey(): Any = "sticky_corner_0_0"

// Launch a decayed fling independently on X and Y axes.
private fun launchFling(
    initialVelocityX: Float,
    initialVelocityY: Float,
    decay: DecayAnimationSpec<Float>,
    state: ViewportState,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    scope.launch {
        coroutineScope {
            val jobX = launch { animateFlingAxis(initialVelocityX, decay) { dx -> state.scrollBy(dx, 0f) } }
            val jobY = launch { animateFlingAxis(initialVelocityY, decay) { dy -> state.scrollBy(0f, dy) } }
            jobX.join(); jobY.join()
        }
    }
}

private suspend fun animateFlingAxis(
    initialVelocity: Float,
    decay: DecayAnimationSpec<Float>,
    applyDelta: (Float) -> Unit,
) {
    var lastValue = 0f
    val anim = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
    anim.animateDecay(decay) {
        val delta = value - lastValue
        lastValue = value
        if (delta != 0f) applyDelta(delta)
    }
}
