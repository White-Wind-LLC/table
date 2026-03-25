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
import sh.calvin.reorderable.ReorderableListItemScope

/**
 * Scope available for a rendered table item.
 *
 * Provides item-level interaction helpers that can be reused by more specific scopes,
 * such as [TableCellScope].
 */
@Stable
public interface TableItemScope {
    /**
     * Returns a [Modifier] that makes the given UI element a drag handle for the current table item.
     *
     * This API is intended to be used from content rendered inside the current reorderable item.
     * The resulting modifier only works correctly when applied to a node that is a descendant
     * of [ReorderableItem].
     *
     * @param modifier base modifier to extend with drag-handle behavior.
     * @param enabled whether drag gestures are enabled for this handle.
     * @param interactionSource optional [MutableInteractionSource] used to emit
     * [DragInteraction.Start] when dragging begins.
     * @param onDragStarted callback invoked when dragging starts.
     * @param onDragStopped callback invoked when dragging stops.
     * @param dragGestureDetector gesture detector that defines how dragging is started.
     * @return the resulting modifier with drag-handle behavior applied.
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
     * Returns a [Modifier] that makes the given UI element a long-press drag handle for the
     * current table item.
     *
     * This API is intended to be used from content rendered inside the current reorderable item.
     * The resulting modifier only works correctly when applied to a node that is a descendant
     * of [ReorderableItem]. Dragging starts only after a long press.
     *
     * @param modifier base modifier to extend with drag-handle behavior.
     * @param enabled whether drag gestures are enabled for this handle.
     * @param interactionSource optional [MutableInteractionSource] used to emit
     * [DragInteraction.Start] when dragging begins.
     * @param onDragStarted callback invoked when dragging starts.
     * @param onDragStopped callback invoked when dragging stops.
     * @return the resulting modifier with long-press drag-handle behavior applied.
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
internal object DefaultTableItemScope : TableItemScope {
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
internal class TableItemDragScope(
    private val delegate: ReorderableCollectionItemScope
) : TableItemScope, ReorderableCollectionItemScope by delegate {
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

@Stable
internal class TableItemListDragScope(
    private val delegate: ReorderableListItemScope
) : TableItemScope, ReorderableListItemScope by delegate {
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
                onDragStopped = { onDragStopped() },
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
                onDragStopped = { onDragStopped() },
            )
        }
    }
}
