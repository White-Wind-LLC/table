package ua.wwind.table.filter.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
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

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
public fun FilterDropdownAnyField(
    currentValue: Any?,
    getTitle: @Composable (Any) -> String = { it.toString() },
    placeholder: String = "",
    values: List<Any>,
    onClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    checked: ((Any) -> Boolean)? = null,
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = currentValue?.let { getTitle(it) } ?: placeholder,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier =
                modifier.menuAnchor(
                    MenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                ),
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
