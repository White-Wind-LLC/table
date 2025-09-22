package ua.wwind.table.component.header

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDimensions

private const val OVERLAY_THICKNESS_DP = 6

@Composable
internal fun <T : Any, C> ColumnResizersOverlay(
    tableWidth: Dp,
    visibleColumns: List<ColumnSpec<T, C>>,
    widthResolver: (C) -> Dp,
    dimensions: TableDimensions,
    leadingColumnWidth: Dp?,
    onResize: (key: C, newWidth: Dp) -> Unit,
    onResizeStart: () -> Unit = {},
    onResizeEnd: () -> Unit = {},
    onDoubleClick: (key: C) -> Unit = {},
) {
    Box(modifier = Modifier.width(tableWidth)) {
        val density = LocalDensity.current

        // Compute absolute X offsets (in Dp) for each column boundary divider
        var cumulativeX: Dp = 0.dp
        if (leadingColumnWidth != null) {
            cumulativeX += leadingColumnWidth + dimensions.verticalDividerThickness
            // Note: Leading divider is non-resizable; we intentionally do not draw an overlay here.
        }

        visibleColumns.forEachIndexed { index, spec ->
            val currentWidth = widthResolver(spec.key)
            cumulativeX += currentWidth

            if (spec.resizable) {
                // Resize handle at absolute position cumulativeX
                var dragStartWidth by remember(spec.key) { mutableStateOf<Dp?>(null) }
                var accumulatedDeltaPx by remember(spec.key) { mutableStateOf(0f) }

                val interaction = remember(spec.key) { MutableInteractionSource() }
                val isHovered = interaction.collectIsHoveredAsState().value
                val overlayOffset =
                    if (index == visibleColumns.size - 1) OVERLAY_THICKNESS_DP else OVERLAY_THICKNESS_DP / 2

                Box(
                    modifier =
                        Modifier
                            .height(dimensions.defaultRowHeight)
                            // Absolute placement at boundary
                            .offset(x = cumulativeX - overlayOffset.dp, y = 0.dp)
                            .hoverable(interactionSource = interaction)
                            .pointerInput(spec.key) {
                                detectTapGestures(onDoubleTap = { onDoubleClick(spec.key) })
                            }
                            .combinedClickable(onDoubleClick = { onDoubleClick(spec.key) }) {}
                            .draggable(
                                state =
                                    rememberDraggableState { delta ->
                                        accumulatedDeltaPx += delta

                                        val baseWidth = dragStartWidth ?: currentWidth
                                        val totalDeltaDp = with(density) { accumulatedDeltaPx.toDp() }
                                        val newWidth =
                                            (baseWidth + totalDeltaDp).coerceAtLeast(spec.minWidth)
                                        onResize(spec.key, newWidth)
                                    },
                                orientation = Orientation.Horizontal,
                                onDragStarted = {
                                    dragStartWidth = widthResolver(spec.key)
                                    accumulatedDeltaPx = 0f
                                    onResizeStart()
                                },
                                onDragStopped = {
                                    dragStartWidth = null
                                    accumulatedDeltaPx = 0f
                                    onResizeEnd()
                                },
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                            .width(dimensions.verticalDividerThickness + OVERLAY_THICKNESS_DP.dp)
                            .background(
                                color = if (isHovered) {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            ),
                )
            }
            // Advance past the divider thickness to align subsequent handles
            cumulativeX += dimensions.verticalDividerThickness
        }
    }
}
