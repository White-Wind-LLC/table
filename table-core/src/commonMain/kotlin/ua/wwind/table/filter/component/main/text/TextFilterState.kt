package ua.wwind.table.filter.component.main.text

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
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.applyEmission
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isNullCheck

/**
 * State holder for text filter components
 */
@Immutable
internal data class TextFilterState(
    val text: String,
    val constraint: FilterConstraint,
    val isEditing: Boolean,
    val onTextChange: (String) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
)

/**
 * Shared state management for text filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - No input loss during typing
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param defaultConstraint Default constraint to use if not specified in state
 * @param autoApply Whether to apply changes automatically with debounce
 * @param debounceMs Debounce delay in milliseconds
 * @param onStateChange Callback when filter state changes
 * @return TextFilterState containing current values and update functions
 */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberTextFilterState(
    externalState: TableFilterState<String>?,
    defaultConstraint: FilterConstraint = FilterConstraint.CONTAINS,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    isFastFilter: Boolean = false,
    onStateChange: (TableFilterState<String>?) -> Unit,
): TextFilterState {
    val sourceText by remember(externalState) {
        derivedStateOf {
            externalState?.values?.firstOrNull().orEmpty()
        }
    }

    val sourceConstraint by remember(externalState, defaultConstraint) {
        derivedStateOf {
            if (isFastFilter) defaultConstraint else externalState?.constraint ?: defaultConstraint
        }
    }

    var editingText by remember { mutableStateOf(sourceText) }
    var editingConstraint by remember { mutableStateOf(sourceConstraint) }
    var isEditing by remember { mutableStateOf(false) }

    val currentOnStateChange = rememberUpdatedState(onStateChange)

    LaunchedEffect(sourceText, sourceConstraint) {
        if (!isEditing) {
            editingText = sourceText
            editingConstraint = sourceConstraint
        }
    }

    if (autoApply) {
        LaunchedEffect(editingText, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                isEditing = false
                applyEmission(resolveTextFilter(editingText, editingConstraint), currentOnStateChange.value)
            }
        }
    }

    return remember(editingText, editingConstraint, isEditing) {
        TextFilterState(
            text = editingText,
            constraint = editingConstraint,
            isEditing = isEditing,
            onTextChange = { newText ->
                editingText = newText
                isEditing = true
            },
            onConstraintChange = { newConstraint ->
                editingConstraint = newConstraint
                isEditing = true
            },
            applyFilter = {
                applyEmission(resolveTextFilter(editingText, editingConstraint), currentOnStateChange.value)
                isEditing = false
            },
            clearFilter = {
                editingText = ""
                currentOnStateChange.value(null)
                isEditing = false
            },
        )
    }
}

/**
 * Resolves the current text-filter input into the single [FilterEmission] that both the debounced
 * auto-apply path and the explicit Apply path act on, so the two can never disagree (issue #55).
 *
 * Any non-blank text applies the filter; IS_NULL/IS_NOT_NULL apply with no value; blank text clears.
 */
internal fun resolveTextFilter(
    text: String,
    constraint: FilterConstraint,
): FilterEmission<String> {
    val isNullConstraint = constraint.isNullCheck()
    return when {
        text.isBlank() && !isNullConstraint -> FilterEmission.Clear
        isNullConstraint -> FilterEmission.Apply(TableFilterState(constraint, emptyList()))
        else -> FilterEmission.Apply(TableFilterState(constraint, listOf(text)))
    }
}
