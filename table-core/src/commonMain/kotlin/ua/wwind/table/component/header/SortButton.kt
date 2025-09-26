package ua.wwind.table.component.header

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.data.SortOrder

@Composable
internal fun SortButton(
    enabled: Boolean,
    order: SortOrder?,
    icons: TableHeaderIcons,
    onToggle: () -> Unit,
    clickable: Boolean,
) {
    if (!enabled) return
    val sortIcon =
        when (order) {
            SortOrder.DESCENDING -> icons.sortDesc
            SortOrder.ASCENDING -> icons.sortAsc
            null -> icons.sortNeutral
        }
    Icon(
        imageVector = sortIcon,
        contentDescription = null,
        modifier =
            Modifier
                .size(24.dp)
                .then(if (clickable) Modifier.clickable { onToggle() } else Modifier)
                .padding(start = 4.dp),
    )
}
