package ua.wwind.table.format.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.component.collectAsEffect
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
internal fun <E : Enum<E>> FormatColorField(
    color: Color?,
    label: String,
    onClick: (Color?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    strings: StringProvider,
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    var show by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    if (enabled) {
        interactionSource.interactions.collectAsEffect {
            if (it is PressInteraction.Release) show = true
        }
    }
    OutlinedTextField(
        value = label,
        textStyle = MaterialTheme.typography.bodyLarge,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        modifier = modifier,
        interactionSource = interactionSource,
        trailingIcon =
            color?.let {
                {
                    IconButton(
                        onClick = { onClick(null) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                        )
                    }
                }
            },
        leadingIcon =
            color?.let {
                { Box(modifier = Modifier.size(32.dp).background(color)) }
            },
    )
    if (show) {
        ColorPickerDialog(
            initialColor = color ?: Color.Unspecified,
            onDismiss = { show = false },
            onChooseColor = { color ->
                onClick(
                    if (color == Color.Unspecified) null else color,
                )
                show = false
            },
            strings = strings,
            scrollbarRenderer = scrollbarRenderer,
        )
    }
}
