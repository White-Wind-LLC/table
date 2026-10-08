package ua.wwind.table.filter.component.main.number

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.component.TableTextField
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.icon.TableIcons
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
    val numberFilterState =
        rememberNumberFilterState(
            externalState = state,
            filter = filter,
            strings = strings,
            defaultConstraint = filter.constraints.first(),
            autoApply = autoApplyFilters,
            debounceMs = autoFilterDebounce,
            onStateChange = onChange,
        )

    val isNullConstraint = numberFilterState.constraint.isNullCheck()
    val isBetween = numberFilterState.constraint == FilterConstraint.BETWEEN
    val min = filter.rangeOptions?.first ?: filter.delegate.default
    val max = filter.rangeOptions?.second ?: filter.delegate.default

    val inputFormat = numberFilterState.inputFormat
    val fromValue = inputFormat.parse(numberFilterState.text) ?: min
    val toValue =
        if (isBetween) {
            inputFormat.parse(numberFilterState.secondText) ?: max
        } else {
            max
        }

    FilterDropdownField(
        currentValue = numberFilterState.constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { constraint ->
            numberFilterState.onConstraintChange(constraint)
        },
        label = strings.get(UiString.FilterConditionLabel),
    )

    if (!isNullConstraint) {
        NumberFields(numberFilterState, isBetween, strings)
    }

    if (!isNullConstraint && isBetween && filter.rangeOptions != null) {
        RangeSlider(
            value = filter.delegate.toSliderValue(fromValue)..filter.delegate.toSliderValue(toValue),
            onValueChange = { range ->
                val newFrom = filter.delegate.fromSliderValue(range.start)
                val newTo = filter.delegate.fromSliderValue(range.endInclusive)
                numberFilterState.onTextChange(inputFormat.format(newFrom))
                numberFilterState.onSecondTextChange(inputFormat.format(newTo))
            },
            valueRange = filter.delegate.toSliderValue(min)..filter.delegate.toSliderValue(max),
        )
    }

    FilterPanelActions(
        autoApplyFilters = autoApplyFilters,
        enabled = !numberFilterState.isError,
        onApply = numberFilterState.applyFilter,
        onClear = numberFilterState.clearFilter,
        onClose = onClose,
        strings = strings,
    )
}

private fun NumberInputError.toUiString(): UiString =
    when (this) {
        NumberInputError.InvalidNumber -> UiString.FilterErrorInvalidNumber
        NumberInputError.RangeIncomplete -> UiString.FilterErrorRangeIncomplete
        NumberInputError.RangeInverted -> UiString.FilterErrorRangeInverted
    }

/** The value field (or From/To pair) with the input error, if any, as one message under them. */
@Composable
private fun <T : Number> NumberFields(
    numberFilterState: NumberFilterState<T>,
    isBetween: Boolean,
    strings: StringProvider,
) {
    val keyboardOptions = KeyboardOptions(keyboardType = numberFilterState.inputFormat.delegate.keyboardType)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableTextField(
                value = numberFilterState.text,
                onValueChange = numberFilterState.onTextChange,
                label = {
                    Text(
                        strings.get(if (isBetween) UiString.FilterRangeFromPlaceholder else UiString.FilterValueLabel),
                        maxLines = 1,
                    )
                },
                placeholder =
                    if (isBetween) {
                        null
                    } else {
                        { Text(strings.get(UiString.FilterEnterNumberPlaceholder), maxLines = 1) }
                    },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = numberFilterState.isError,
                keyboardOptions = keyboardOptions,
            )

            if (isBetween) {
                Icon(
                    painter = painterResource(TableIcons.SwapHoriz),
                    contentDescription = strings.get(UiString.FilterRangeIconDescription),
                )
                TableTextField(
                    value = numberFilterState.secondText,
                    onValueChange = numberFilterState.onSecondTextChange,
                    label = { Text(strings.get(UiString.FilterRangeToPlaceholder), maxLines = 1) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = numberFilterState.isError,
                    keyboardOptions = keyboardOptions,
                )
            }
        }
        // One message under the whole row: a range error belongs to both fields, not one of them.
        numberFilterState.error?.let { error ->
            Text(
                text = strings.get(error.toUiString()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
