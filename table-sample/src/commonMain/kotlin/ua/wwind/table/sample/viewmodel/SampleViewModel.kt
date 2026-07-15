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
import ua.wwind.table.moveRowGroup
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.data.createDemoData
import ua.wwind.table.sample.filter.filterTypes
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

    // Conditional formatting rules (editable via dialog)
    var rules by
        mutableStateOf(DefaultFormatRulesProvider.createDefaultRules())
        private set

    // Dialog visibility
    var showFormatDialog by mutableStateOf(false)
        private set

    // Group whose name is being edited; null while the rename dialog is closed. The id doubles as
    // the dialog's payload, so there is no "open but no target" state to get wrong.
    var renamingGroupId by mutableStateOf<String?>(null)
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

    /** Open the rename dialog for [groupId], or close it when null. */
    fun setRenamingGroup(groupId: String?) {
        renamingGroupId = groupId
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

            is SampleUiEvent.GroupSelected -> {
                val ids = selectedIds.value
                if (ids.size < 2) return
                var grouped = false
                _people.update { currentPeople ->
                    val anchor = currentPeople.indexOfFirst { it.id in ids }
                    if (anchor < 0) return@update currentPeople

                    // The id IS the name, so the block is named after its leader - the topmost
                    // selected row - and group ids are kept unique: a duplicate would make two
                    // blocks one identity, renamed together yet still separate drag units unless
                    // adjacent. A leader's name can collide, so it is suffixed until free.
                    // Only rows staying OUTSIDE the new block can hold a name against it; the
                    // selected ones are all about to take the new id, which is what lets two whole
                    // groups merge back under the leader's plain name.
                    // Derived from `currentPeople` alone, so a retried CAS attempt is harmless.
                    val takenGroupIds =
                        currentPeople.filterNot { it.id in ids }.mapNotNull { it.groupId }.toSet()
                    val groupId = uniqueGroupId(currentPeople[anchor].name, takenGroupIds)

                    // A drag unit must be contiguous: rowGroupsOf only collapses adjacent rows,
                    // so the rows are pulled together first and only then share an id.
                    val block =
                        currentPeople.filter { it.id in ids }.map { it.copy(groupId = groupId) }
                    val rest = currentPeople.filterNot { it.id in ids }

                    // The anchor is the FIRST selected index, so every row before it is unselected
                    // and survives in `rest` - the insertion point is still the anchor after the
                    // removal. Do not "fix" this by shifting it. Removing the selected rows is safe
                    // on its own: it can only bring same-id rows closer, never split a run.
                    // Never cut another group in half: if the insertion point lands between two rows
                    // of the same group, push it past that group's last row. Splitting a run would
                    // leave two blocks sharing one id - one logical group torn in two, which renames
                    // together but drags apart.
                    var insertAt = anchor.coerceAtMost(rest.size)
                    while (
                        insertAt > 0 && insertAt < rest.size &&
                        rest[insertAt - 1].groupId != null &&
                        rest[insertAt - 1].groupId == rest[insertAt].groupId
                    ) {
                        insertAt++
                    }

                    grouped = true
                    rest.toMutableList().apply { addAll(insertAt, block) }
                }
                if (grouped) {
                    // Grouping reorders rows, so a stale sort would fight the new order.
                    currentSort.value = null
                }
            }

            is SampleUiEvent.UngroupSelected -> {
                val ids = selectedIds.value
                if (ids.isEmpty()) return
                var ungrouped = false
                _people.update { currentPeople ->
                    // Only a row that is IN a group can leave one: a selected row without a group
                    // keeps both its null id and its place.
                    val touchedGroupIds =
                        currentPeople.filter { it.id in ids }.mapNotNull { it.groupId }.toSet()
                    if (touchedGroupIds.isEmpty()) return@update currentPeople

                    // A row leaving a group must leave the block as well. rowGroupsOf only collapses
                    // ADJACENT rows, so merely clearing the id of a row in the middle of the run
                    // tears the block into two halves that BOTH keep the group's id - one name, two
                    // drag units, exactly the duplicate the uniqueness invariant forbids. Dropping
                    // the leavers just past the group's last row keeps the rows that stay
                    // contiguous, so the group survives whole under its own id.
                    val lastIndexOfGroup =
                        touchedGroupIds.associateWith { groupId ->
                            currentPeople.indexOfLast { it.groupId == groupId }
                        }
                    val leavers = mutableMapOf<String, MutableList<Person>>()

                    ungrouped = true
                    buildList {
                        currentPeople.forEachIndexed { index, person ->
                            val groupId = person.groupId
                            if (groupId != null && person.id in ids) {
                                leavers.getOrPut(groupId) { mutableListOf() } +=
                                    person.copy(groupId = null)
                            } else {
                                add(person)
                            }
                            // Flushed once the run is over, so the leavers land right under what is
                            // left of the block - and back in their own place when the whole group
                            // left and there is no block any more. Relative order is preserved
                            // because they were collected in list order.
                            if (groupId != null && lastIndexOfGroup[groupId] == index) {
                                leavers.remove(groupId)?.let { addAll(it) }
                            }
                        }
                    }
                }
                if (ungrouped) {
                    // Ungrouping reorders rows, so a stale sort would fight the new order.
                    currentSort.value = null
                }
            }

            is SampleUiEvent.RenameGroup -> {
                val newGroupId = event.newGroupId
                // A blank id still groups - rowGroupsOf only skips nulls - so it would not ungroup
                // the block, just leave it with a nameless band. Checked ahead of the uniqueness
                // guard below, which would only catch the SECOND blank name, not the first.
                // A no-op rename would rewrite every row for nothing.
                if (newGroupId.isBlank() || newGroupId == event.groupId) return
                // Rows keep their position: renaming must never reorder, so no sort reset either.
                _people.update { currentPeople ->
                    // The id IS the name, so a taken name would fuse two groups into one identity:
                    // both would rename together yet stay separate drag units unless adjacent.
                    // The dialog already refuses taken names; this is the last line of defence, and
                    // it bails out rather than half-applying. Any row already carrying `newGroupId`
                    // is outside the renamed group - the equal-id case returned above.
                    if (currentPeople.any { it.groupId == newGroupId }) return@update currentPeople
                    currentPeople.map {
                        if (it.groupId == event.groupId) it.copy(groupId = newGroupId) else it
                    }
                }
            }

            is SampleUiEvent.ClearSelection -> {
                selectedIds.value = emptySet()
            }

            is SampleUiEvent.RowMove -> {
                var moved = false
                _people.update { currentPeople ->
                    if (currentPeople.size < 2) return@update currentPeople

                    val sourceIndex = currentPeople.indexOfFirst { it.id == event.fromPersonId }
                    val targetIndex = currentPeople.indexOfFirst { it.id == event.toPersonId }
                    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex == targetIndex) {
                        return@update currentPeople
                    }

                    moved = true
                    currentPeople.toMutableList().apply {
                        val sourcePerson = this[sourceIndex]
                        this[sourceIndex] = this[targetIndex]
                        this[targetIndex] = sourcePerson
                    }
                }
                if (moved) {
                    currentSort.value = null
                }
            }

            is SampleUiEvent.RowsMove -> {
                var moved = false
                _people.update { currentPeople ->
                    fun indicesOf(ids: List<Int>): List<Int> =
                        ids
                            .mapNotNull { id ->
                                currentPeople.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                            }.sorted()

                    fun contiguousRangeOrNull(indices: List<Int>): IntRange? =
                        when {
                            indices.isEmpty() -> null
                            indices.last() - indices.first() + 1 != indices.size -> null
                            else -> indices.first()..indices.last()
                        }

                    // Blocks are contiguous on screen but may not be in the unfiltered master list;
                    // moving a split block would scramble it, so skip instead.
                    val from =
                        contiguousRangeOrNull(indicesOf(event.fromPersonIds))
                            ?: return@update currentPeople
                    val to =
                        contiguousRangeOrNull(indicesOf(event.toPersonIds))
                            ?: return@update currentPeople
                    if (to.first in from) return@update currentPeople

                    moved = true
                    currentPeople.toMutableList().apply {
                        moveRowGroup(from = from, to = to)
                    }
                }
                if (moved) {
                    currentSort.value = null
                }
            }

            is SampleUiEvent.MovementRowMove -> {
                _people.update { currentPeople ->
                    val personIndex = currentPeople.indexOfFirst { it.id == event.personId }
                    if (personIndex < 0) return@update currentPeople

                    val person = currentPeople[personIndex]
                    val movements = person.movements
                    if (
                        movements.size < 2 ||
                        event.fromIndex !in movements.indices ||
                        event.toIndex !in movements.indices ||
                        event.fromIndex == event.toIndex
                    ) {
                        return@update currentPeople
                    }

                    val reorderedMovements =
                        movements.toMutableList().apply {
                            add(event.toIndex, removeAt(event.fromIndex))
                        }

                    currentPeople.toMutableList().apply {
                        this[personIndex] = person.copy(movements = reorderedMovements)
                    }
                }
            }
        }
    }

    /**
     * First free name in the sequence [base], "[base] 2", "[base] 3"... Deterministic on purpose:
     * the id doubles as the label, so a random or timestamped suffix would be unique but unreadable.
     * Comparison is exact, matching `rowGroupsOf`'s `==` identity rule.
     */
    private fun uniqueGroupId(
        base: String,
        takenGroupIds: Set<String>,
    ): String {
        if (base !in takenGroupIds) return base
        var suffix = 2
        while ("$base $suffix" in takenGroupIds) suffix++
        return "$base $suffix"
    }
}
