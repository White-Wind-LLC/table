package ua.wwind.table.component.header

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.state.currentTableState
import ua.wwind.table.state.dividerWidthAfterColumn

private const val OVERLAY_REACH_DP = 3

/** The last strip has no room to hang over the boundary, so it takes that width back on the left. */
private const val LAST_OVERLAY_REACH_DP = 6

private const val EDGE_ZONE_DP = 24
private const val EDGE_GROWTH_DP_PER_SECOND = 200f

private const val NANOS_PER_SECOND = 1_000_000_000f

/** Frames the released gesture is given for layout to catch up before the scroll stops chasing it. */
private const val SETTLE_FRAMES = 8

@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> ColumnResizersOverlay(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    dimensions: TableDimensions,
    horizontalState: ScrollState,
    onResize: (key: C, newWidth: Dp) -> Unit,
    onResizeStart: () -> Unit = {},
    onResizeEnd: () -> Unit = {},
    onDoubleClick: (key: C) -> Unit = {},
) {
    val state = currentTableState()
    Box(modifier = Modifier.width(state.tableWidth)) {
        var cumulativeX: Dp = 0.dp

        visibleColumns.forEachIndexed { index, spec ->
            cumulativeX += widthResolver(spec.key)

            if (spec.resizable) {
                val isLast = index == visibleColumns.size - 1
                ResizeHandle(
                    columnKey = spec.key,
                    boundaryX = cumulativeX,
                    minWidth = spec.minWidth,
                    reach = if (isLast) LAST_OVERLAY_REACH_DP.dp else OVERLAY_REACH_DP.dp,
                    overhang = if (isLast) 0.dp else OVERLAY_REACH_DP.dp,
                    dividerThickness = dimensions.dividerThickness,
                    widthResolver = widthResolver,
                    horizontalState = horizontalState,
                    onResize = onResize,
                    onResizeStart = onResizeStart,
                    onResizeEnd = onResizeEnd,
                    onDoubleClick = onDoubleClick,
                )
            }
            cumulativeX +=
                dividerWidthAfterColumn(index, visibleColumns.size, state.settings, dimensions)
        }
    }
}

/** Grab strip [reach] wide to the left of one column boundary; dragging resizes and scrolls after it. */
@Composable
@Suppress("LongParameterList")
private fun <C> ResizeHandle(
    columnKey: C,
    boundaryX: Dp,
    minWidth: Dp,
    reach: Dp,
    overhang: Dp,
    dividerThickness: Dp,
    widthResolver: (C) -> Dp,
    horizontalState: ScrollState,
    onResize: (key: C, newWidth: Dp) -> Unit,
    onResizeStart: () -> Unit,
    onResizeEnd: () -> Unit,
    onDoubleClick: (key: C) -> Unit,
) {
    val density = LocalDensity.current
    val interaction = remember(columnKey) { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val drag = remember(columnKey) { ColumnResizeDrag() }

    // pointerInput is keyed on the column, so its block outlives the composition that started it:
    // stale geometry restarts every drag from the first width, a stale callback writes to a dead state.
    val currentBoundaryX by rememberUpdatedState(boundaryX)
    val currentWidth by rememberUpdatedState(widthResolver(columnKey))
    val currentMinWidth by rememberUpdatedState(minWidth)
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnResizeStart by rememberUpdatedState(onResizeStart)
    val currentOnResizeEnd by rememberUpdatedState(onResizeEnd)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)

    fun grow(deltaPx: Float) {
        val start = drag.startWidth ?: return
        drag.accumulatedPx += deltaPx
        currentOnResize(
            columnKey,
            (start + with(density) { drag.accumulatedPx.toDp() }).coerceAtLeast(currentMinWidth),
        )
        val pullback = resizeScrollPullback(drag.boundaryPx, horizontalState.value, horizontalState.viewportSize)
        if (pullback > 0f) horizontalState.dispatchRawDelta(pullback)
    }

    GrowWhileHeldAtEdge(drag, ::grow)
    SettleScrollAfterDrag(drag, horizontalState) { with(density) { currentBoundaryX.toPx() } }

    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .offset(x = boundaryX - reach, y = 0.dp)
                .hoverable(interactionSource = interaction)
                .pointerInput(columnKey) {
                    detectTapGestures(onDoubleTap = { currentOnDoubleClick(columnKey) })
                }.combinedClickable(onDoubleClick = { currentOnDoubleClick(columnKey) }) {}
                .pointerInput(columnKey) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            drag.begin(with(density) { currentBoundaryX.toPx() }, currentWidth)
                            currentOnResizeStart()
                        },
                        onDragEnd = {
                            drag.end()
                            currentOnResizeEnd()
                        },
                        onDragCancel = {
                            drag.end()
                            currentOnResizeEnd()
                        },
                    ) { change, dragAmount ->
                        val handleLeftPx = drag.boundaryPx - with(density) { reach.toPx() }
                        drag.onDrag(
                            dragAmount = dragAmount,
                            pointerViewportPx = handleLeftPx - horizontalState.value + change.position.x,
                            viewportPx = horizontalState.viewportSize,
                            zonePx = with(density) { EDGE_ZONE_DP.dp.toPx() },
                        )
                        grow(dragAmount)
                    }
                }.pointerHoverIcon(PointerIcon.Hand)
                .width(reach + dividerThickness + overhang)
                .background(
                    color =
                        if (isHovered) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)
                        } else {
                            Color.Transparent
                        },
                ),
    )
}

