package ua.wwind.table.filter.component.main.date

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.collectAsEffect
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.time.ExperimentalTime

@OptIn(FlowPreview::class, ExperimentalTime::class)
@Suppress("LongParameterList")
@Composable
internal fun DateFilter(
    filter: TableFilterType.DateTableFilter,
    state: TableFilterState<LocalDate>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<LocalDate>?) -> Unit,
) {
    val dateFilterState = rememberDateFilterState(
        externalState = state,
        defaultConstraint = filter.constraints.first(),
        autoApply = autoApplyFilters,
        debounceMs = autoFilterDebounce,
        onStateChange = onChange,
    )

    val isBetween = dateFilterState.constraint == FilterConstraint.BETWEEN

    FilterDropdownField(
        currentValue = dateFilterState.constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { dateFilterState.onConstraintChange(it) },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateField(
            value = dateFilterState.firstDate,
            onDateSelected = { selected ->
                dateFilterState.onFirstDateChange(selected)
            },
            modifier = Modifier.weight(1f),
            label = { Text(strings.get(UiString.FilterRangeFromPlaceholder)) },
            onClear = { dateFilterState.onFirstDateChange(null) },
            strings = strings,
        )

        if (isBetween) {
            DateField(
                value = dateFilterState.secondDate,
                onDateSelected = { selected ->
                    dateFilterState.onSecondDateChange(selected)
                },
                modifier = Modifier.weight(1f),
                label = { Text(strings.get(UiString.FilterRangeToPlaceholder)) },
                onClear = { dateFilterState.onSecondDateChange(null) },
                strings = strings,
            )
        }
    }

    FilterPanelActions(
        onClose = onClose,
        onApply = { dateFilterState.applyFilter() },
        onClear = { dateFilterState.clearFilter() },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun DateField(
    value: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    onClear: () -> Unit = {},
    dateValidator: (Long) -> Boolean = { true },
    strings: StringProvider,
    contentPadding: PaddingValues = TableTextFieldDefaults.contentPadding(),
) {
    var showDatePickerDialog by remember {
        mutableStateOf(false)
    }

    val interactionSource = remember { MutableInteractionSource() }
    interactionSource.interactions.collectAsEffect {
        if (it is PressInteraction.Release) showDatePickerDialog = true
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value?.atStartOfDayIn(TimeZone.UTC)?.epochSeconds?.times(1000),
        yearRange = IntRange(1, 2100),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return dateValidator(utcTimeMillis)
            }
        },
    )

    TableTextField(
        value = value?.toFormatString().orEmpty(),
        onValueChange = {},
        placeholder = label ?: {
            Text(text = strings.get(UiString.DatePickerSelectDate), maxLines = 1)
        },
        readOnly = true,
        interactionSource = interactionSource,
        modifier = modifier,
        singleLine = true,
        contentPadding = contentPadding,
    )

    if (showDatePickerDialog) {
        val confirmEnabled by remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
        DatePickerDialog(
            onDismissRequest = {
                showDatePickerDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(
                                kotlin.time.Instant.fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.currentSystemDefault()).date,
                            )
                        }
                    },
                    enabled = confirmEnabled,
                ) {
                    Text(strings.get(UiString.DatePickerConfirm))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                            onClear()
                        },
                    ) {
                        Text(strings.get(UiString.DatePickerClear))
                    }
                    TextButton(
                        onClick = {
                            showDatePickerDialog = false
                        },
                    ) {
                        Text(strings.get(UiString.DatePickerCancel))
                    }
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

internal fun LocalDate.toFormatString(): String {
    return this.format(
        LocalDate.Format {
            dayOfMonth()
            chars(".")
            monthNumber()
            chars(".")
            year()
        }
    )
}
