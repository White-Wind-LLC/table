package ua.wwind.table.component.header

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.icon.TableIcons

/** Opens the column menu. Not focusable: the header is the Tab stop, and Shift+F10 opens the menu. */
@Composable
internal fun ColumnMenuButton(
    contentDescription: String,
    targetSize: Dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(targetSize).focusProperties { canFocus = false },
    ) {
        Icon(TableIcons.MoreVert, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
