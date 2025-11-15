package ua.wwind.table.filter.component.main.booleann

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState

/**
 * Represents the state of a boolean filter with three possible states:
 * - null: no filter (indeterminate state)
 * - true: filter for true values
 * - false: filter for false values
 */
@Immutable
internal data class BooleanFilterState(
    val value: Boolean?,
    val isEditing: Boolean,
    val onValueChange: (Boolean?) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit
)

/**
 * Shared state management for boolean filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - Three-state checkbox support (null/true/false)
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param autoApply Whether to apply changes automatically with debounce
 * @param debounceMs Debounce delay in milliseconds
 * @param onStateChange Callback when filter state changes
 * @return BooleanFilterState containing current values and update functions
 */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberBooleanFilterState(
    externalState: TableFilterState<Boolean>?,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    onStateChange: (TableFilterState<Boolean>?) -> Unit
): BooleanFilterState {
    val sourceValue by remember(externalState) {
        derivedStateOf {
            externalState?.values?.firstOrNull()
        }
    }

    var editingValue by remember { mutableStateOf(sourceValue) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(sourceValue) {
        if (!isEditing) {
            editingValue = sourceValue
        }
    }

    if (autoApply) {
        LaunchedEffect(editingValue) {
            if (isEditing) {
                delay(debounceMs)
                isEditing = false

                if (editingValue == null) {
                    onStateChange(null)
                } else {
                    onStateChange(TableFilterState(FilterConstraint.EQUALS, listOf(editingValue!!)))
                }
            }
        }
    }

    return remember(editingValue, isEditing) {
        BooleanFilterState(
            value = editingValue,
            isEditing = isEditing,
            onValueChange = { newValue ->
                editingValue = newValue
                isEditing = true
            },
            applyFilter = {
                if (editingValue == null) {
                    onStateChange(null)
                } else {
                    onStateChange(TableFilterState(FilterConstraint.EQUALS, listOf(editingValue!!)))
                }
                isEditing = false
            },
            clearFilter = {
                editingValue = null
                onStateChange(null)
                isEditing = false
            }
        )
    }
}