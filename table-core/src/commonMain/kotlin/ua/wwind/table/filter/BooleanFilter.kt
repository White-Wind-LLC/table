package ua.wwind.table.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
    var valueState by remember { mutableStateOf(state.values?.firstOrNull() ?: false) }
    if (autoApplyFilters) {
        LaunchedEffect(Unit) {
            snapshotFlow { valueState }
                .drop(1)
                .debounce(autoFilterDebounce)
                .distinctUntilChanged()
                .collect {
                    onChange(TableFilterState(filter.constraints.first(), listOf(valueState)))
                }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = valueState,
                onClick = { valueState = true },
            )
            val title = filter.getTitle?.let { it(BooleanType.TRUE) } ?: strings.get(BooleanType.TRUE.toUiString())
            Text(title)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = !valueState,
                onClick = { valueState = false },
            )
            val title = filter.getTitle?.let { it(BooleanType.FALSE) } ?: strings.get(BooleanType.FALSE.toUiString())
            Text(title)
        }
    }
    FilterPanelActions(
        onClose = onClose,
        onApply = { onChange(TableFilterState(filter.constraints.first(), listOf(valueState))) },
        onClear = { onChange(TableFilterState(filter.constraints.first(), null)) },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
