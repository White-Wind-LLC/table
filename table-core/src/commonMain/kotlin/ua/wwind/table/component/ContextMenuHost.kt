package ua.wwind.table.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import ua.wwind.table.interaction.ContextMenuState

@Composable
internal fun <T : Any> ContextMenuHost(
    contextMenuState: ContextMenuState<T>,
    contextMenu: (@Composable (item: T, pos: Offset, dismiss: () -> Unit) -> Unit)?,
    onDismiss: () -> Unit,
) {
    if (contextMenuState.visible) {
        contextMenuState.item?.let { item ->
            contextMenu?.invoke(item, contextMenuState.position, onDismiss)
        }
    }
}
