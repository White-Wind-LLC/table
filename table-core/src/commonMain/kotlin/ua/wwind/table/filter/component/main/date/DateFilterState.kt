package ua.wwind.table.filter.component.main.date

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
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.isNullCheck

/**
 * State holder for date filter components.
 * Manages date selection, constraint selection, and validation.
 */
@Immutable
internal data class DateFilterState(
    val firstDate: LocalDate?,
    val secondDate: LocalDate?,
    val constraint: FilterConstraint,
    val isEditing: Boolean,
    val onFirstDateChange: (LocalDate?) -> Unit,
    val onSecondDateChange: (LocalDate?) -> Unit,
    val onConstraintChange: (FilterConstraint) -> Unit,
    val applyFilter: () -> Unit,
    val clearFilter: () -> Unit,
)

/**
 * Shared state management for date filters using Derived State Pattern.
 * Provides synchronized state between different filter components.
 *
 * This implementation ensures:
 * - Automatic synchronization between fast and main filters
 * - Support for single date (EQUALS, GT, GTE, LT, LTE) and range (BETWEEN) constraints
 * - Debounced filter updates for performance
 * - Proper handling of external state changes
 *
 * @param externalState Current filter state from the table
 * @param defaultConstraint Default constraint to use if not specified in state
 * @param autoApply Whether to apply changes automatically with debounce
 * @param debounceMs Debounce delay in milliseconds
 * @param isFastFilter Whether this is a fast filter (affects constraint handling)
 * @param onStateChange Callback when filter state changes
 * @return DateFilterState containing current values and update functions
 */
@OptIn(FlowPreview::class)
@Composable
internal fun rememberDateFilterState(
    externalState: TableFilterState<LocalDate>?,
    defaultConstraint: FilterConstraint = FilterConstraint.EQUALS,
    autoApply: Boolean = true,
    debounceMs: Long = 300L,
    isFastFilter: Boolean = false,
    onStateChange: (TableFilterState<LocalDate>?) -> Unit,
): DateFilterState {
    // Derived state from external source
    val sourceFirstDate by remember(externalState) {
        derivedStateOf {
            externalState?.values?.getOrNull(0)
        }
    }

    val sourceSecondDate by remember(externalState) {
        derivedStateOf {
            if (externalState?.constraint == FilterConstraint.BETWEEN) {
                externalState.values?.getOrNull(1)
            } else {
                null
            }
        }
    }

    val sourceConstraint by remember(externalState, defaultConstraint) {
        derivedStateOf {
            if (isFastFilter) defaultConstraint else externalState?.constraint ?: defaultConstraint
        }
    }

    var editingFirstDate by remember { mutableStateOf(sourceFirstDate) }
    var editingSecondDate by remember { mutableStateOf(sourceSecondDate) }
    var editingConstraint by remember { mutableStateOf(sourceConstraint) }
    var isEditing by remember { mutableStateOf(false) }

    // Sync editing state with external state when not editing
    LaunchedEffect(sourceFirstDate, sourceSecondDate, sourceConstraint) {
        if (!isEditing) {
            editingFirstDate = sourceFirstDate
            editingSecondDate = sourceSecondDate
            editingConstraint = sourceConstraint
        }
    }

    // Auto-apply with debounce
    if (autoApply) {
        LaunchedEffect(editingFirstDate, editingSecondDate, editingConstraint) {
            if (isEditing) {
                delay(debounceMs)
                isEditing = false
                emitDebouncedFilter(editingFirstDate, editingSecondDate, editingConstraint, onStateChange)
            }
        }
    }

    return remember(editingFirstDate, editingSecondDate, editingConstraint, isEditing) {
        DateFilterState(
            firstDate = editingFirstDate,
            secondDate = editingSecondDate,
            constraint = editingConstraint,
            isEditing = isEditing,
            onFirstDateChange = { newDate ->
                editingFirstDate = newDate
                isEditing = true
            },
            onSecondDateChange = { newDate ->
                editingSecondDate = newDate
                isEditing = true
            },
            onConstraintChange = { newConstraint ->
                editingConstraint = newConstraint
                if (newConstraint != FilterConstraint.BETWEEN) {
                    editingSecondDate = null
                }
                isEditing = true
            },
            applyFilter = {
                emitAppliedFilter(editingFirstDate, editingSecondDate, editingConstraint, onStateChange)
                isEditing = false
            },
            clearFilter = {
                editingFirstDate = null
                editingSecondDate = null
                onStateChange(null)
                isEditing = false
            },
        )
    }
}

/**
 * The values a date filter carries for [constraint], or null when the constraint needs a date the
 * user has not picked yet. Shared by both emit paths — this half never differed between them.
 */
private fun dateFilterValues(
    firstDate: LocalDate?,
    secondDate: LocalDate?,
    constraint: FilterConstraint,
): List<LocalDate>? =
    when (constraint) {
        FilterConstraint.BETWEEN -> {
            if (firstDate != null && secondDate != null) listOf(firstDate, secondDate) else null
        }

        FilterConstraint.IS_NULL, FilterConstraint.IS_NOT_NULL -> {
            emptyList()
        }

        else -> {
            firstDate?.let { listOf(it) }
        }
    }

/**
 * Emits the filter the debounced auto-apply path settles on.
 *
 * Deliberately kept separate from [emitAppliedFilter] rather than merged: the two paths do not agree
 * today, and reconciling them is a behaviour change rather than a refactor. This one also clears the
 * filter on an empty value list unless the constraint is IS_NULL/IS_NOT_NULL.
 */
private fun emitDebouncedFilter(
    firstDate: LocalDate?,
    secondDate: LocalDate?,
    constraint: FilterConstraint,
    onStateChange: (TableFilterState<LocalDate>?) -> Unit,
) {
    val values = dateFilterValues(firstDate, secondDate, constraint)
    if (values == null || (values.isEmpty() && !constraint.isNullCheck())) {
        onStateChange(null)
    } else {
        onStateChange(TableFilterState(constraint, values))
    }
}

/**
 * Emits the filter an explicit Apply settles on.
 *
 * See [emitDebouncedFilter] for why the two are not one function: this one clears only when the
 * values are absent altogether, so an empty list still emits a filter.
 */
private fun emitAppliedFilter(
    firstDate: LocalDate?,
    secondDate: LocalDate?,
    constraint: FilterConstraint,
    onStateChange: (TableFilterState<LocalDate>?) -> Unit,
) {
    val values = dateFilterValues(firstDate, secondDate, constraint)
    if (values == null) {
        onStateChange(null)
    } else {
        onStateChange(TableFilterState(constraint, values))
    }
}
