package ua.wwind.table.filter.component.fast

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.main.text.rememberTextFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun <T : Any, C> FastTextFilter(
    spec: ColumnSpec<T, C>,
    state: TableFilterState<String>,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
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
