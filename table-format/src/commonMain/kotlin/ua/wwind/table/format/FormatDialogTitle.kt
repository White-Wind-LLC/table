package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun <E : Enum<E>, FILTER> FormatDialogTitle(
    state: FormatDialogState<E, FILTER>,
    strings: StringProvider,
    titleContentColor: Color,
    onDismissRequest: (() -> Unit)?,
) {
    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.get(UiString.FormatRules),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (state.editItem == null && onDismissRequest != null) {
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        }
    }
}
