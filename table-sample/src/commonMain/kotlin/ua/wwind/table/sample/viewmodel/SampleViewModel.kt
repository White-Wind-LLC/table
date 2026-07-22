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
import ua.wwind.table.RowBlockMove
import ua.wwind.table.RowWithinBlockMove
import ua.wwind.table.applyRowBlockMove
import ua.wwind.table.applyRowReorderWithinBlock
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.data.createDemoData
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonEditState
import ua.wwind.table.sample.model.PersonMovement
import ua.wwind.table.sample.model.PersonTableData
import ua.wwind.table.sample.model.movementBlockId
import ua.wwind.table.sample.util.DefaultFormatRulesProvider
import ua.wwind.table.sample.util.PersonFilterMatcher
import ua.wwind.table.sample.util.PersonSorter
import ua.wwind.table.sample.util.PersonValidator
import ua.wwind.table.sortedWithinRowBlocks
import ua.wwind.table.state.SortState

class SampleViewModel : ViewModel() {
    // StateFlow for people list to enable reactive transformations
    private val _people = MutableStateFlow<List<Person>>(createDemoData())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    // Current filters state
    private val currentFilters =
        MutableStateFlow<Map<PersonColumn, TableFilterState<*>>>(emptyMap())

    // Current sort state
    private val currentSort = MutableStateFlow<SortState<PersonColumn>?>(null)

    // Within-blocks sort demo: a free sort fragments blocks into duplicate bands, so this toggle
    // routes the active sort through sortedWithinRowBlocks, which keeps every block whole.
    private val _sortWithinBlocks = MutableStateFlow(false)
    val sortWithinBlocks: StateFlow<Boolean> = _sortWithinBlocks.asStateFlow()

    // Selection state
    private val selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val selectionModeEnabled = MutableStateFlow(false)

