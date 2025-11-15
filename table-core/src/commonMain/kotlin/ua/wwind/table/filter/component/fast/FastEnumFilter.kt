package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.component.FilterDropdownAnyField
import ua.wwind.table.filter.component.main.enumm.rememberEnumFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T : Any, C, E : Enum<E>> FastEnumFilter(
    spec: ColumnSpec<T, C>,
    state: TableFilterState<*>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
) {
    val filter = spec.filter as TableFilterType.EnumTableFilter<E>

    val enumFilterState = rememberEnumFilterState<E>(
        externalState = state,
        defaultConstraint = FilterConstraint.EQUALS,
        autoApply = true,
        isFastFilter = true,
        debounceMs = autoFilterDebounce,
        onStateChange = { filterState ->
            onChange(spec, filterState as? TableFilterState<T>)
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
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
                enumFilterState.onSingleValueChange(item as? E)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}