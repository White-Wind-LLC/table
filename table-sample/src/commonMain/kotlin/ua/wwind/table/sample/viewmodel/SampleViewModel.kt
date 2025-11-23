package ua.wwind.table.sample.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatFilterData
import ua.wwind.table.format.data.TableCellStyleConfig
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.data.createDemoData
import ua.wwind.table.sample.filter.NumericRangeFilterState
import ua.wwind.table.sample.filter.createFilterTypes
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonEditState
import ua.wwind.table.sample.model.Position
import ua.wwind.table.state.SortState

@OptIn(ExperimentalTableApi::class)
class SampleViewModel : ViewModel() {
    // StateFlow for people list to enable reactive transformations
    val _people = MutableStateFlow<List<Person>>(createDemoData())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    // Current filters state
    private val currentFilters =
        MutableStateFlow<Map<PersonColumn, TableFilterState<*>>>(emptyMap())

    // Current sort state
    private val currentSort = MutableStateFlow<SortState<PersonColumn>?>(null)

    // Filtered and sorted people - derived from combining three StateFlows
    val displayedPeople: StateFlow<List<Person>> =
        combine(_people, currentFilters, currentSort) { peopleList, filters, sort ->
            // Apply filtering
            val filtered =
                peopleList.filter { person -> matchesPerson(person, filters) }

            // Apply sorting
            if (sort == null) {
                filtered
            } else {
                val base =
                    when (sort.column) {
                        PersonColumn.NAME ->
                            filtered.sortedBy { it.name.lowercase() }
                        PersonColumn.AGE -> filtered.sortedBy { it.age }
                        PersonColumn.ACTIVE -> filtered.sortedBy { it.active }
                        PersonColumn.ID -> filtered.sortedBy { it.id }
                        PersonColumn.EMAIL ->
                            filtered.sortedBy { it.email.lowercase() }
                        PersonColumn.CITY ->
                            filtered.sortedBy { it.city.lowercase() }
                        PersonColumn.COUNTRY ->
                            filtered.sortedBy { it.country.lowercase() }
                        PersonColumn.DEPARTMENT ->
                            filtered.sortedBy { it.department.lowercase() }
                        PersonColumn.POSITION ->
                            filtered.sortedBy { it.position.name }
                        PersonColumn.SALARY -> filtered.sortedBy { it.salary }
                        PersonColumn.RATING -> filtered.sortedBy { it.rating }
                        PersonColumn.HIRE_DATE -> filtered.sortedBy { it.hireDate }
                        PersonColumn.NOTES ->
                            filtered.sortedBy { it.notes.lowercase() }
                        PersonColumn.AGE_GROUP ->
                            filtered.sortedBy {
                                when {
                                    it.age < 25 -> 0
                                    it.age < 35 -> 1
                                    else -> 2
                                }
                            }
                        else -> filtered
                    }
                if (sort.order == SortOrder.DESCENDING) base.asReversed() else base
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
        mutableStateOf<
            ImmutableList<
                TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
            >,
        >(
            persistentListOf(),
        )
        private set

    // Dialog visibility
    var showFormatDialog by mutableStateOf(false)
        private set

    // Editing state
    var editingRowState by mutableStateOf<PersonEditState>(PersonEditState())
        private set

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
            val defaultState: TableFilterState<*> =
                when (column) {
                    PersonColumn.NAME ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.AGE ->
                        TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.ACTIVE ->
                        TableFilterState<Boolean>(
                            constraint = FilterConstraint.EQUALS,
                            values = null,
                        )
                    PersonColumn.ID ->
                        TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.EMAIL ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.CITY ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.COUNTRY ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.DEPARTMENT ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.POSITION ->
                        TableFilterState<List<Position>>(
                            constraint = null,
                            values = null,
                        )
                    PersonColumn.SALARY ->
                        TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.RATING ->
                        TableFilterState<Int>(constraint = null, values = null)
                    PersonColumn.HIRE_DATE ->
                        TableFilterState<kotlinx.datetime.LocalDate>(
                            constraint = null,
                            values = null,
                        )
                    PersonColumn.NOTES ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.AGE_GROUP ->
                        TableFilterState<String>(constraint = null, values = null)
                    PersonColumn.EXPAND ->
                        TableFilterState<Boolean>(
                            constraint = FilterConstraint.EQUALS,
                            values = null,
                        )
                }
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
     * Evaluate whether the given person matches the rule's filter map. Supports Text, Number(Int)
     * and Boolean filter types used in the sample.
     */
    fun matchesPerson(
        person: Person,
        ruleFilters: Map<PersonColumn, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in ruleFilters) {
            val type = filterTypes[column] ?: continue
            // If state has no constraint or values, skip this field (not restrictive)
            if (stateAny.constraint == null ||
                (
                    stateAny.values == null &&
                        stateAny.constraint != FilterConstraint.IS_NULL &&
                        stateAny.constraint != FilterConstraint.IS_NOT_NULL
                )
            ) {
                continue
            }

            when (column) {
                PersonColumn.NAME -> {
                    val value = person.name
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.AGE -> {
                    val value = person.age
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.EQUALS ->
                                value == (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.NOT_EQUALS ->
                                value != (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.BETWEEN -> {
                                val from = st.values?.getOrNull(0) ?: value
                                val to = st.values?.getOrNull(1) ?: value
                                from <= value && value <= to
                            }
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.ACTIVE -> {
                    val value = person.active
                    val st = stateAny as TableFilterState<Boolean>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.EQUALS ->
                                st.values?.firstOrNull()?.let { v -> value == v } ?: true
                            FilterConstraint.NOT_EQUALS ->
                                st.values?.firstOrNull()?.let { v -> value != v } ?: true
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.ID -> {
                    val value = person.id
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.EQUALS ->
                                value == (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.NOT_EQUALS ->
                                value != (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.BETWEEN -> {
                                val from = st.values?.getOrNull(0) ?: value
                                val to = st.values?.getOrNull(1) ?: value
                                from <= value && value <= to
                            }
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.EMAIL -> {
                    val value = person.email
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.CITY -> {
                    val value = person.city
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.COUNTRY -> {
                    val value = person.country
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.DEPARTMENT -> {
                    val value = person.department
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.POSITION -> {
                    val value = person.position
                    val st = stateAny as TableFilterState<*>
                    val constraint = st.constraint ?: continue

                    @Suppress("UNCHECKED_CAST")
                    val selectedValues = (st.values as? List<Position>) ?: emptyList()
                    val ok =
                        when (constraint) {
                            FilterConstraint.IN ->
                                selectedValues.isEmpty() || selectedValues.contains(value)
                            FilterConstraint.NOT_IN ->
                                selectedValues.isEmpty() || !selectedValues.contains(value)
                            FilterConstraint.EQUALS -> selectedValues.firstOrNull() == value
                            FilterConstraint.NOT_EQUALS -> selectedValues.firstOrNull() != value
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.SALARY -> {
                    val value = person.salary
                    // Check if using custom NumericRangeFilter
                    if (stateAny.values?.firstOrNull() is
                            NumericRangeFilterState
                    ) {
                        val customState =
                            stateAny.values?.firstOrNull() as?
                                NumericRangeFilterState
                        val ok = customState?.let { value in it.min..it.max } ?: true
                        if (!ok) return false
                    } else {
                        // Standard number filter
                        val st = stateAny as TableFilterState<Int>
                        val constraint = st.constraint ?: continue
                        val ok =
                            when (constraint) {
                                FilterConstraint.GT ->
                                    value > (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.GTE ->
                                    value >= (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.LT ->
                                    value < (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.LTE ->
                                    value <= (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.EQUALS ->
                                    value == (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.NOT_EQUALS ->
                                    value != (st.values?.getOrNull(0) ?: value)
                                FilterConstraint.BETWEEN -> {
                                    val from = st.values?.getOrNull(0) ?: value
                                    val to = st.values?.getOrNull(1) ?: value
                                    from <= value && value <= to
                                }
                                else -> true
                            }
                        if (!ok) return false
                    }
                }
                PersonColumn.RATING -> {
                    val value = person.rating
                    val st = stateAny as TableFilterState<Int>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.EQUALS ->
                                value == (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.NOT_EQUALS ->
                                value != (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.BETWEEN -> {
                                val from = st.values?.getOrNull(0) ?: value
                                val to = st.values?.getOrNull(1) ?: value
                                from <= value && value <= to
                            }
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.HIRE_DATE -> {
                    val value = person.hireDate
                    val st = stateAny as TableFilterState<LocalDate>
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.EQUALS ->
                                value == (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.NOT_EQUALS ->
                                value != (st.values?.getOrNull(0) ?: value)
                            FilterConstraint.BETWEEN -> {
                                val from = st.values?.getOrNull(0) ?: value
                                val to = st.values?.getOrNull(1) ?: value
                                from <= value && value <= to
                            }
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.NOTES -> {
                    val value = person.notes
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.AGE_GROUP -> {
                    val value =
                        when {
                            person.age < 25 -> "<25"
                            person.age < 35 -> "25-34"
                            else -> "35+"
                        }
                    val st = stateAny as TableFilterState<String>
                    val query = st.values?.firstOrNull().orEmpty()
                    val constraint = st.constraint ?: continue
                    val ok =
                        when (constraint) {
                            FilterConstraint.CONTAINS ->
                                value.contains(query, ignoreCase = true)
                            FilterConstraint.STARTS_WITH ->
                                value.startsWith(query, ignoreCase = true)
                            FilterConstraint.ENDS_WITH ->
                                value.endsWith(query, ignoreCase = true)
                            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
                            FilterConstraint.NOT_EQUALS ->
                                !value.equals(query, ignoreCase = true)
                            else -> true
                        }
                    if (!ok) return false
                }
                PersonColumn.EXPAND -> return true
            }
        }
        return true
    }

    /** Update filters - triggers automatic recalculation via StateFlow combination */
    fun updateFilters(filters: Map<PersonColumn, TableFilterState<*>>) {
        currentFilters.value = filters
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<PersonColumn>?) {
        currentSort.value = sort
    }

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
     * Validate the edited person and update PersonEditState with errors. Returns true if validation
     * passed, false otherwise.
     */
    fun validateEditedPerson(): Boolean {
        val edited = editingRowState.person ?: return true

        var nameError = ""
        var ageError = ""
        var salaryError = ""

        // Validate name
        if (edited.name.isBlank()) {
            nameError = "Name cannot be empty"
        }

        // Validate age
        if (edited.age < 18) {
            ageError = "Age must be at least 18"
        } else if (edited.age > 100) {
            ageError = "Age must not exceed 100"
        }

        // Validate salary
        if (edited.salary < 0) {
            salaryError = "Salary cannot be negative"
        }

        // Update edit state with errors
        editingRowState =
            editingRowState.copy(
                nameError = nameError,
                ageError = ageError,
                salaryError = salaryError,
            )

        // Return true if no errors
        return nameError.isEmpty() && ageError.isEmpty() && salaryError.isEmpty()
    }

    /** Handle UI events, including editing events from table columns */
    fun onEvent(event: SampleUiEvent) {
        when (event) {
            is SampleUiEvent.StartEditing -> {
                if (editingRowState.rowIndex != event.rowIndex) {
                    // Create a copy of the person for editing
                    editingRowState = PersonEditState(event.person, rowIndex = event.rowIndex)
                }
            }
            is SampleUiEvent.UpdateName -> {
                editingRowState =
                    editingRowState.copy(
                        person = editingRowState.person?.copy(name = event.name),
                        nameError = "", // Clear error on update
                    )
            }
            is SampleUiEvent.UpdateAge -> {
                editingRowState =
                    editingRowState.copy(
                        person = editingRowState.person?.copy(age = event.age),
                        ageError = "", // Clear error on update
                    )
            }
            is SampleUiEvent.UpdateEmail -> {
                editingRowState =
                    editingRowState.copy(
                        person = editingRowState.person?.copy(email = event.email),
                    )
            }
            is SampleUiEvent.UpdatePosition -> {
                editingRowState =
                    editingRowState.copy(
                        person = editingRowState.person?.copy(position = event.position),
                    )
            }
            is SampleUiEvent.UpdateSalary -> {
                editingRowState =
                    editingRowState.copy(
                        person = editingRowState.person?.copy(salary = event.salary),
                        salaryError = "", // Clear error on update
                    )
            }
            is SampleUiEvent.CompleteEditing -> {
                val edited = editingRowState.person
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
                editingRowState = PersonEditState()
            }
            is SampleUiEvent.CancelEditing -> {
                // Discard changes
                editingRowState = PersonEditState()
            }
        }
    }

    init {
        // Default conditional formatting: if RATING >= 4, set content color to gold for the Rating
        // column
        val ratingFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.RATING to
                    TableFilterState(
                        constraint = FilterConstraint.GTE,
                        values = listOf(4),
                    ),
            )
        val ratingRule =
            TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id = 1L,
                enabled = true,
                base = false,
                columns = listOf(PersonColumn.RATING),
                cellStyle =
                    TableCellStyleConfig(
                        contentColor = 0xFFFFD700.toInt(), // Gold
                    ),
                filter = ratingFilter,
            )
        // Default conditional formatting: if ACTIVE = false, set content color to gray for the
        // whole row
        val activeFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.ACTIVE to
                    TableFilterState(
                        constraint = FilterConstraint.EQUALS,
                        values = listOf(false),
                    ),
            )
        val activeRule =
            TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                id = 2L,
                enabled = true,
                base = false,
                columns = emptyList(),
                cellStyle =
                    TableCellStyleConfig(
                        contentColor = Color.LightGray.toArgb(),
                    ),
                filter = activeFilter,
            )
        rules = persistentListOf(ratingRule, activeRule)
    }
}
