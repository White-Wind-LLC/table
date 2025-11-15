package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.component.main.booleann.rememberBooleanFilterState
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> FastBooleanFilter(
    spec: ColumnSpec<T, C>,
    state: TableFilterState<Boolean>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C>, TableFilterState<T>?) -> Unit,
) {
    val booleanFilterState = rememberBooleanFilterState(
        externalState = state,
        autoApply = true,
        debounceMs = autoFilterDebounce,
        onStateChange = { filterState ->
            @Suppress("UNCHECKED_CAST")
            onChange(spec, filterState as? TableFilterState<T>)
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TriStateCheckbox(
            state = when (booleanFilterState.value) {
                null -> ToggleableState.Indeterminate
                true -> ToggleableState.On
                false -> ToggleableState.Off
            },
            onClick = {
                // Cycle through states: null -> true -> false -> null
                val nextValue = when (booleanFilterState.value) {
                    null -> true
                    true -> false
                    false -> null
                }
                booleanFilterState.onValueChange(nextValue)
            }
        )
    }
}