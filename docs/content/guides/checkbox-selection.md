# Checkbox selection with tableData

The `tableData` parameter enables implementing custom checkbox-based selection that shares state between cells, headers,
and external UI components. This pattern is useful when you need:

- Custom selection logic independent of the built-in `SelectionMode`
- A dedicated checkbox column with select-all functionality in the header
- External UI (e.g., floating action bar) that reacts to selection state
- Bulk operations on selected items (delete, export, etc.)

#### 1) Define table data with selection state

```kotlin
data class Person(val id: Int, val name: String, val age: Int)

enum class PersonColumn { SELECTION, NAME, AGE }

// Table data containing selection state
data class PersonTableData(
    val displayedPeople: List<Person> = emptyList(),
    val selectedIds: Set<Int> = emptySet(),
    val selectionModeEnabled: Boolean = false,
)
```

#### 2) Create a checkbox column using tableData

```kotlin
val columns = tableColumns<Person, PersonColumn, PersonTableData> {
    // Checkbox column for selection
    column(PersonColumn.SELECTION, valueOf = { it.id }) {
        width(48.dp, 48.dp)
        resizable(false)

        // Cell renders checkbox based on selection state from tableData
        cell { person, tableData ->
            if (tableData.selectionModeEnabled) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Checkbox(
                        checked = person.id in tableData.selectedIds,
                        onCheckedChange = { onToggleSelection(person.id) },
                    )
                }
            }
        }

        // Header renders tri-state checkbox for select all/none
        header { tableData ->
            if (tableData.selectionModeEnabled) {
                val displayedIds = tableData.displayedPeople.map { it.id }.toSet()
                val selectedCount = displayedIds.count { it in tableData.selectedIds }
                val toggleState = when (selectedCount) {
                    0 -> ToggleableState.Off
                    displayedIds.size -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TriStateCheckbox(
                        state = toggleState,
                        onClick = { onToggleSelectAll() },
                    )
                }
            }
        }
    }

    // Other columns...
    column(PersonColumn.NAME, valueOf = { it.name }) {
        title { "Name" }
        cell { person, _ -> Text(person.name) }
    }
}
```

#### 3) Manage selection state in ViewModel

```kotlin
class MyViewModel : ViewModel() {
    private val _people = MutableStateFlow<List<Person>>(loadPeople())
    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val _selectionModeEnabled = MutableStateFlow(false)

    val tableData: StateFlow<PersonTableData> = combine(
        _people,
        _selectedIds,
        _selectionModeEnabled,
    ) { people, selected, enabled ->
        PersonTableData(
            displayedPeople = people,
            selectedIds = selected,
            selectionModeEnabled = enabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonTableData())

    fun setSelectionMode(enabled: Boolean) {
        _selectionModeEnabled.value = enabled
        if (!enabled) _selectedIds.value = emptySet()
    }

    fun toggleSelection(personId: Int) {
        _selectedIds.update { current ->
            if (personId in current) current - personId else current + personId
        }
    }

    fun toggleSelectAll() {
        val displayedIds = _people.value.map { it.id }.toSet()
        _selectedIds.update { current ->
            if (displayedIds.all { it in current }) {
                current - displayedIds  // Deselect all
            } else {
                current + displayedIds  // Select all
            }
        }
    }

    fun deleteSelected() {
        val idsToDelete = _selectedIds.value
        _people.update { it.filter { person -> person.id !in idsToDelete } }
        _selectedIds.value = emptySet()
    }
}
```

#### 4) Render table with external action bar

```kotlin
@Composable
fun PeopleScreen(viewModel: MyViewModel) {
    val tableData by viewModel.tableData.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Table(
            itemsCount = tableData.displayedPeople.size,
            itemAt = { tableData.displayedPeople.getOrNull(it) },
            state = state,
            columns = columns,
            tableData = tableData,
        )

        // Floating action bar shown when items are selected
        if (tableData.selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${tableData.selectedIds.size} selected")
                    Button(onClick = { viewModel.deleteSelected() }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
```

#### Key benefits of this pattern

- **Reactive UI**: Checkbox state updates instantly when `tableData.selectedIds` changes.
- **Centralized state**: Selection logic lives in ViewModel, making it testable and reusable.
- **Flexible visibility**: Show/hide the checkbox column by controlling width via `state.setColumnWidths()`.
- **Bulk operations**: Easy to implement delete, export, or other actions on selected items.
- **Header integration**: Tri-state checkbox in header provides intuitive select-all UX.
