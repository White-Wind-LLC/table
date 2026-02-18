package ua.wwind.table.format.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.filter.component.collectAsEffect
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun <E : Enum<E>> FormatDropdownField(
    currentValue: E?,
    getTitle: @Composable (E) -> String = { it.name },
    placeholder: String = "",
    label: @Composable (() -> Unit)? = null,
    values: ImmutableList<E>,
    onClick: (E?) -> Unit,
    modifier: Modifier = Modifier,
    checked: ((E) -> Boolean)? = null,
    enabled: Boolean = true,
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    if (enabled) {
        interactionSource.interactions.collectAsEffect {
            if (it is PressInteraction.Release) expanded = true
        }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = currentValue?.let { getTitle(it) } ?: placeholder,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = label,
            modifier = modifier,
            trailingIcon =
                currentValue?.let {
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
            interactionSource = interactionSource,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Box {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(scrollState),
                ) {
                    values.forEach { enum ->
                        DropdownMenuItem(
                            text = {
                                when (checked) {
                                    null -> {
                                        Text(getTitle(enum))
                                    }

                                    else -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = checked.invoke(enum),
                                                onCheckedChange = {
                                                    onClick(enum)
                                                },
                                            )
                                            Text(getTitle(enum))
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onClick(enum)
                                if (checked == null) expanded = false
                            },
                        )
                    }
                }
                scrollbarRenderer?.Render(
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight(),
                    state = VerticalScrollbarState.Scroll(scrollState),
                )
            }
        }
    }
}
