package ua.wwind.table.filter.component.main.number

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Number filter component with support for various comparison constraints.
 * Refactored to use NumberFilterState for consistent state management.
 */
@OptIn(FlowPreview::class)
@Suppress("LongParameterList")
@Composable
internal fun <T : Number> NumberFilter(
    filter: TableFilterType.NumberTableFilter<T>,
    state: TableFilterState<T>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<T>?) -> Unit,
) {
    val numberFilterState = rememberNumberFilterState(
        externalState = state,
        filter = filter,
        defaultConstraint = filter.constraints.first(),
        autoApply = autoApplyFilters,
        debounceMs = autoFilterDebounce,
        onStateChange = onChange,
    )

    val isBetween = numberFilterState.constraint == FilterConstraint.BETWEEN
    val min = filter.rangeOptions?.first ?: filter.delegate.default
    val max = filter.rangeOptions?.second ?: filter.delegate.default

    val fromValue = filter.delegate.parse(numberFilterState.text) ?: min
    val toValue = if (isBetween) {
        filter.delegate.parse(numberFilterState.secondText) ?: max
    } else {
        max
    }
    val isRangeValid = !isBetween || filter.delegate.compare(fromValue, toValue)

    FilterDropdownField(
        currentValue = numberFilterState.constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { constraint ->
            numberFilterState.onConstraintChange(constraint)
        },
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = numberFilterState.text,
            onValueChange = numberFilterState.onTextChange,
            placeholder = {
                Text(
                    strings.get(
                        if (isBetween) {
                            UiString.FilterRangeFromPlaceholder
                        } else {
                            UiString.FilterEnterNumberPlaceholder
                        }
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
                value = numberFilterState.secondText,
                onValueChange = numberFilterState.onSecondTextChange,
                placeholder = {
                    Text(strings.get(UiString.FilterRangeToPlaceholder))
                },
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
                val newFrom = filter.delegate.fromSliderValue(range.start)
                val newTo = filter.delegate.fromSliderValue(range.endInclusive)
                numberFilterState.onTextChange(filter.delegate.format(newFrom))
                numberFilterState.onSecondTextChange(filter.delegate.format(newTo))
            },
            valueRange = filter.delegate.toSliderValue(min)..filter.delegate.toSliderValue(max),
        )
    }

    FilterPanelActions(
        autoApplyFilters = autoApplyFilters,
        enabled = isRangeValid,
        onApply = numberFilterState.applyFilter,
        onClear = numberFilterState.clearFilter,
        onClose = onClose,
        strings = strings,
    )
}