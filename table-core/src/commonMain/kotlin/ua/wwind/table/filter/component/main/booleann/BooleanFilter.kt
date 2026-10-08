package ua.wwind.table.filter.component.main.booleann

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.BooleanType
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.toUiString
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

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
    onChange: (TableFilterState<Boolean>?) -> Unit,
) {
    check(filter.constraints.size == 1 && filter.constraints.first() == FilterConstraint.EQUALS) {
        "Boolean filter supports only EQUALS constraint"
    }

    val booleanFilterState =
        rememberBooleanFilterState(
            externalState = state,
            autoApply = autoApplyFilters,
            debounceMs = autoFilterDebounce,
            onStateChange = onChange,
        )

    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        BooleanOption(
            title = strings.get(UiString.BooleanAnyTitle),
            selected = booleanFilterState.value == null,
            onClick = { booleanFilterState.onValueChange(null) },
        )
        BooleanOption(
            title = filter.getTitle?.let { it(BooleanType.TRUE) } ?: strings.get(BooleanType.TRUE.toUiString()),
            selected = booleanFilterState.value == true,
            onClick = { booleanFilterState.onValueChange(true) },
        )
        BooleanOption(
            title = filter.getTitle?.let { it(BooleanType.FALSE) } ?: strings.get(BooleanType.FALSE.toUiString()),
            selected = booleanFilterState.value == false,
            onClick = { booleanFilterState.onValueChange(false) },
        )
    }
    FilterPanelActions(
        onClose = onClose,
        onApply = { booleanFilterState.applyFilter() },
        onClear = { booleanFilterState.clearFilter() },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}

/** One radio choice of the boolean filter; the whole row is the click target. */
@Composable
private fun RowScope.BooleanOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title)
    }
}
