package ua.wwind.table.filter.component.main.number

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
import ua.wwind.table.filter.data.TableFilterType

/**
 * State holder for number filter components.
 * Manages value input, constraint selection, and validation.
 */
@Immutable
internal data class NumberFilterState<T : Number>(
    val text: String,
    val secondText: String,
    val constraint: FilterConstraint,
    val isEditing: Boolean,
    val onTextChange: (String) -> Unit,
    val onSecondTextChange: (String) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
    val delegate: TableFilterType.NumberTableFilter.NumberFilterDelegate<T>
)

/**
 * Shared state management for number filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - Input validation using delegate regex
 * - Support for single value (EQUALS) and range (BETWEEN) constraints
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param filter The number filter configuration with delegate and constraints
 * @param defaultConstraint Default constraint to use if not specified in state
 * @param autoApply Whether to apply changes automatically with debounce
 * @param debounceMs Debounce delay in milliseconds
 * @param onStateChange Callback when filter state changes
 * @return NumberFilterState containing current values and update functions
 */
@OptIn(FlowPreview::class)
@Composable
internal fun <T : Number> rememberNumberFilterState(
    externalState: TableFilterState<T>?,
    filter: TableFilterType.NumberTableFilter<T>,
    defaultConstraint: FilterConstraint = FilterConstraint.EQUALS,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    isFastFilter: Boolean = false,
    onStateChange: (TableFilterState<T>?) -> Unit
): NumberFilterState<T> {
    // Derived state from external source
    val sourceText by remember(externalState) {
        derivedStateOf {
            externalState?.values?.firstOrNull()?.toString() ?: ""
        }
    }

    val sourceSecondText by remember(externalState) {
        derivedStateOf {
            if (externalState?.constraint == FilterConstraint.BETWEEN) {
                externalState.values?.getOrNull(1)?.toString() ?: ""
            } else {
                ""
            }
        }
    }

    val sourceConstraint by remember(externalState, defaultConstraint) {
        derivedStateOf {
            if (isFastFilter) defaultConstraint else externalState?.constraint ?: defaultConstraint
        }
    }

    var editingText by remember { mutableStateOf(sourceText) }
    var editingSecondText by remember { mutableStateOf(sourceSecondText) }
    var editingConstraint by remember { mutableStateOf(sourceConstraint) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(sourceText, sourceSecondText, sourceConstraint) {
        if (!isEditing) {
            editingText = sourceText
            editingSecondText = sourceSecondText
            editingConstraint = sourceConstraint
        }
    }

    if (autoApply) {
        LaunchedEffect(editingText, editingSecondText, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                isEditing = false

                val firstValue = filter.delegate.parse(editingText)

                when (editingConstraint) {
                    FilterConstraint.BETWEEN -> {
                        val secondValue = filter.delegate.parse(editingSecondText)
                        if (firstValue != null && secondValue != null &&
                            filter.delegate.compare(firstValue, secondValue)
                        ) {
                            onStateChange(TableFilterState(editingConstraint, listOf(firstValue, secondValue)))
                        } else if (editingText.isBlank() && editingSecondText.isBlank()) {
                            onStateChange(null)
                        }
                    }

                    else -> {
                        if (firstValue != null) {
                            onStateChange(TableFilterState(editingConstraint, listOf(firstValue)))
                        } else if (editingText.isBlank()) {
                            onStateChange(null)
                        }
                    }
                }
            }
        }
    }

    return remember(editingText, editingSecondText, editingConstraint, isEditing) {
        NumberFilterState(
            text = editingText,
            secondText = editingSecondText,
            constraint = editingConstraint,
            isEditing = isEditing,
            onTextChange = { newText ->
                if (newText.matches(filter.delegate.regex)) {
                    editingText = newText
                    isEditing = true
                }
            },
            onSecondTextChange = { newText ->
                if (newText.matches(filter.delegate.regex)) {
                    editingSecondText = newText
                    isEditing = true
                }
            },
            onConstraintChange = { newConstraint ->
                editingConstraint = newConstraint
                if (newConstraint != FilterConstraint.BETWEEN) {
                    editingSecondText = ""
                }
                isEditing = true
            },
            applyFilter = {
                val firstValue = filter.delegate.parse(editingText)

                when (editingConstraint) {
                    FilterConstraint.BETWEEN -> {
                        val secondValue = filter.delegate.parse(editingSecondText)
                        if (firstValue != null && secondValue != null) {
                            onStateChange(TableFilterState(editingConstraint, listOf(firstValue, secondValue)))
                        }
                    }

                    else -> {
                        if (firstValue != null) {
                            onStateChange(TableFilterState(editingConstraint, listOf(firstValue)))
                        } else {
                            onStateChange(null)
                        }
                    }
                }
                isEditing = false
            },
            clearFilter = {
                editingText = ""
                editingSecondText = ""
                onStateChange(null)
                isEditing = false
            },
            delegate = filter.delegate
        )
    }
}