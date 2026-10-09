package ua.wwind.table.interaction

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile

/**
 * Builds platform-aware interactions for a table row.
 * - Desktop/Web: single click -> select (or click), double click -> open (if select is primary),
 *   right click -> context menu
 * - Mobile: tap -> select (if requested) or open, long press if provided
 *
 * The clickable never takes focus itself: the table is a single Tab stop whose own focus target
 * handles the arrow keys, so a focusable cell would only add a Tab stop per cell. Nor does it draw
 * an indication: it reports hover and press to [interactionSource], which the row shares across its
 * cells and draws as one state layer.
 */
@Suppress("LongParameterList")
internal fun <T : Any> Modifier.tableRowInteractions(
    item: T?,
    interactionSource: MutableInteractionSource,
    onFocus: ((T) -> Unit)? = null,
    useSelectAsPrimary: Boolean,
    onSelect: ((T) -> Unit)?,
    onClick: ((T) -> Unit)?,
    onLongClick: ((T) -> Unit)?,
    onContextMenu: ((item: T, position: Offset) -> Unit)?,
): Modifier {
    if (item == null) return this
    return if (getPlatform().isMobile()) {
        // Mobile has no double click, so a row that only wants selection but has no [onSelect]
        // falls back to [onClick] rather than losing the action entirely.
        val onTap = (if (useSelectAsPrimary) onSelect else null) ?: onClick
        mobileRowInteractions(item, interactionSource, onFocus, onTap, onLongClick)
    } else {
        desktopRowInteractions(
            item = item,
            interactionSource = interactionSource,
            onFocus = onFocus,
            onPrimary = if (useSelectAsPrimary) onSelect else onClick,
            // Selection took the single click, so opening the row moves to the double click.
            onDoubleClick = if (useSelectAsPrimary) onClick else null,
            onLongClick = onLongClick,
        ).contextMenuGesture(item, onContextMenu)
    }
}

/**
 * Keeps keys that bubble up from a cell's content away from the row's clickable, and hands them to
 * the table instead. The clickable never holds focus, so every key it would see comes from content
 * such as an edit field — and it would turn Enter or Space into a row click. Chain it after
 * [tableRowInteractions] so that it sees those keys first.
 */
internal fun Modifier.forwardContentKeysToTable(onKey: (KeyEvent) -> Boolean): Modifier =
    onKeyEvent { event ->
        when (event.key) {
            Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter -> {
                onKey(event)
                true
            }

            // Anything else bubbles on to the table as usual.
            else -> {
                false
            }
        }
    }

private fun <T : Any> Modifier.mobileRowInteractions(
    item: T,
    interactionSource: MutableInteractionSource,
    onFocus: ((T) -> Unit)?,
    onTap: ((T) -> Unit)?,
    onLongClick: ((T) -> Unit)?,
): Modifier {
    // With nothing to invoke there is no reason to make the row clickable at all.
    if (onTap == null && onLongClick == null) return this
    return this.then(
        Modifier.focusProperties { canFocus = false }.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                onFocus?.invoke(item)
                onTap?.invoke(item)
            },
            onLongClick = onLongClick?.let { { it(item) } },
        ),
    )
}

@Suppress("LongParameterList")
private fun <T : Any> Modifier.desktopRowInteractions(
    item: T,
    interactionSource: MutableInteractionSource,
    onFocus: ((T) -> Unit)?,
    onPrimary: ((T) -> Unit)?,
    onDoubleClick: ((T) -> Unit)?,
    onLongClick: ((T) -> Unit)?,
): Modifier =
    this
        .then(
            // No onDoubleClick here: combinedClickable would hold every click back for the whole
            // double-tap window to rule out a second one, delaying selection.
            Modifier.focusProperties { canFocus = false }.combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                // The row stays clickable even without a primary action, so that a click still moves focus.
                onClick = {
                    onFocus?.invoke(item)
                    onPrimary?.invoke(item)
                },
                onLongClick = onLongClick?.let { { it(item) } },
            ),
        ).then(
            // Chained inside the clickable so that it sees the Main pass after the cell content but before
            // the row's clickable consumes the click, which tells a child's click apart from the row's own.
            if (onDoubleClick == null) Modifier else Modifier.then(DoubleClickElement { onDoubleClick(item) }),
        )

/**
 * Skiko hardcodes a 300 ms double-tap timeout and ignores the OS setting; 500 ms is the Windows and
 * macOS default, so a slower double click is still one to the user.
 */
private const val MIN_DOUBLE_CLICK_TIMEOUT_MILLIS = 500L

/**
 * Detects a double click without delaying the single click, which the enclosing clickable handles as
 * usual. Kept as a node so the timestamp of the previous click survives the recomposition the first
 * click causes.
 */
private class DoubleClickElement(
    private val onDoubleClick: () -> Unit,
) : ModifierNodeElement<DoubleClickNode>() {
    override fun create(): DoubleClickNode = DoubleClickNode(onDoubleClick)

    override fun update(node: DoubleClickNode) {
        node.onDoubleClick = onDoubleClick
    }

    override fun equals(other: Any?): Boolean = other is DoubleClickElement && other.onDoubleClick === onDoubleClick

    override fun hashCode(): Int = onDoubleClick.hashCode()
}

private class DoubleClickNode(
    var onDoubleClick: () -> Unit,
) : DelegatingNode() {
    private var lastClickUptime: Long? = null

    init {
        delegate(SuspendingPointerInputModifierNode { detectDoubleClicks() })
    }

    private suspend fun PointerInputScope.detectDoubleClicks() {
        val timeout = maxOf(viewConfiguration.doubleTapTimeoutMillis, MIN_DOUBLE_CLICK_TIMEOUT_MILLIS)
        awaitEachGesture {
            // Observe only: the row's clickable consumes these events, and it must keep doing so.
            val down = awaitFirstDown(requireUnconsumed = false)
            // A click the cell content consumed belongs to that content, not to the row. Read it now: the
            // change is shared, and the row's clickable consumes it later in this same pass.
            val downConsumedByContent = down.isConsumed
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            var up: PointerInputChange
            do {
                val event = awaitPointerEvent()
                up = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            } while (up.pressed)
            if (downConsumedByContent || up.isConsumed || up.isOutOfBounds(size, extendedTouchPadding)) {
                lastClickUptime = null
                return@awaitEachGesture
            }
            val previous = lastClickUptime
            if (previous != null && down.uptimeMillis - previous <= timeout) {
                lastClickUptime = null
                onDoubleClick()
            } else {
                lastClickUptime = up.uptimeMillis
            }
        }
    }
}

private fun <T : Any> Modifier.contextMenuGesture(
    item: T,
    onContextMenu: ((item: T, position: Offset) -> Unit)?,
): Modifier {
    if (onContextMenu == null) return this
    return this.then(
        Modifier.pointerInput(item) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val pos = event.changes.firstOrNull()?.position ?: Offset.Zero
                        onContextMenu.invoke(item, pos)
                    }
                }
            }
        },
    )
}
