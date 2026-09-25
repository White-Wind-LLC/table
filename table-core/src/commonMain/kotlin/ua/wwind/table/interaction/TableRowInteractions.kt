package ua.wwind.table.interaction

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 */
@Suppress("LongParameterList")
internal fun <T : Any> Modifier.tableRowInteractions(
    item: T?,
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
        mobileRowInteractions(item, onFocus, onTap, onLongClick)
    } else {
        desktopRowInteractions(
            item = item,
            onFocus = onFocus,
            onPrimary = if (useSelectAsPrimary) onSelect else onClick,
            // Selection took the single click, so opening the row moves to the double click.
            onDoubleClick = if (useSelectAsPrimary) onClick else null,
            onLongClick = onLongClick,
        ).contextMenuGesture(item, onContextMenu)
    }
}

private fun <T : Any> Modifier.mobileRowInteractions(
    item: T,
    onFocus: ((T) -> Unit)?,
    onTap: ((T) -> Unit)?,
    onLongClick: ((T) -> Unit)?,
): Modifier {
    // With nothing to invoke there is no reason to make the row clickable at all.
    if (onTap == null && onLongClick == null) return this
    return this.then(
        Modifier.combinedClickable(
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
    onFocus: ((T) -> Unit)?,
    onPrimary: ((T) -> Unit)?,
    onDoubleClick: ((T) -> Unit)?,
    onLongClick: ((T) -> Unit)?,
): Modifier =
    this
        .then(
            if (onDoubleClick == null) Modifier else Modifier.then(DoubleClickElement { onDoubleClick(item) }),
        ).then(
            // No onDoubleClick here: combinedClickable would hold every click back for the whole
            // double-tap window to rule out a second one, delaying selection.
            Modifier.combinedClickable(
                // The row stays clickable even without a primary action, so that a click still moves focus.
                onClick = {
                    onFocus?.invoke(item)
                    onPrimary?.invoke(item)
                },
                onLongClick = onLongClick?.let { { it(item) } },
            ),
        )

/**
 * Skiko hardcodes a 300 ms double-tap timeout and ignores the OS setting; 500 ms is the Windows and
 * macOS default, so a slower double click is still one to the user.
 */
private const val MIN_DOUBLE_CLICK_TIMEOUT_MILLIS = 500L

/**
 * Detects a double click without delaying the single click, which the sibling clickable handles as
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
            // Observe only: the clickable consumes these events, and it must keep doing so.
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            var up: PointerInputChange
            do {
                val event = awaitPointerEvent()
                up = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            } while (up.pressed)
            if (up.isOutOfBounds(size, extendedTouchPadding)) {
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
