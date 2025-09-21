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
 * - Desktop/Web: single click -> select (or click), double click -> open (if select is primary), right click -> context menu
 * - Mobile: tap -> select (if requested) or open, long press if provided
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
public fun <T : Any> Modifier.tableRowInteractions(
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
        val tapHandler: (() -> Unit)? =
            when {
                useSelectAsPrimary && onSelect != null -> ({ onSelect(item) })
                onClick != null -> ({ onClick(item) })
                else -> null
            }
        if (tapHandler != null || onLongClick != null) {
            this.then(
                Modifier.combinedClickable(
                    onClick = {
                        onFocus?.invoke(item)
                        tapHandler?.invoke()
                    },
                    onLongClick = onLongClick?.let { { it(item) } },
                ),
            )
        } else {
            this
        }
    } else {
        var desktopModifier =
            this.then(
                Modifier.combinedClickable(
                    onClick =
                        if (useSelectAsPrimary) {
                            onSelect?.let {
                                {
                                    onFocus?.invoke(item)
                                    it(item)
                                }
                            } ?: { onFocus?.invoke(item) }
                        } else {
                            onClick?.let {
                                {
                                    onFocus?.invoke(item)
                                    it(item)
                                }
                            } ?: { onFocus?.invoke(item) }
                        },
                    onDoubleClick = if (useSelectAsPrimary) onClick?.let { { it(item) } } else null,
                    onLongClick = onLongClick?.let { { it(item) } },
                ),
            )

        if (onContextMenu != null) {
            desktopModifier =
                desktopModifier.then(
                    Modifier.pointerInput(item) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (
                                    event.type == PointerEventType.Press &&
                                    event.buttons.isSecondaryPressed
                                ) {
                                    val pos = event.changes.firstOrNull()?.position ?: Offset.Zero
                                    onContextMenu.invoke(item, pos)
                                }
                            }
                        }
                    },
                )
        }
        desktopModifier
    }
}
