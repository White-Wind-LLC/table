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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults

/**
 * Collects a [Flow] in a composable scope, invoking [block] on each emission.
 * Shorthand utility to avoid manual `LaunchedEffect` wiring.
 */
@Composable
public fun <T> Flow<T>.collectAsEffect(block: (T) -> Unit) {
    val currentBlock = rememberUpdatedState(block)

    LaunchedEffect(this, currentBlock) {
        this@collectAsEffect.collect {
            currentBlock.value(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
/**
 * Dropdown field specialized for enum values. Optionally supports multi-select with [checked].
 */
public fun <E : Enum<E>> FilterDropdownField(
    currentValue: E?,
    getTitle: @Composable (E) -> String = { it.name },
    placeholder: String = "",
    values: List<E>,
    onClick: (E) -> Unit,
    modifier: Modifier = Modifier,
    checked: ((E) -> Boolean)? = null,
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TableTextField(
            value = currentValue?.let { getTitle(it) } ?: placeholder,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = modifier.menuAnchor(
                MenuAnchorType.PrimaryNotEditable,
                enabled = true,
            ),
            contentPadding = contentPadding,
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
                                    null -> Text(getTitle(enum))
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
            }
        }
    }
}
