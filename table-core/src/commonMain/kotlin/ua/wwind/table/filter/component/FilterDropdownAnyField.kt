package ua.wwind.table.filter.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** Option lists longer than this get a search field when a [FilterDropdownAnyField] search placeholder is set. */
internal const val OPTIONS_SEARCH_THRESHOLD: Int = 8

/**
 * Generic dropdown field used by filter panels to select one or many values of arbitrary type.
 * If [checked] is provided, the menu displays checkboxes for multi-select.
 *
 * The menu scrolls within the height available in the window.
 *
 * @param searchPlaceholder when set and there are more than eight [values], the menu starts with a
 *   search field that narrows the options by their [getTitle], ignoring case; null shows no search
 * @param selectAllLabel label of the action that checks every option shown
 * @param selectNoneLabel label of the action that unchecks every option shown
 * @param onShownCheckedChange called by the Select all / None actions with the options shown (those matching
 *   the search) and whether to check them; the actions appear only in multi-select mode when this and
 *   both labels are set
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
public fun FilterDropdownAnyField(
    currentValue: Any?,
    values: ImmutableList<Any>,
    onClick: (Any) -> Unit,
    modifier: Modifier = Modifier,
    getTitle: @Composable (Any) -> String = { it.toString() },
    placeholder: String = "",
    checked: ((Any) -> Boolean)? = null,
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
    showBorder: Boolean = true,
    searchPlaceholder: String? = null,
    selectAllLabel: String? = null,
    selectNoneLabel: String? = null,
    onShownCheckedChange: ((List<Any>, Boolean) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(expanded) {
        if (!expanded) query = ""
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
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
                Modifier.menuAnchor(
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
            val showSearch = searchPlaceholder != null && values.size > OPTIONS_SEARCH_THRESHOLD
            val titled = values.map { it to getTitle(it) }
            val shown = if (showSearch) titled.matching(query) else titled
            if (showSearch) {
                OptionsSearchField(query = query, onQueryChange = { query = it }, placeholder = searchPlaceholder)
            }
            if (checked != null && onShownCheckedChange != null) {
                SelectAllNoneRow(
                    selectAllLabel = selectAllLabel,
                    selectNoneLabel = selectNoneLabel,
                    onCheckedChange = { selected -> onShownCheckedChange(shown.map { it.first }, selected) },
                )
            }
            shown.forEach { (item, title) ->
                DropdownMenuItem(
                    text = {
                        OptionText(title = title, checked = checked?.invoke(item), onCheckedChange = { onClick(item) })
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

/** Options whose title contains [query], ignoring case and surrounding blanks; all of them for a blank query. */
private fun List<Pair<Any, String>>.matching(query: String): List<Pair<Any, String>> {
    val needle = query.trim()
    return if (needle.isEmpty()) this else filter { (_, title) -> title.contains(needle, ignoreCase = true) }
}

@Composable
private fun OptionsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
) {
    TableTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = placeholder, maxLines = 1) },
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Select all / None over the options shown; nothing when either label is missing. */
@Composable
private fun SelectAllNoneRow(
    selectAllLabel: String?,
    selectNoneLabel: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (selectAllLabel == null || selectNoneLabel == null) return
    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
        TextButton(onClick = { onCheckedChange(true) }) {
            Text(selectAllLabel)
        }
        TextButton(onClick = { onCheckedChange(false) }) {
            Text(selectNoneLabel)
        }
    }
}

/** An option's title, behind a checkbox in multi-select mode ([checked] is not null). */
@Composable
private fun OptionText(
    title: String,
    checked: Boolean?,
    onCheckedChange: () -> Unit,
) {
    if (checked == null) {
        Text(title)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
        Text(title)
    }
}