@Composable
private fun GrowWhileHeldAtEdge(
    drag: ColumnResizeDrag,
    grow: (Float) -> Unit,
) {
    val ratePx = with(LocalDensity.current) { EDGE_GROWTH_DP_PER_SECOND.dp.toPx() }
    val currentGrow by rememberUpdatedState(grow)
    LaunchedEffect(drag.holdingAtEdge) {
        if (!drag.holdingAtEdge) return@LaunchedEffect
        var previousNanos = withFrameNanos { it }
        while (true) {
            val nanos = withFrameNanos { it }
            currentGrow(ratePx * ((nanos - previousNanos) / NANOS_PER_SECOND))
            previousNanos = nanos
        }
    }
}

/** Mid-drag pull-backs are clamped by the last measured scroll range, so the release needs chasing. */
@Composable
private fun SettleScrollAfterDrag(
    drag: ColumnResizeDrag,
    horizontalState: ScrollState,
    boundaryPx: () -> Float,
) {
    val currentBoundaryPx by rememberUpdatedState(boundaryPx)
    LaunchedEffect(drag.settling) {
        if (!drag.settling) return@LaunchedEffect
        var frames = 0
        while (frames < SETTLE_FRAMES) {
            withFrameNanos { }
            val pullback =
                resizeScrollPullback(currentBoundaryPx(), horizontalState.value, horizontalState.viewportSize)
            if (pullback <= 0f) break
            horizontalState.dispatchRawDelta(pullback)
            frames++
        }
        drag.settled()
    }
}

/** One resize gesture. Tracks the boundary from drag start: the composed `cumulativeX` goes stale. */
private class ColumnResizeDrag {
    var startWidth: Dp? by mutableStateOf(null)
        private set

    /** True while the pointer has run out of travel at the right edge and the column must grow on its own. */
    var holdingAtEdge: Boolean by mutableStateOf(false)
        private set

    /** True until the scroll has caught up with the width the released gesture left behind. */
    var settling: Boolean by mutableStateOf(false)
        private set

    var accumulatedPx: Float by mutableFloatStateOf(0f)

    private var startBoundaryPx: Float = 0f

    val boundaryPx: Float get() = startBoundaryPx + accumulatedPx

    fun begin(
        boundaryPx: Float,
        width: Dp,
    ) {
        startBoundaryPx = boundaryPx
        startWidth = width
        accumulatedPx = 0f
        settling = false
    }

    fun end() {
        startWidth = null
        accumulatedPx = 0f
        holdingAtEdge = false
        settling = true
    }

    fun settled() {
        settling = false
    }

    fun onDrag(
        dragAmount: Float,
        pointerViewportPx: Float,
        viewportPx: Int,
        zonePx: Float,
    ) {
        holdingAtEdge = shouldGrowAtResizeEdge(dragAmount, pointerViewportPx, viewportPx, zonePx)
    }
}
