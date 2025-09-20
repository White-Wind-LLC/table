package ua.wwind.table.filter

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@OptIn(FlowPreview::class)
@Suppress("LongParameterList")
@Composable
internal fun TextFilter(
    filter: TableFilterType.TextTableFilter,
    state: TableFilterState<String>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<String>) -> Unit,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    var searchText by remember { mutableStateOf(state.values?.firstOrNull() ?: "") }
    if (autoApplyFilters) {
        LaunchedEffect(Unit) {
            snapshotFlow { searchText }
                .drop(1)
                .debounce(autoFilterDebounce)
                .distinctUntilChanged()
                .collect {
                    onChange(TableFilterState(constraint, listOf(searchText)))
                }
        }
    }
    FilterDropdownField(
        currentValue = constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = {
            constraint = it
            if (autoApplyFilters) {
                onChange(TableFilterState(it, listOf(searchText)))
            }
        },
    )
    OutlinedTextField(
        value = searchText,
        onValueChange = {
            searchText = it
        },
        placeholder = { Text(strings.get(UiString.FilterSearchPlaceholder)) },
        singleLine = true,
    )
    FilterPanelActions(
        onClose = onClose,
        onApply = { onChange(TableFilterState(constraint, listOf(searchText))) },
        onClear = { onChange(TableFilterState(constraint, null)) },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
