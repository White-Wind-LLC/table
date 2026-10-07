package ua.wwind.table.component.header

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
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
    targetSize: Dp,
) {
    if (!enabled) return
    val sortIcon =
        when (order) {
            SortOrder.DESCENDING -> icons.sortDesc
            SortOrder.ASCENDING -> icons.sortAsc
            null -> icons.sortNeutral
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .sizeIn(minWidth = targetSize, minHeight = targetSize)
                .then(
                    if (clickable) {
                        // The header row is the Tab stop; the button is reached with the pointer only.
                        Modifier.focusProperties { canFocus = false }.clickable(role = Role.Button) {
                            onToggle()
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Icon(
            imageVector = sortIcon,
            contentDescription = null,
            modifier = Modifier.size(HeaderIconSize).padding(start = 4.dp),
            tint =
                sortIconTint(
                    order = order,
                    contentColor = LocalContentColor.current,
                    primary = MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

/** The sort icon's tint: [primary] while the column is sorted, a dimmed [contentColor] otherwise. */
internal fun sortIconTint(
    order: SortOrder?,
    contentColor: Color,
    primary: Color,
): Color = if (order != null) primary else contentColor.copy(alpha = NEUTRAL_SORT_ICON_ALPHA)

/** Material's disabled-content emphasis: visible enough to signal sortability without competing. */
private const val NEUTRAL_SORT_ICON_ALPHA = 0.38f
