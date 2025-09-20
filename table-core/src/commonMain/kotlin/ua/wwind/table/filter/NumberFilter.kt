package ua.wwind.table.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun <T : Number> NumberFilter(
    filter: TableFilterType.NumberTableFilter<T>,
    state: TableFilterState<T>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<T>) -> Unit,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    // Use two text states; the first is reused for both single-value and range modes
    val initialSingle = state.values?.firstOrNull()?.toString()
    val initialFrom = state.values?.getOrNull(0)?.toString()
    val initialTo = state.values?.getOrNull(1)?.toString()
    val min = filter.rangeOptions?.first ?: filter.delegate.default
    val max = filter.rangeOptions?.second ?: filter.delegate.default
    var firstText by remember { mutableStateOf(initialSingle ?: initialFrom ?: "") }
    var secondText by remember { mutableStateOf(initialTo ?: "") }
    val fromValue = filter.delegate.parse(firstText) ?: min
    val toValue = filter.delegate.parse(secondText) ?: max
    val isBetween = constraint == FilterConstraint.BETWEEN
    val isRangeValid = filter.delegate.compare(fromValue, toValue)
    if (autoApplyFilters) {
        LaunchedEffect(isBetween) {
            snapshotFlow { firstText to secondText }
                .drop(1)
                .debounce(autoFilterDebounce)
                .distinctUntilChanged()
                .collect { (from, to) ->
                    val fromVal = filter.delegate.parse(from)
                    if (isBetween) {
                        val toVal = filter.delegate.parse(to)
                        if (fromVal != null && toVal != null && filter.delegate.compare(fromVal, toVal)) {
                            onChange(TableFilterState(constraint, listOf(fromVal, toVal)))
                        }
                    } else {
                        onChange(TableFilterState(constraint, fromVal?.let { listOf(it) }))
                    }
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
                val fromVal = filter.delegate.parse(firstText)
                if (it == FilterConstraint.BETWEEN) {
                    val toVal = filter.delegate.parse(secondText)
                    if (fromVal != null && toVal != null && filter.delegate.compare(fromVal, toVal)) {
                        onChange(TableFilterState(it, listOf(fromVal, toVal)))
                    }
                } else {
                    onChange(TableFilterState(it, fromVal?.let { parsed -> listOf(parsed) }))
                }
            }
        },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = firstText,
            onValueChange = {
                if (it.matches(filter.delegate.regex)) {
                    firstText = it
                }
            },
            placeholder = {
                Text(
                    strings.get(
                        if (isBetween) UiString.FilterRangeFromPlaceholder else UiString.FilterEnterNumberPlaceholder,
                    ),
                )
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        if (isBetween) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = strings.get(UiString.FilterRangeIconDescription),
            )
            OutlinedTextField(
                value = secondText,
                onValueChange = {
                    if (it.matches(filter.delegate.regex)) {
                        secondText = it
                    }
                },
                placeholder = { Text(strings.get(UiString.FilterRangeToPlaceholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = !isRangeValid,
            )
        }
    }
    if (isBetween && filter.rangeOptions != null) {
        RangeSlider(
            value = filter.delegate.toSliderValue(fromValue)..filter.delegate.toSliderValue(toValue),
            onValueChange = { range ->
                firstText = filter.delegate.fromSliderValue(range.start).toString()
                secondText = filter.delegate.fromSliderValue(range.endInclusive).toString()
            },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }

    FilterPanelActions(
        autoApplyFilters = autoApplyFilters,
        enabled = isRangeValid,
        onApply = {
            val fromVal = filter.delegate.parse(firstText)
            if (isBetween) {
                val toVal = filter.delegate.parse(secondText)
                if (fromVal != null && toVal != null) {
                    onChange(TableFilterState(constraint, listOf(fromVal, toVal)))
                }
            } else {
                onChange(TableFilterState(constraint, fromVal?.let { listOf(it) }))
            }
        },
        onClear = { onChange(TableFilterState(constraint, null)) },
        onClose = onClose,
        strings = strings,
    )
}
