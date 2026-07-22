package ua.wwind.table.filter.component.main.number

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
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isNullCheck

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
    val isError: Boolean,
    val onTextChange: (String) -> Unit,
    val onSecondTextChange: (String) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
    val delegate: TableFilterType.NumberTableFilter.NumberFilterDelegate<T>,
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
    onStateChange: (TableFilterState<T>?) -> Unit,
): NumberFilterState<T> {
    // Derived state from external source
    val sourceText by remember(externalState) {
        derivedStateOf {
            externalState?.values?.firstOrNull()?.let { filter.delegate.format(it) } ?: ""
        }
    }

    val sourceSecondText by remember(externalState) {
        derivedStateOf {
            if (externalState?.constraint == FilterConstraint.BETWEEN) {
                externalState.values?.getOrNull(1)?.let { filter.delegate.format(it) } ?: ""
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

    val currentOnStateChange = rememberUpdatedState(onStateChange)

    LaunchedEffect(sourceText, sourceSecondText, sourceConstraint) {
        if (!isEditing) {
            editingText = sourceText
            editingSecondText = sourceSecondText
            editingConstraint = sourceConstraint
        }
    }

    val emission = resolveNumberFilter(editingText, editingSecondText, editingConstraint, filter.delegate)

    if (autoApply) {
        LaunchedEffect(editingText, editingSecondText, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                // isEditing stays true only when the input is invalid, keeping it for the user to fix.
                isEditing = !applyEmission(emission, currentOnStateChange.value)
            }
        }
    }

    return remember(editingText, editingSecondText, editingConstraint, isEditing) {
        NumberFilterState(
            text = editingText,
            secondText = editingSecondText,
            constraint = editingConstraint,
            isEditing = isEditing,
            isError = emission is FilterEmission.Invalid,
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
                isEditing = !applyEmission(emission, currentOnStateChange.value)
            },
            clearFilter = {
                editingText = ""
                editingSecondText = ""
                currentOnStateChange.value(null)
                isEditing = false
            },
            delegate = filter.delegate,
        )
    }
}

/**
 * Resolves the current number-filter input into the single [FilterEmission] that both the debounced
 * auto-apply path and the explicit Apply path act on, so the two can never disagree (issue #55).
 *
 * - Empty text clears the filter.
 * - A parseable value (or a valid ascending `from <= to` BETWEEN range) applies it.
 * - Anything else — unparsable text, a half-filled range, or an inverted `from > to` range — is
 *   [FilterEmission.Invalid]: nothing is emitted, the input is kept so the user can fix it, and the
 *   UI shows the error.
 */
internal fun <T : Number> resolveNumberFilter(
    text: String,
    secondText: String,
    constraint: FilterConstraint,
    delegate: TableFilterType.NumberTableFilter.NumberFilterDelegate<T>,
): FilterEmission<T> {
    if (constraint.isNullCheck()) {
        return FilterEmission.Apply(TableFilterState(constraint, emptyList()))
    }

    val firstValue = delegate.parse(text)

    return when (constraint) {
        FilterConstraint.BETWEEN -> {
            val secondValue = delegate.parse(secondText)
            when {
                text.isBlank() && secondText.isBlank() -> {
                    FilterEmission.Clear
                }

                firstValue != null &&
                    secondValue != null &&
                    delegate.compare(firstValue, secondValue) -> {
                    FilterEmission.Apply(TableFilterState(constraint, listOf(firstValue, secondValue)))
                }

                else -> {
                    FilterEmission.Invalid
                }
            }
        }

        else -> {
            when {
                text.isBlank() -> FilterEmission.Clear
                firstValue != null -> FilterEmission.Apply(TableFilterState(constraint, listOf(firstValue)))
                else -> FilterEmission.Invalid
            }
        }
    }
}
