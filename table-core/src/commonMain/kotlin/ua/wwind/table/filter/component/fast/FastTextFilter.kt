package ua.wwind.table.filter.component.fast

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.main.text.rememberTextFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun <T : Any, C, E> FastTextFilter(
    spec: ColumnSpec<T, C, E>,
    state: TableFilterState<String>,
    autoFilterDebounce: Long,
    strings: StringProvider,
    clearTargetSize: Dp,
    onChange: (ColumnSpec<T, C, E>, TableFilterState<T>?) -> Unit,
) {
    val textFilterState =
        rememberTextFilterState(
            externalState = state,
            defaultConstraint = FilterConstraint.CONTAINS,
            autoApply = true,
            isFastFilter = true,
            debounceMs = autoFilterDebounce,
            onStateChange = { filterState ->
                @Suppress("UNCHECKED_CAST")
                onChange(spec, filterState as? TableFilterState<T>)
            },
        )

    val filter = spec.filter as TableFilterType.TextTableFilter
    val mode = fastFilterMode(filter, state)
    if (mode == FastFilterMode.Locked) {
        FastLockedField(
            type = filter,
            state = state,
            strings = strings,
            clearTargetSize = clearTargetSize,
            onClear = textFilterState.clearFilter,
        )
        return
    }

    FastFieldFrame(
        mode = mode,
        showClear = textFilterState.text.isNotEmpty(),
        strings = strings,
        clearTargetSize = clearTargetSize,
        onClear = textFilterState.clearFilter,
    ) {
        TableTextField(
            value = textFilterState.text,
            onValueChange = { textFilterState.onTextChange(it) },
            placeholder = {
                Text(
                    text = strings.get(UiString.FilterSearchPlaceholder),
                    maxLines = 1,
                )
            },
            singleLine = true,
            contentPadding = TableTextFieldDefaults.reducedContentPadding(),
            showBorder = false,
        )
    }
}
