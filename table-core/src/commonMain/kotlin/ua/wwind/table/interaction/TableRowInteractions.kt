package ua.wwind.table.interaction

import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
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
    this.then(
        Modifier.combinedClickable(
            // The row stays clickable even without a primary action, so that a click still moves focus.
            onClick = {
                onFocus?.invoke(item)
                onPrimary?.invoke(item)
            },
            onDoubleClick = onDoubleClick?.let { { it(item) } },
            onLongClick = onLongClick?.let { { it(item) } },
        ),
    )

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
