package ua.wwind.table.filter.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun FilterPanelActions(
    autoApplyFilters: Boolean,
    enabled: Boolean = true,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    strings: StringProvider,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(280.dp),
    ) {
        TextButton(
            onClick = {
                onClear()
                onClose()
            },
        ) {
            Text(strings.get(UiString.FilterClear))
        }
        if (!autoApplyFilters) {
            Button(
                enabled = enabled,
                onClick = {
                    onApply()
                    onClose()
                },
            ) {
                Text(strings.get(UiString.FilterApply))
            }
        }
    }
}
