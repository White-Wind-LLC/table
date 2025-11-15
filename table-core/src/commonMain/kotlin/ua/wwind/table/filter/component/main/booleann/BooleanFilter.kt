package ua.wwind.table.filter.component.main.booleann

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.BooleanType
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.toUiString
import ua.wwind.table.strings.StringProvider

@OptIn(FlowPreview::class)
@Suppress("LongParameterList")
@Composable
internal fun BooleanFilter(
    filter: TableFilterType.BooleanTableFilter,
    state: TableFilterState<Boolean>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<Boolean>) -> Unit,
) {
    check(filter.constraints.size == 1 && filter.constraints.first() == FilterConstraint.EQUALS) {
        "Boolean filter supports only EQUALS constraint"
    }

    val booleanFilterState = rememberBooleanFilterState(
        externalState = state,
        autoApply = autoApplyFilters,
        debounceMs = autoFilterDebounce,
        onStateChange = { filterState ->
            filterState?.let { onChange(it) }
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = booleanFilterState.value == true,
                onClick = { booleanFilterState.onValueChange(true) },
            )
            val title = filter.getTitle?.let { it(BooleanType.TRUE) } ?: strings.get(BooleanType.TRUE.toUiString())
            Text(title)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = booleanFilterState.value == false,
                onClick = { booleanFilterState.onValueChange(false) },
            )
            val title = filter.getTitle?.let { it(BooleanType.FALSE) } ?: strings.get(BooleanType.FALSE.toUiString())
            Text(title)
        }
    }
    FilterPanelActions(
        onClose = onClose,
        onApply = { booleanFilterState.applyFilter() },
        onClear = { booleanFilterState.clearFilter() },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
