package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import ua.wwind.table.component.TableHeaderIcons
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/** The filter button's compact size; a larger target size (touch) widens it. */
private const val MIN_FILTER_BUTTON_SIZE_DP = 32

/** The header is one Tab stop; its buttons are reached by pointer, not by Tab. */
private val NotFocusable = Modifier.focusProperties { canFocus = false }

@Composable
@Suppress("LongParameterList")
internal fun FilterButton(
    enabled: Boolean,
    active: Boolean,
    icons: TableHeaderIcons,
    strings: StringProvider,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    targetSize: Dp,
) {
    if (!enabled) return
    Box {
        if (active) {
            FilledTonalIconButton(
                onClick = { if (!isOpen) onOpen() else onDismiss() },
                modifier = Modifier.size(max(MIN_FILTER_BUTTON_SIZE_DP.dp, targetSize)).then(NotFocusable),
            ) {
                Icon(
                    painter = icons.filterActive,
                    contentDescription = strings.get(UiString.HeaderFilterActive),
                    modifier = Modifier.size(HeaderIconSize),
                )
            }
        } else {
            IconButton(
                onClick = { if (!isOpen) onOpen() else onDismiss() },
                modifier = Modifier.size(max(MIN_FILTER_BUTTON_SIZE_DP.dp, targetSize)).then(NotFocusable),
            ) {
                Icon(
                    painter = icons.filterInactive,
                    contentDescription = strings.get(UiString.HeaderFilter),
                    modifier = Modifier.size(HeaderIconSize),
                )
            }
        }
    }
}
