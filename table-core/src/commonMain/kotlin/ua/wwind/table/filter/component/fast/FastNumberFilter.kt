package ua.wwind.table.filter.component.fast

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.component.main.number.rememberNumberFilterState
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Fast number filter component for quick numeric filtering.
 * Supports only EQUALS constraint for fast filtering with a single input field.
 *
 * @param spec Column specification containing the number filter type
 * @param state Current filter state
 * @param autoFilterDebounce Debounce delay for auto-apply in milliseconds
 * @param onChange Callback when filter state changes
 */
@Composable
internal fun <T : Any, C> FastNumberFilter(
    spec: ColumnSpec<T, C>,
    state: TableFilterState<*>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    val filter = spec.filter as TableFilterType.NumberTableFilter<Number>

    @Suppress("UNCHECKED_CAST")
    val numberFilterState =
        rememberNumberFilterState(
            externalState = state as? TableFilterState<Number>,
            filter = filter,
            defaultConstraint = FilterConstraint.EQUALS,
            autoApply = true,
            isFastFilter = true,
            debounceMs = autoFilterDebounce,
            onStateChange = { filterState ->
                @Suppress("UNCHECKED_CAST")
                onChange(spec, filterState as? TableFilterState<T>)
            },
        )

    TableTextField(
        value = numberFilterState.text,
        onValueChange = numberFilterState.onTextChange,
        placeholder = {
            Text(
                text = strings.get(UiString.FilterEnterNumberPlaceholder),
                maxLines = 1,
            )
        },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        singleLine = true,
        contentPadding = TableTextFieldDefaults.reducedContentPadding(),
        showBorder = false,
    )
}
