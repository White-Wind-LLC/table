package ua.wwind.table.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import ua.wwind.table.filter.component.FilterDropdownAnyField
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "UNCHECKED_CAST", "CyclomaticComplexMethod")
@Composable
internal fun EnumFilter(
    filter: TableFilterType.EnumTableFilter<*>,
    state: TableFilterState<*>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<*>) -> Unit,
) {
    var constraint by remember { mutableStateOf(state.constraint ?: filter.constraints.first()) }
    var values by remember { mutableStateOf((state.values as? List<Enum<*>>).orEmpty()) }
    if (autoApplyFilters) {
        LaunchedEffect(Unit) {
            snapshotFlow { values }
                .drop(1)
                .debounce(autoFilterDebounce)
                .distinctUntilChanged()
                .collect {
                    onChange(TableFilterState(constraint, values.takeIf { it.isNotEmpty() }))
                }
        }
    }
    FilterDropdownField(
        currentValue = constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { filterConstraint ->
            constraint = filterConstraint
            when (filterConstraint) {
                FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {
                    if (values.isNotEmpty()) values = emptyList()
                }
                FilterConstraint.EQUALS -> {
                    if (values.size > 1) values = values.take(1)
                }
                else -> { /* NoOp */ }
            }
            if (autoApplyFilters) {
                onChange(TableFilterState<Any?>(filterConstraint, values))
            }
        },
    )
    when (constraint) {
        FilterConstraint.EQUALS -> {
            FilterDropdownAnyField(
                currentValue = if (values.size == 1) values.first() else null,
                getTitle = { item -> (filter.getTitle as @Composable (Enum<*>) -> String).invoke(item as Enum<*>) },
                placeholder = strings.get(UiString.FilterSelectOnePlaceholder),
                values = filter.options,
                onClick = { item -> values = listOf(item as Enum<*>) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FilterConstraint.IN, FilterConstraint.NOT_IN -> {
            FilterDropdownAnyField(
                currentValue = null,
                getTitle = { anyItem -> (filter.getTitle as @Composable (Enum<*>) -> String).invoke(anyItem as Enum<*>) },
                placeholder =
                    if (values.isEmpty()) {
                        strings.get(UiString.FilterSelectManyPlaceholder)
                    } else {
                        val titles =
                            values.map { item ->
                                (filter.getTitle as @Composable (Enum<*>) -> String).invoke(item)
                            }
                        titles.joinToString(", ")
                    },
                values = filter.options,
                onClick = { anyItem ->
                    val enumItem = anyItem as Enum<*>
                    values = if (values.contains(enumItem)) values - enumItem else values + enumItem
                },
                checked = { enum -> values.contains(enum as Enum<*>) },
            )
        }

        FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {}

        else -> error("Unsupported constraint for list filter: $constraint")
    }
    FilterPanelActions(
        onClose = onClose,
        onApply = { onChange(TableFilterState<Any?>(constraint, values.takeIf { it.isNotEmpty() } as List<Any?>?)) },
        onClear = { onChange(TableFilterState<Any?>(constraint, null)) },
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
