package ua.wwind.table.filter.component.main.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.FlowPreview
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.component.TableTextField
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
    onChange: (TableFilterState<String>?) -> Unit,
) {
    val textFilterState = rememberTextFilterState(
        externalState = state,
        defaultConstraint = filter.constraints.first(),
        autoApply = autoApplyFilters,
        debounceMs = autoFilterDebounce,
        onStateChange = onChange,
    )
    FilterDropdownField(
        currentValue = textFilterState.constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { textFilterState.onConstraintChange(it) },
    )
    TableTextField(
        value = textFilterState.text,
        onValueChange = { textFilterState.onTextChange(it) },
        placeholder = { Text(strings.get(UiString.FilterSearchPlaceholder)) },
        singleLine = true,
    )
    FilterPanelActions(
        onClose = onClose,
        onApply = { textFilterState.applyFilter() },
        onClear = { textFilterState.clearFilter() },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