    // Filtered and sorted people - derived from combining the source list with view settings
    private val displayedPeople: StateFlow<List<Person>> =
        combine(
            _people,
            currentFilters,
            currentSort,
            _sortWithinBlocks,
        ) { peopleList, filters, sort, withinBlocks ->
            // Apply filtering
            val filtered =
                peopleList.filter { person ->
                    PersonFilterMatcher.matchesPerson(person, filters)
                }

            // Apply sorting
            val comparator = PersonSorter.comparatorFor(sort)
            when {
                comparator == null -> filtered

                // A live projection re-pins unit order after every committed move, hiding it. Safe
                // ONLY because every move handler clears currentSort on commit — copy this without
                // that clear and moves become invisible.
                withinBlocks -> filtered.sortedWithinRowBlocks({ it.groupId }, comparator)

                else -> filtered.sortedWith(comparator)
            }
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

    /** Update filters - triggers automatic recalculation via StateFlow combination */
    fun updateFilters(filters: Map<PersonColumn, TableFilterState<*>>) {
        currentFilters.value = filters
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<PersonColumn>?) {
        currentSort.value = sort
    }

    /** Route the active sort through `sortedWithinRowBlocks` instead of a free (fragmenting) sort. */
    fun setSortWithinBlocks(enabled: Boolean) {
        _sortWithinBlocks.value = enabled
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

    /**
     * Handle UI events, including editing events from table columns.
     *
     * The single exhaustive dispatch over [SampleUiEvent]. Every branch is either a field update on
     * the row being edited or a one-line delegation to the private handler below it, so both the
     * length and the complexity count track the number of events in the sealed type rather than
     * anything a reader has to follow — both rules are suppressed rather than fixed. Keeping the
     * dispatch in one place is what makes the event contract readable at a glance.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
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
                groupSelected()
            }

            is SampleUiEvent.UngroupSelected -> {
                ungroupSelected()
            }

            is SampleUiEvent.RenameGroup -> {
                renameGroup(event.groupId, event.newGroupId)
            }

            is SampleUiEvent.ClearSelection -> {
                selectedIds.value = emptySet()
            }

            is SampleUiEvent.RowMove -> {
                moveRow(event.fromPersonId, event.toPersonId)
            }

            is SampleUiEvent.BlockMove -> {
                moveBlock(event.move)
            }

            is SampleUiEvent.RowWithinBlockMove -> {
                moveRowWithinBlock(event.move)
            }

            is SampleUiEvent.MovementBlockMove -> {
                moveMovementBlock(event.personId, event.move)
            }

            is SampleUiEvent.MovementRowWithinBlockMove -> {
                moveMovementRowWithinBlock(event.personId, event.move)
            }

            is SampleUiEvent.MovementRowMove -> {
                moveMovementRow(event.personId, event.fromIndex, event.toIndex)
            }
        }
    }

    /**
     * Pulls the selected rows together under one block id, anchored on the first of them.
     */
    private fun groupSelected() {
        val ids = selectedIds.value
        if (ids.size < 2) return
        var grouped = false
        _people.update { currentPeople ->
            val anchor = currentPeople.indexOfFirst { it.id in ids }
            if (anchor < 0) return@update currentPeople

            // The id IS the name and must stay unique: a duplicate would fuse two blocks
            // into one identity. Only rows staying OUTSIDE the new block can hold a name
            // against it, which is what lets two whole groups merge under the leader's name.
            val takenGroupIds =
                currentPeople.filterNot { it.id in ids }.mapNotNull { it.groupId }.toSet()
            val groupId = uniqueGroupId(currentPeople[anchor].name, takenGroupIds)

            // A drag unit must be contiguous: blocks form over ADJACENT rows with equal
            // blockOf ids, so the rows are pulled together first and only then share an id.
            val block =
                currentPeople.filter { it.id in ids }.map { it.copy(groupId = groupId) }
            val rest = currentPeople.filterNot { it.id in ids }

            grouped = true
            rest.toMutableList().apply { addAll(blockInsertionIndex(rest, anchor), block) }
        }
        if (grouped) {
            // Grouping reorders rows, so a stale sort would fight the new order.
            currentSort.value = null
        }
    }

    /**
     * Where a freshly formed block may land in [rest] without cutting an existing one.
     *
     * [anchor] is the FIRST selected index, so rows before it survive in [rest] and it still points
     * at the right slot after the removal — do not "fix" it by shifting. It is then pushed past a
     * group it would cut: a split run leaves two blocks sharing one id.
     */
    private fun blockInsertionIndex(
        rest: List<Person>,
        anchor: Int,
    ): Int {
        var insertAt = anchor.coerceAtMost(rest.size)
        while (
            insertAt > 0 && insertAt < rest.size &&
            rest[insertAt - 1].groupId != null &&
            rest[insertAt - 1].groupId == rest[insertAt].groupId
        ) {
            insertAt++
        }
        return insertAt
    }

    /**
     * Drops the selected rows out of their blocks, past the last row of each block they leave.
     */
    private fun ungroupSelected() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        var ungrouped = false
        _people.update { currentPeople ->
            // Only a row that is IN a group can leave one: a selected row without a group
            // keeps both its null id and its place.
            val touchedGroupIds =
                currentPeople.filter { it.id in ids }.mapNotNull { it.groupId }.toSet()
            if (touchedGroupIds.isEmpty()) return@update currentPeople

            ungrouped = true
            withLeaversDropped(currentPeople, ids, touchedGroupIds)
        }
        if (ungrouped) {
            // Ungrouping reorders rows, so a stale sort would fight the new order.
            currentSort.value = null
        }
    }

    /**
     * [people] with every selected row of [touchedGroupIds] ungrouped and re-emitted after the last
     * row of the block it left.
     *
     * Clearing the id of a row mid-run would tear the block into two halves that both keep the
     * group's id. Dropping leavers past the group's last row keeps the rows that stay contiguous,
     * and flushing per group preserves the leavers' relative order.
     */
    private fun withLeaversDropped(
        people: List<Person>,
        ids: Set<Int>,
        touchedGroupIds: Set<String>,
    ): List<Person> {
        val lastIndexOfGroup =
            touchedGroupIds.associateWith { groupId ->
                people.indexOfLast { it.groupId == groupId }
            }
        val leavers = mutableMapOf<String, MutableList<Person>>()

        return buildList {
            people.forEachIndexed { index, person ->
                val groupId = person.groupId
                if (groupId != null && person.id in ids) {
                    leavers.getOrPut(groupId) { mutableListOf() } += person.copy(groupId = null)
                } else {
                    add(person)
                }
                if (groupId != null && lastIndexOfGroup[groupId] == index) {
                    leavers.remove(groupId)?.let { addAll(it) }
                }
            }
        }
    }

    /** Renames a block in place. Rows keep their position, so no sort reset either. */
    private fun renameGroup(
        groupId: String,
        newGroupId: String,
    ) {
        // A blank id still groups (derivation only skips nulls), so it would leave a nameless
        // band rather than ungroup.
        if (newGroupId.isBlank() || newGroupId == groupId) return
        _people.update { currentPeople ->
            // Last line of defence behind the dialog: a taken name would fuse two groups into
            // one identity, so bail out rather than half-apply.
            if (currentPeople.any { it.groupId == newGroupId }) return@update currentPeople
            currentPeople.map {
                if (it.groupId == groupId) it.copy(groupId = newGroupId) else it
            }
        }
    }

    /** Swaps two rows by person id. */
    private fun moveRow(
        fromPersonId: Int,
        toPersonId: Int,
    ) {
        var moved = false
        _people.update { currentPeople ->
            if (currentPeople.size < 2) return@update currentPeople

            val sourceIndex = currentPeople.indexOfFirst { it.id == fromPersonId }
            val targetIndex = currentPeople.indexOfFirst { it.id == toPersonId }
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
            // Same invariant as moveBlock below: an active sort is a live projection over the
            // source, and any projection re-pins row order — the swap would commit to the source
            // yet never show. Clearing the sort keeps the reorder visible.
            currentSort.value = null
        }
    }

    /** Commits a whole-block drag against the people list. */
    private fun moveBlock(move: RowBlockMove) {
        var moved = false
        _people.update { currentPeople ->
            // keyOf must mirror the table's rowKey — the move's anchors are row keys.
            val updated = currentPeople.toMutableList()
            updated.applyRowBlockMove(
                move = move,
                keyOf = { it.id.toString() },
                blockOf = { it.groupId },
            )
            // applyRowBlockMove leaves the list untouched when no anchor resolves.
            moved = updated != currentPeople
            updated
        }
        if (moved) {
            // Load-bearing, not tidiness: without this clear the live sort projection
            // re-pins unit order and the committed move becomes invisible.
            currentSort.value = null
        }
    }

    /** Commits a within-block row drag against the people list. */
    private fun moveRowWithinBlock(move: RowWithinBlockMove) {
        var moved = false
        _people.update { currentPeople ->
            // keyOf must mirror the table's rowKey — the move's anchors are row keys.
            val updated = currentPeople.toMutableList()
            updated.applyRowReorderWithinBlock(
                move = move,
                keyOf = { it.id.toString() },
                blockOf = { it.groupId },
            )
            moved = updated != currentPeople
            updated
        }
        if (moved) {
            // Same dissolution as moveBlock: drop the live within-block sort projection so
            // the order just committed is not re-pinned on the next emission.
            currentSort.value = null
        }
    }

    /** Commits a whole-block drag inside one person's embedded movements table. */
    private fun moveMovementBlock(
        personId: Int,
        move: RowBlockMove,
    ) {
        updateMovements(personId) { movements ->
            movements.applyRowBlockMove(
                move = move,
                // Mirrors the embedded table's rowKey and blockOf — see movementBlockId.
                keyOf = { it.date },
                blockOf = { it.movementBlockId },
            )
        }
    }

    /** Commits a within-block row drag inside one person's embedded movements table. */
    private fun moveMovementRowWithinBlock(
        personId: Int,
        move: RowWithinBlockMove,
    ) {
        updateMovements(personId) { movements ->
            movements.applyRowReorderWithinBlock(
                move = move,
                keyOf = { it.date },
                blockOf = { it.movementBlockId },
            )
        }
    }

    /** Moves one movement row of [personId] from [fromIndex] to [toIndex]. */
    private fun moveMovementRow(
        personId: Int,
        fromIndex: Int,
        toIndex: Int,
    ) {
        updateMovements(personId) { movements ->
            if (
                movements.size < 2 ||
                fromIndex !in movements.indices ||
                toIndex !in movements.indices ||
                fromIndex == toIndex
            ) {
                return@updateMovements
            }
            movements.add(toIndex, movements.removeAt(fromIndex))
        }
    }

    /**
     * Applies [mutate] to a mutable copy of [personId]'s movements and writes it back, leaving the
     * people list untouched when the person is gone or the mutation was a no-op.
     */
    private fun updateMovements(
        personId: Int,
        mutate: (MutableList<PersonMovement>) -> Unit,
    ) {
        _people.update { currentPeople ->
            val personIndex = currentPeople.indexOfFirst { it.id == personId }
            if (personIndex < 0) return@update currentPeople

            val person = currentPeople[personIndex]
            val movements = person.movements.toMutableList()
            mutate(movements)
            if (movements == person.movements) return@update currentPeople

            currentPeople.toMutableList().apply {
                this[personIndex] = person.copy(movements = movements)
            }
        }
    }

    /**
     * First free name in the sequence [base], "[base] 2", "[base] 3"… Deterministic on purpose: the
     * id doubles as the label, so a random suffix would be unique but unreadable.
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
