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
        dragGestureDetector: DragGestureDetector = DragGestureDetector.Press,
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

/**
 * Scope for a row block's header band. Distinct from [TableCellScope] (not a supertype of it) so a
 * [Modifier.draggableHandle] call here binds unambiguously to the whole-block (outer unit) engine,
 * while the same call inside a cell binds to whatever engine that row is rendered under.
 */
@Stable
public interface RowBlockHeaderScope : TableItemScope

@Stable
internal class RowBlockHeaderScopeImpl(
    delegate: TableItemScope,
) : RowBlockHeaderScope,
    TableItemScope by delegate

/**
 * Makes this modifier the drag handle for the whole block, from its header band. The node must be a
 * descendant of the block's [ReorderableItem].
 */
context(scope: RowBlockHeaderScope)
public fun Modifier.draggableHandle(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: () -> Unit = {},
    dragGestureDetector: DragGestureDetector = DragGestureDetector.Press,
): Modifier =
    scope.applyDraggableHandle(
        modifier = this,
        enabled = enabled,
        interactionSource = interactionSource,
        onDragStarted = onDragStarted,
        onDragStopped = onDragStopped,
        dragGestureDetector = dragGestureDetector,
    )

@Immutable
internal object DefaultTableItemScope : TableItemScope {
    override fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector,
    ): Modifier = modifier

    override fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
    ): Modifier = modifier
}

/**
 * Lazy-path drag scope. The hooks wrap the consumer's handle callbacks so the block commit fires
 * exactly once per gesture, observed by the handle that started it.
 */
@Stable
internal class TableItemDragScope(
    private val delegate: ReorderableCollectionItemScope,
    private val onDragStartedHook: (() -> Unit)? = null,
    private val onDragStoppedHook: (() -> Unit)? = null,
) : TableItemScope,
    ReorderableCollectionItemScope by delegate {
    override fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector,
    ): Modifier =
        context(delegate) {
            modifier.draggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = { startedPosition ->
                    onDragStartedHook?.invoke()
                    onDragStarted(startedPosition)
                },
                onDragStopped = {
                    onDragStoppedHook?.invoke()
                    onDragStopped()
                },
                dragGestureDetector = dragGestureDetector,
            )
        }

    override fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
    ): Modifier =
        context(delegate) {
            modifier.longPressDraggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = { startedPosition ->
                    onDragStartedHook?.invoke()
                    onDragStarted(startedPosition)
                },
                onDragStopped = {
                    onDragStoppedHook?.invoke()
                    onDragStopped()
                },
            )
        }
}

/**
 * Embedded-path drag scope. Only a start hook: the embedded engine reports the gesture's net result
 * through its settle callback, which is where the commit rides instead.
 */
@Stable
internal class TableItemListDragScope(
    private val delegate: ReorderableListItemScope,
    private val onDragStartedHook: (() -> Unit)? = null,
) : TableItemScope,
    ReorderableListItemScope by delegate {
    override fun applyDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
        dragGestureDetector: DragGestureDetector,
    ): Modifier =
        context(delegate) {
            modifier.draggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = { startedPosition ->
                    onDragStartedHook?.invoke()
                    onDragStarted(startedPosition)
                },
                onDragStopped = { onDragStopped() },
                dragGestureDetector = dragGestureDetector,
            )
        }

    override fun applyLongPressDraggableHandle(
        modifier: Modifier,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: () -> Unit,
    ): Modifier =
        context(delegate) {
            modifier.longPressDraggableHandle(
                enabled = enabled,
                interactionSource = interactionSource,
                onDragStarted = { startedPosition ->
                    onDragStartedHook?.invoke()
                    onDragStarted(startedPosition)
                },
                onDragStopped = { onDragStopped() },
            )
        }
}
