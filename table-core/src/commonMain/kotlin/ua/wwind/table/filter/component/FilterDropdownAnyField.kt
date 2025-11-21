package ua.wwind.table.filter.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
/**
 * Generic dropdown field used by filter panels to select one or many values of arbitrary type.
 * If [checked] is provided, the menu displays checkboxes for multi-select.
 */
public fun FilterDropdownAnyField(
    currentValue: Any?,
    getTitle: @Composable (Any) -> String = { it.toString() },
    placeholder: String = "",
    values: ImmutableList<Any>,
    onClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    checked: ((Any) -> Boolean)? = null,
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
    showBorder: Boolean = true,
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TableTextField(
            value = currentValue?.let { getTitle(it) } ?: "",
            onValueChange = {},
            placeholder = {
                Text(
                    text = placeholder,
                    maxLines = 1,
                )
            },
            readOnly = true,
            singleLine = true,
            modifier =
                modifier.menuAnchor(
                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                ),
            contentPadding = contentPadding,
            showBorder = showBorder,
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
                    values.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                when (checked) {
                                    null -> Text(getTitle(item))
                                    else -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Checkbox(
                                                checked = checked.invoke(item),
                                                onCheckedChange = {
                                                    onClick(item)
                                                },
                                            )
                                            Text(getTitle(item))
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onClick(item)
                                if (checked == null) expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
