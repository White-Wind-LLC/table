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
 * Scope available inside table body cell content.
 *
 * Extends [TableItemScope] with a cell-specific receiver used by the `cell { ... }` DSL,
 * so cell content can access item-level helpers in a more natural way.
 */
@Stable
public interface TableCellScope : TableItemScope

@Stable
internal class TableCellScopeImpl(private val delegate: TableItemScope) : TableCellScope, TableItemScope by delegate

@Immutable
internal object DefaultTableCellScope : TableCellScope, TableItemScope by DefaultTableItemScope

/**
 * Makes this modifier act as a drag handle for the current table item from cell content.
 *
 * This is a convenience wrapper around [TableItemScope.applyDraggableHandle] for use inside
 * a [TableCellScope] context. The modifier only works correctly when applied to a node that
 * is a descendant of [ReorderableItem].
 *
 * @param enabled whether drag gestures are enabled for this handle.
 * @param interactionSource optional [MutableInteractionSource] used to emit
 * [DragInteraction.Start] when dragging begins.
 * @param onDragStarted callback invoked when dragging starts.
 * @param onDragStopped callback invoked when dragging stops.
 * @param dragGestureDetector gesture detector that defines how dragging is started.
 * @return a modifier with drag-handle behavior applied.
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
 * Makes this modifier act as a long-press drag handle for the current table item from cell
 * content.
 *
 * This is a convenience wrapper around [TableItemScope.applyLongPressDraggableHandle] for use
 * inside a [TableCellScope] context. The modifier only works correctly when applied to a node
 * that is a descendant of [ReorderableItem]. Dragging starts only after a long press.
 *
 * @param enabled whether drag gestures are enabled for this handle.
 * @param interactionSource optional [MutableInteractionSource] used to emit
 * [DragInteraction.Start] when dragging begins.
 * @param onDragStarted callback invoked when dragging starts.
 * @param onDragStopped callback invoked when dragging stops.
 * @return a modifier with long-press drag-handle behavior applied.
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
