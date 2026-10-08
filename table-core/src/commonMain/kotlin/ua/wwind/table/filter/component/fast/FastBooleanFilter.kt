package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.component.main.booleann.rememberBooleanFilterState
import ua.wwind.table.filter.data.BooleanType
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.toUiString
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Tri-state checkbox cycling Any → Yes → No. The indeterminate box means "Any", so the current
 * choice is named in a tooltip and as the state description read by screen readers (issue #105).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : Any, C, E> FastBooleanFilter(
    spec: ColumnSpec<T, C, E>,
    state: TableFilterState<Boolean>?,
    autoFilterDebounce: Long,
    strings: StringProvider,
    onChange: (ColumnSpec<T, C, E>, TableFilterState<T>?) -> Unit,
) {
    val filter = spec.filter as TableFilterType.BooleanTableFilter
    val booleanFilterState =
        rememberBooleanFilterState(
            externalState = state,
            autoApply = true,
            debounceMs = autoFilterDebounce,
            onStateChange = { filterState ->
                @Suppress("UNCHECKED_CAST")
                onChange(spec, filterState as? TableFilterState<T>)
            },
        )
    val title =
        when (val value = booleanFilterState.value) {
            null -> {
                strings.get(UiString.BooleanAnyTitle)
            }

            else -> {
                val type = if (value) BooleanType.TRUE else BooleanType.FALSE
                filter.getTitle?.invoke(type) ?: strings.get(type.toUiString())
            }
        }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            state = rememberTooltipState(isPersistent = false),
            focusable = false,
            enableUserInput = true,
            tooltip = { PlainTooltip { Text(title) } },
        ) {
            TriStateCheckbox(
                state =
                    when (booleanFilterState.value) {
                        null -> ToggleableState.Indeterminate
                        true -> ToggleableState.On
                        false -> ToggleableState.Off
                    },
                onClick = {
                    // Cycle through states: null -> true -> false -> null
                    val nextValue =
                        when (booleanFilterState.value) {
                            null -> true
                            true -> false
                            false -> null
                        }
                    booleanFilterState.onValueChange(nextValue)
                },
                modifier = Modifier.semantics { stateDescription = title },
            )
        }
    }
}
