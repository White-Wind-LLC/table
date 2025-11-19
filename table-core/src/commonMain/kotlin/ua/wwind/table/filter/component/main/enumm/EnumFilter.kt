package ua.wwind.table.filter.component.main.enumm

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.FlowPreview
import ua.wwind.table.filter.component.FilterDropdownAnyField
import ua.wwind.table.filter.component.FilterDropdownField
import ua.wwind.table.filter.component.FilterPanelActions
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Enum filter component with support for single and multiple selection.
 * Refactored to use EnumFilterState for consistent state management.
 */
@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "UNCHECKED_CAST")
@Composable
internal fun <E : Enum<E>> EnumFilter(
    filter: TableFilterType.EnumTableFilter<E>,
    state: TableFilterState<*>,
    onClose: () -> Unit,
    strings: StringProvider,
    autoApplyFilters: Boolean,
    autoFilterDebounce: Long,
    onChange: (TableFilterState<*>?) -> Unit,
) {
    val enumFilterState =
        rememberEnumFilterState<E>(
            externalState = state,
            defaultConstraint = filter.constraints.first(),
            autoApply = autoApplyFilters,
            debounceMs = autoFilterDebounce,
            onStateChange = onChange,
        )

    FilterDropdownField(
        currentValue = enumFilterState.constraint,
        getTitle = { c -> strings.get(c.toUiString()) },
        values = filter.constraints,
        onClick = { filterConstraint ->
            enumFilterState.onConstraintChange(filterConstraint)
        },
    )

    when (enumFilterState.constraint) {
        FilterConstraint.EQUALS -> {
            FilterDropdownAnyField(
                currentValue = enumFilterState.selectedValues.firstOrNull(),
                getTitle = { item ->
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

        FilterConstraint.IN, FilterConstraint.NOT_IN -> {
            FilterDropdownAnyField(
                currentValue = null,
                getTitle = { anyItem ->
                    (filter.getTitle as @Composable (Enum<*>) -> String).invoke(anyItem as Enum<*>)
                },
                placeholder =
                    if (enumFilterState.selectedValues.isEmpty()) {
                        strings.get(UiString.FilterSelectManyPlaceholder)
                    } else {
                        val titles =
                            enumFilterState.selectedValues.map { item ->
                                (filter.getTitle as @Composable (Enum<*>) -> String).invoke(item)
                            }
                        titles.joinToString(", ")
                    },
                values = filter.options,
                onClick = { anyItem ->
                    enumFilterState.onMultiValueToggle(anyItem as E)
                },
                checked = { item ->
                    enumFilterState.selectedValues.contains(item as E)
                },
            )
        }

        FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {
            // No value selection needed for null checks
        }

        else -> error("Unsupported constraint for enum filter: ${enumFilterState.constraint}")
    }

    FilterPanelActions(
        onClose = onClose,
        onApply = enumFilterState.applyFilter,
        onClear = enumFilterState.clearFilter,
        autoApplyFilters = autoApplyFilters,
        strings = strings,
    )
}
