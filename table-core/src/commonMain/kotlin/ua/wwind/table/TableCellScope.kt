package ua.wwind.table

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableItem

/**
 * Context passed to cell content.
 *
 * This scope is intentionally generic to allow adding new cell-level capabilities in future.
 */
@Stable
public interface TableCellScope : TableRowScope

@Stable
internal class TableCellScopeImpl(private val delegate: TableRowScope) : TableCellScope, TableRowScope by delegate

@Immutable
internal object DefaultTableCellScope : TableCellScope, TableRowScope by DefaultTableRowScope

/**
 * Make the UI element the draggable handle for the reorderable item.
 *
 * This modifier can only be used on the UI element that is a child of [ReorderableItem].
 *
 * @param enabled Whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged.
 * @param onDragStarted The function that is called when the item starts being dragged
 * @param onDragStopped The function that is called when the item stops being dragged
 * @param dragGestureDetector [DragGestureDetector] that will be used to detect drag gestures
 */
public context(cellScope: TableCellScope)
fun Modifier.draggableHandle(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: () -> Unit = {},
    dragGestureDetector: DragGestureDetector = DragGestureDetector.Press
): Modifier = cellScope.applyDraggableHandle(
    modifier = this,
    enabled = enabled,
    interactionSource = interactionSource,
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped,
    dragGestureDetector = dragGestureDetector
)

/**
 * Make the UI element the draggable handle for the reorderable item. Drag will start only after a long press.
 *
 * This modifier can only be used on the UI element that is a child of [ReorderableItem].
 *
 * @param enabled Whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit [DragInteraction.Start] when this draggable is being dragged.
 * @param onDragStarted The function that is called when the item starts being dragged
 * @param onDragStopped The function that is called when the item stops being dragged
 */
public context(cellScope: TableCellScope)
fun Modifier.longPressDraggableHandle(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: () -> Unit = {},
): Modifier = cellScope.applyLongPressDraggableHandle(
    modifier = this,
    enabled = enabled,
    interactionSource = interactionSource,
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped
)
