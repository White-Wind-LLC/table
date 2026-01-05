package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.component.TableHeaderIcons

@Composable
internal fun FilterButton(
    enabled: Boolean,
    active: Boolean,
    icons: TableHeaderIcons,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!enabled) return
    Box {
        if (active) {
            FilledTonalIconButton(
                onClick = { if (!isOpen) onOpen() else onDismiss() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = icons.filterActive,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            IconButton(
                onClick = { if (!isOpen) onOpen() else onDismiss() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = icons.filterInactive,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
