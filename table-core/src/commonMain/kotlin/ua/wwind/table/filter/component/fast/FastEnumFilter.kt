package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.FilterDropdownAnyField
import ua.wwind.table.filter.component.main.enumm.rememberEnumFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T : Any, C, E, ENUM : Enum<ENUM>> FastEnumFilter(
    spec: ColumnSpec<T, C, E>,
    state: TableFilterState<*>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    clearTargetSize: Dp,
    onChange: (ColumnSpec<T, C, E>, TableFilterState<T>?) -> Unit,
) {
    val filter = spec.filter as TableFilterType.EnumTableFilter<ENUM>

    val enumFilterState =
        rememberEnumFilterState<ENUM>(
            externalState = state,
            defaultConstraint = FilterConstraint.EQUALS,
            autoApply = true,
            isFastFilter = true,
            debounceMs = autoFilterDebounce,
            onStateChange = { filterState ->
                onChange(spec, filterState as? TableFilterState<T>)
            },
        )

    val mode = fastFilterMode(filter, state)
    if (mode == FastFilterMode.Locked && state != null) {
        FastLockedField(
            type = filter,
            state = state,
            strings = strings,
            clearTargetSize = clearTargetSize,
            onClear = enumFilterState.clearFilter,
        )
        return
    }

    FastFieldFrame(
        mode = mode,
        showClear = enumFilterState.selectedValues.isNotEmpty(),
        strings = strings,
        clearTargetSize = clearTargetSize,
        onClear = enumFilterState.clearFilter,
    ) {
        FilterDropdownAnyField(
            currentValue = enumFilterState.selectedValues.firstOrNull(),
            getTitle = { item ->
                @Suppress("UNCHECKED_CAST")
                (filter.getTitle as @Composable (Enum<*>) -> String).invoke(item as Enum<*>)
            },
            placeholder = strings.get(UiString.FilterSelectOnePlaceholder),
            values = filter.options,
            onClick = { item ->
                enumFilterState.onSingleValueChange(item as? ENUM)
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = TableTextFieldDefaults.reducedContentPadding(),
            showBorder = false,
            searchPlaceholder = strings.get(UiString.FilterOptionsSearchPlaceholder),
        )
    }
}
