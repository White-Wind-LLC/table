package ua.wwind.table.interaction

import androidx.compose.ui.geometry.Offset

/**
 * State holder for table row context menu.
 */
public data class ContextMenuState<T : Any>(
    val visible: Boolean = false,
    val position: Offset = Offset.Zero,
    val item: T? = null,
)
