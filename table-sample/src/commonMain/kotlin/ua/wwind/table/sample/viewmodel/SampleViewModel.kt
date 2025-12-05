package ua.wwind.table.sample.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatFilterData
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.data.createDemoData
import ua.wwind.table.sample.filter.createFilterTypes
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonEditState
import ua.wwind.table.sample.model.PersonTableData
import ua.wwind.table.sample.util.DefaultFormatRulesProvider
import ua.wwind.table.sample.util.PersonFilterMatcher
import ua.wwind.table.sample.util.PersonFilterStateFactory
import ua.wwind.table.sample.util.PersonSorter
import ua.wwind.table.sample.util.PersonValidator
import ua.wwind.table.state.SortState

@OptIn(ExperimentalTableApi::class)
class SampleViewModel : ViewModel() {
    // StateFlow for people list to enable reactive transformations
    private val _people = MutableStateFlow<List<Person>>(createDemoData())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    // Current filters state
    private val currentFilters =
        MutableStateFlow<Map<PersonColumn, TableFilterState<*>>>(emptyMap())

    // Current sort state
    private val currentSort = MutableStateFlow<SortState<PersonColumn>?>(null)

    // Selection state
    private val selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val selectionModeEnabled = MutableStateFlow(false)

    // Filtered and sorted people - derived from combining three StateFlows
    private val displayedPeople: StateFlow<List<Person>> =
        combine(_people, currentFilters, currentSort) { peopleList, filters, sort ->
            // Apply filtering
            val filtered =
                peopleList.filter { person ->
                    PersonFilterMatcher.matchesPerson(person, filters)
                }

            // Apply sorting
            PersonSorter.sortPeople(filtered, sort)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // People filtered by all filters except salary filter (for salary range calculation)
    private val peopleExcludingSalaryFilter: StateFlow<List<Person>> =
        combine(_people, currentFilters) { peopleList, filters ->
            // Apply all filters except salary filter
            val filtersExcludingSalary = filters.filterKeys { it != PersonColumn.SALARY }
            peopleList.filter { person ->
                PersonFilterMatcher.matchesPerson(person, filtersExcludingSalary)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // Define filter types per field (to drive the format dialog conditions)
    val filterTypes = createFilterTypes()

    // Conditional formatting rules (editable via dialog)
    var rules by
        mutableStateOf(DefaultFormatRulesProvider.createDefaultRules())
        private set

    // Dialog visibility
    var showFormatDialog by mutableStateOf(false)
        private set

    // Editing state as StateFlow for reactive composition
    private val editingRowState = MutableStateFlow(PersonEditState())

    // Combined table data - reactive state containing displayed people and editing state
    val tableData: StateFlow<PersonTableData> =
        combine(
            displayedPeople,
            peopleExcludingSalaryFilter,
            editingRowState,
            selectedIds,
            selectionModeEnabled,
        ) { people, peopleExcludingSalary, editState, selected, selectionEnabled ->
            PersonTableData(
                displayedPeople = people,
                peopleExcludingSalaryFilter = peopleExcludingSalary,
                editState = editState,
                selectedIds = selected,
                selectionModeEnabled = selectionEnabled,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersonTableData(),
        )

    /** Toggle dialog visibility */
    fun toggleFormatDialog(show: Boolean) {
        showFormatDialog = show
    }

    /** Update formatting rules */
    fun updateRules(
        newRules: ImmutableList<
            TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
        >,
    ) {
        rules = newRules
    }

    /** Build `FormatFilterData` list for the dialog from current rule state. */
    fun buildFormatFilterData(
        rule: TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
        onApply: (TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>) -> Unit,
    ): List<FormatFilterData<PersonColumn>> =
        PersonColumn.entries.map { column ->
            val type = filterTypes.getValue(column)
            val current: TableFilterState<*>? = rule.filter[column]
            val defaultState: TableFilterState<*> = PersonFilterStateFactory.createDefaultState(column)

            FormatFilterData(
                field = column,
                filterType = type,
                filterState = current ?: defaultState,
                onChange = { newState ->
                    val newMap = rule.filter.toMutableMap().apply { put(column, newState) }
                    onApply(rule.copy(filter = newMap))
                },
            )
        }

    /**
     * Evaluate whether the given person matches the rule's filter map.
     * Delegates to PersonFilterMatcher utility.
     */
    fun matchesPerson(
        person: Person,
        ruleFilters: Map<PersonColumn, TableFilterState<*>>,
    ): Boolean = PersonFilterMatcher.matchesPerson(person, ruleFilters)

    /** Update filters - triggers automatic recalculation via StateFlow combination */
    fun updateFilters(filters: Map<PersonColumn, TableFilterState<*>>) {
        currentFilters.value = filters
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<PersonColumn>?) {
        currentSort.value = sort
    }

    /** Toggle selection mode on or off */
    fun setSelectionMode(enabled: Boolean) {
        selectionModeEnabled.value = enabled
        if (!enabled) {
            // Clear selections when disabling selection mode
            selectedIds.value = emptySet()
        }
    }

    /** Toggle expanded state for person movement details */
    fun toggleMovementExpanded(personId: Int) {
        _people.update { currentPeople ->
            val index = currentPeople.indexOfFirst { person -> person.id == personId }
            if (index < 0) return@update currentPeople

            val currentPerson = currentPeople[index]
            val updatedPerson =
                currentPerson.copy(expandedMovement = !currentPerson.expandedMovement)

            // Return updated list with modified person
            currentPeople.toMutableList().apply { set(index, updatedPerson) }
        }
    }

    /**
     * Validate the edited person and update PersonEditState with errors.
     * Returns true if validation passed, false otherwise.
     */
    fun validateEditedPerson(): Boolean {
        val edited = editingRowState.value.person ?: return true

        val validationResult = PersonValidator.validate(edited)

        // Update edit state with errors
        editingRowState.update { current ->
            current.copy(
                nameError = validationResult.nameError,
                ageError = validationResult.ageError,
                salaryError = validationResult.salaryError,
            )
        }

        return validationResult.isValid
    }

    /** Handle UI events, including editing events from table columns */
    fun onEvent(event: SampleUiEvent) {
        when (event) {
            is SampleUiEvent.StartEditing -> {
                if (editingRowState.value.rowIndex != event.rowIndex) {
                    // Create a copy of the person for editing
                    editingRowState.value = PersonEditState(event.person, rowIndex = event.rowIndex)
                }
            }

            is SampleUiEvent.UpdateName -> {
                editingRowState.update { current ->
                    current.copy(
                        person = current.person?.copy(name = event.name),
                        nameError = "", // Clear error on update
                    )
                }
            }

            is SampleUiEvent.UpdateAge -> {
                editingRowState.update { current ->
                    current.copy(
                        person = current.person?.copy(age = event.age),
                        ageError = "", // Clear error on update
                    )
                }
            }

            is SampleUiEvent.UpdateEmail -> {
                editingRowState.update { current ->
                    current.copy(
                        person = current.person?.copy(email = event.email),
                    )
                }
            }

            is SampleUiEvent.UpdatePosition -> {
                editingRowState.update { current ->
                    current.copy(
                        person = current.person?.copy(position = event.position),
                    )
                }
            }

            is SampleUiEvent.UpdateSalary -> {
                editingRowState.update { current ->
                    current.copy(
                        person = current.person?.copy(salary = event.salary),
                        salaryError = "", // Clear error on update
                    )
                }
            }

            is SampleUiEvent.CompleteEditing -> {
                val edited = editingRowState.value.person
                if (edited != null) {
                    _people.update { currentPeople ->
                        val index = currentPeople.indexOfFirst { it.id == edited.id }
                        if (index >= 0) {
                            // Return updated list with modified person
                            currentPeople.toMutableList().apply { set(index, edited) }
                        } else {
                            currentPeople
                        }
                    }
                }
                // Clear editing state
                editingRowState.value = PersonEditState()
            }

            is SampleUiEvent.CancelEditing -> {
                // Discard changes
                editingRowState.value = PersonEditState()
            }

            is SampleUiEvent.ToggleSelection -> {
                selectedIds.update { current ->
                    if (event.personId in current) {
                        current - event.personId
                    } else {
                        current + event.personId
                    }
                }
            }

            is SampleUiEvent.ToggleSelectAll -> {
                val displayedIds = displayedPeople.value.map { it.id }.toSet()
                selectedIds.update { current ->
                    // If all displayed are selected, deselect all; otherwise select all displayed
                    if (displayedIds.all { it in current }) {
                        current - displayedIds
                    } else {
                        current + displayedIds
                    }
                }
            }

            is SampleUiEvent.DeleteSelected -> {
                val idsToDelete = selectedIds.value
                _people.update { currentPeople ->
                    currentPeople.filter { it.id !in idsToDelete }
                }
                selectedIds.value = emptySet()
            }

            is SampleUiEvent.ClearSelection -> {
                selectedIds.value = emptySet()
            }
        }
    }
}
