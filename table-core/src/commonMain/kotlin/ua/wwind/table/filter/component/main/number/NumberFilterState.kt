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
import ua.wwind.table.filter.component.main.resolveSourceConstraint
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.NumberInputFormat
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.filter.data.rememberNumberInputFormat
import ua.wwind.table.strings.StringProvider

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
    val error: NumberInputError?,
    val onTextChange: (String) -> Unit,
    val onSecondTextChange: (String) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
    /** Parses and formats [text] and [secondText] with the locale decimal separator. */
    val inputFormat: NumberInputFormat<T>,
) {
    val isError: Boolean get() = error != null
}

/**
 * Shared state management for number filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - Input validation using delegate regex, in the locale decimal separator or `.`
 * - Support for single value (EQUALS) and range (BETWEEN) constraints
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param filter The number filter configuration with delegate and constraints
 * @param strings Gives the decimal separator through [StringProvider.formatNumber]
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
    strings: StringProvider,
    defaultConstraint: FilterConstraint = FilterConstraint.EQUALS,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    isFastFilter: Boolean = false,
    onStateChange: (TableFilterState<T>?) -> Unit,
): NumberFilterState<T> {
    val inputFormat = rememberNumberInputFormat(filter.delegate, strings)

    // Derived state from external source
    val sourceText by remember(externalState, inputFormat) {
        derivedStateOf {
            externalState?.values?.firstOrNull()?.let { inputFormat.format(it) } ?: ""
        }
    }

    val sourceSecondText by remember(externalState, inputFormat) {
        derivedStateOf {
            if (externalState?.constraint == FilterConstraint.BETWEEN) {
                externalState.values?.getOrNull(1)?.let { inputFormat.format(it) } ?: ""
            } else {
                ""
            }
        }
    }

    val sourceConstraint by remember(externalState, defaultConstraint) {
        derivedStateOf {
            resolveSourceConstraint(externalState, defaultConstraint, isFastFilter)
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

    val emission = resolveNumberFilter(editingText, editingSecondText, editingConstraint, inputFormat)

    if (autoApply) {
        LaunchedEffect(editingText, editingSecondText, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                // isEditing stays true only when the input is invalid, keeping it for the user to fix.
                isEditing = !applyEmission(emission, currentOnStateChange.value)
            }
        }
    }

    return remember(editingText, editingSecondText, editingConstraint, isEditing, inputFormat) {
        NumberFilterState(
            text = editingText,
            secondText = editingSecondText,
            constraint = editingConstraint,
            isEditing = isEditing,
            error = numberInputError(editingText, editingSecondText, editingConstraint, inputFormat),
            onTextChange = { newText ->
                if (inputFormat.accepts(newText)) {
                    editingText = newText
                    isEditing = true
                }
            },
            onSecondTextChange = { newText ->
                if (inputFormat.accepts(newText)) {
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
            inputFormat = inputFormat,
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
    format: NumberInputFormat<T>,
): FilterEmission<T> {
    if (constraint.isNullCheck()) {
        return FilterEmission.Apply(TableFilterState(constraint, emptyList()))
    }

    val firstValue = format.parse(text)

    return when (constraint) {
        FilterConstraint.BETWEEN -> {
            val secondValue = format.parse(secondText)
            when {
                text.isBlank() && secondText.isBlank() -> {
                    FilterEmission.Clear
                }

                firstValue != null &&
                    secondValue != null &&
                    format.delegate.compare(firstValue, secondValue) -> {
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

/** Why number-filter input can't be applied; shown as inline text under the fields. */
internal enum class NumberInputError {
    /** A field holds text that doesn't parse as a number, such as a lone `-`. */
    InvalidNumber,

    /** Only one bound of a BETWEEN range is filled in. */
    RangeIncomplete,

    /** The BETWEEN range's From is greater than its To. */
    RangeInverted,
}

/**
 * Explains why [resolveNumberFilter] returns [FilterEmission.Invalid] for the same input, or returns
 * null when the input is valid or empty.
 */
internal fun <T : Number> numberInputError(
    text: String,
    secondText: String,
    constraint: FilterConstraint,
    format: NumberInputFormat<T>,
): NumberInputError? {
    if (constraint.isNullCheck()) return null
    val first = format.parse(text)
    val second = format.parse(secondText)
    val isBetween = constraint == FilterConstraint.BETWEEN
    return when {
        text.isNotBlank() && first == null -> NumberInputError.InvalidNumber
        !isBetween -> null
        secondText.isNotBlank() && second == null -> NumberInputError.InvalidNumber
        text.isBlank() && secondText.isBlank() -> null
        first == null || second == null -> NumberInputError.RangeIncomplete
        !format.delegate.compare(first, second) -> NumberInputError.RangeInverted
        else -> null
    }
}
