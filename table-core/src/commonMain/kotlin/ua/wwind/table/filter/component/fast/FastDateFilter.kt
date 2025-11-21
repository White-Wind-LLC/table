package ua.wwind.table.filter.component.fast

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.main.date.DateField
import ua.wwind.table.filter.component.main.date.rememberDateFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C, E> FastDateFilter(
    spec: ColumnSpec<T, C, E>,
    state: TableFilterState<LocalDate>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C, E>, TableFilterState<T>?) -> Unit,
) {
    val dateFilterState =
        rememberDateFilterState(
            externalState = state,
            defaultConstraint = FilterConstraint.EQUALS,
            autoApply = true,
            isFastFilter = true,
            debounceMs = autoFilterDebounce,
            onStateChange = { filterState ->
                @Suppress("UNCHECKED_CAST")
                onChange(spec, filterState as? TableFilterState<T>)
            },
        )

    DateField(
        value = dateFilterState.firstDate,
        onDateSelected = { selectedDate ->
            dateFilterState.onFirstDateChange(selectedDate)
        },
        onClear = {
            dateFilterState.clearFilter()
        },
        strings = strings,
        contentPadding = TableTextFieldDefaults.reducedContentPadding(),
        showBorder = false,
    )
}
