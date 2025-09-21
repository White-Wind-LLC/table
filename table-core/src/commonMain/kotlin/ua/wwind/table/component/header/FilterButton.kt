package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import ua.wwind.table.component.TableHeaderIcons

@Composable
internal fun FilterButton(
    enabled: Boolean,
    active: Boolean,
    icons: TableHeaderIcons,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    panel: @Composable () -> Unit,
) {
    if (!enabled) return
    Box {
        IconButton(
            onClick = { if (!isOpen) onOpen() else onDismiss() },
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                imageVector = if (active) icons.filterActive else icons.filterInactive,
                contentDescription = null,
            )
        }
        if (isOpen) {
            panel()
        }
    }
}
