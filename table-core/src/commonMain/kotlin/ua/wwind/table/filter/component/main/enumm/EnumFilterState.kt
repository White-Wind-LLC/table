package ua.wwind.table.filter.component.main.enumm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isNullCheck

/**
 * State holder for enum filter components.
 * Manages single or multiple selection based on constraint.
 */
@Immutable
internal data class EnumFilterState<T : Enum<T>>(
    val selectedValues: List<T>,
    val constraint: FilterConstraint,
    val isEditing: Boolean,
    val onSingleValueChange: (T?) -> Unit,
    val onMultiValueToggle: (T) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
)

/**
 * Shared state management for enum filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - Support for single selection (EQUALS constraint) and multiple selection (IN/NOT_IN)
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param defaultConstraint Default constraint to use (EQUALS for fast filters)
 * @param autoApply Whether to apply changes automatically with debounce
 * @param debounceMs Debounce delay in milliseconds
 * @param onStateChange Callback when filter state changes
 * @return EnumFilterState containing current values and update functions
 */
@OptIn(FlowPreview::class)
@Composable
internal fun <T : Enum<T>> rememberEnumFilterState(
    externalState: TableFilterState<*>?,
    defaultConstraint: FilterConstraint = FilterConstraint.EQUALS,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    isFastFilter: Boolean = false,
    onStateChange: (TableFilterState<*>?) -> Unit,
): EnumFilterState<T> {
    val sourceValues by remember(externalState) {
        derivedStateOf {
            @Suppress("UNCHECKED_CAST")
            (externalState?.values as? List<T>).orEmpty()
        }
    }

    val sourceConstraint by remember(externalState, defaultConstraint) {
        derivedStateOf {
            if (isFastFilter) defaultConstraint else externalState?.constraint ?: defaultConstraint
        }
    }

    var editingValues by remember { mutableStateOf(sourceValues) }
    var editingConstraint by remember { mutableStateOf(sourceConstraint) }
    var isEditing by remember { mutableStateOf(false) }

    val currentOnStateChange = rememberUpdatedState(onStateChange)

    LaunchedEffect(sourceValues, sourceConstraint) {
        if (!isEditing) {
            editingValues = sourceValues
            editingConstraint = sourceConstraint
        }
    }

    if (autoApply) {
        LaunchedEffect(editingValues, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                isEditing = false
                emitDebouncedFilter(editingValues, editingConstraint, currentOnStateChange.value)
            }
        }
    }

    return remember(editingValues, editingConstraint, isEditing) {
        EnumFilterState(
            selectedValues = editingValues,
            constraint = editingConstraint,
            isEditing = isEditing,
            onSingleValueChange = { newValue ->
                editingValues = if (newValue != null) listOf(newValue) else emptyList()
                isEditing = true
            },
            onMultiValueToggle = { item ->
                editingValues =
                    if (editingValues.contains(item)) {
                        editingValues - item
                    } else {
                        editingValues + item
                    }
                isEditing = true
            },
            onConstraintChange = { newConstraint ->
                editingConstraint = newConstraint
                if (newConstraint == FilterConstraint.EQUALS && editingValues.size > 1) {
                    editingValues = editingValues.take(1)
                }
                if (newConstraint == FilterConstraint.IS_NULL || newConstraint == FilterConstraint.IS_NOT_NULL) {
                    editingValues = emptyList()
                }
                isEditing = true
            },
            applyFilter = {
                emitAppliedFilter(editingValues, editingConstraint, currentOnStateChange.value)
                isEditing = false
            },
            clearFilter = {
                editingValues = emptyList()
                currentOnStateChange.value(null)
                isEditing = false
            },
        )
    }
}

/**
 * Emits the filter the debounced auto-apply path settles on.
 *
 * Deliberately kept separate from [emitAppliedFilter] rather than merged: the two paths do not agree
 * today, and reconciling them is a behaviour change rather than a refactor. This one clears the
 * filter on an empty selection whatever the constraint, so IS_NULL/IS_NOT_NULL never survive it.
 */
private fun <T : Enum<T>> emitDebouncedFilter(
    editingValues: List<T>,
    constraint: FilterConstraint,
    onStateChange: (TableFilterState<*>?) -> Unit,
) {
    if (editingValues.isEmpty()) {
        onStateChange(null)
    } else {
        onStateChange(TableFilterState(constraint, editingValues))
    }
}

/**
 * Emits the filter an explicit Apply settles on.
 *
 * See [emitDebouncedFilter] for why the two are not one function: this one exempts
 * IS_NULL/IS_NOT_NULL from the empty-selection clear, since those constraints carry no values.
 */
private fun <T : Enum<T>> emitAppliedFilter(
    editingValues: List<T>,
    constraint: FilterConstraint,
    onStateChange: (TableFilterState<*>?) -> Unit,
) {
    if (editingValues.isEmpty() && !constraint.isNullCheck()) {
        onStateChange(null)
        return
    }
    val valuesToSend =
        when (constraint) {
            FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> null
            else -> editingValues.takeIf { it.isNotEmpty() }
        }
    onStateChange(TableFilterState(constraint, valuesToSend))
}
