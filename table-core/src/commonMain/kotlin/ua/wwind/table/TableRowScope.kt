package ua.wwind.table

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem

/**
 * Context passed to cell content.
 *
 * This scope is intentionally generic to allow adding new cell-level capabilities in future.
 */
@Stable
public interface TableRowScope : ReorderableCollectionItemScope {
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
    public fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: () -> Unit = {},
        dragGestureDetector: DragGestureDetector = DragGestureDetector.Press
    ): Modifier

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
    public fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource? = null,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: () -> Unit = {},
    ): Modifier
}

@Immutable
internal object DefaultTableRowScope : TableRowScope {
    override fun Modifier.draggableHandle(
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector
    ): Modifier = this

    override fun Modifier.longPressDraggableHandle(
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit
    ): Modifier = this

    override fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector
    ): Modifier = modifier

    override fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit
    ): Modifier = modifier
}

@Stable
internal class TableRowDragScope(
    private val delegate: ReorderableCollectionItemScope
) : TableCellScope, ReorderableCollectionItemScope by delegate {
    override fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector
    ): Modifier {
        return context(delegate) {
            modifier.draggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
                dragGestureDetector = dragGestureDetector
            )
        }
    }

    override fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit
    ): Modifier {
        return context(delegate) {
            modifier.longPressDraggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped,
            )
        }
    }
}
