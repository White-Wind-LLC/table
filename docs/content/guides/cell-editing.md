# Cell editing mode

The table supports row‑scoped cell editing with custom edit UI, validation and keyboard navigation.

- **Table‑level switch**: enable editing via `TableSettings(editingEnabled = true)`.
- **Editable table**: use `EditableTable<T, C, E>` when you need editing support.
- **Table data parameter**: the generic parameter `E` represents table data (shared state) accessible in headers,
  footers, and edit cells. This allows passing validation errors, aggregated values, or any other table-wide state.
- **Editable columns DSL**: declare columns with `editableTableColumns<T, C, E> { ... }` and per‑cell `editCell`.
- **Callbacks**: validate and react to edit lifecycle with `onRowEditStart`, `onRowEditComplete`, `onEditCancelled`.
- **Keyboard**: Enter/Done moves to the next editable cell; Escape cancels editing (desktop targets).

#### TableCellTextField: text field adapted for table editing

For text editing inside table cells there is a dedicated composable `TableCellTextField`:

- **Focus integration**: it is already wired to the table focus system via `syncEditCellFocus()` on its `Modifier`.
  This ensures that when a row enters edit mode, the correct cell receives focus, and that keyboard navigation
  (Enter/Done to move to the next editable cell, Escape to cancel) works consistently across targets.
- **Compact layout**: by default it uses reduced paddings and no border to better fit into dense table rows.
- **Visual consistency**: styles and colors match Material 3 inputs used in the rest of the table UI.

Whenever you build text‑based edit UI for a cell, prefer `TableCellTextField` over a raw `TextField`/
`BasicTextField`. This way you get correct focus behavior and table‑aware UX without any additional setup.

Minimal example with `TableCellTextField`:

```kotlin
data class Person(val id: Int, val name: String, val age: Int)

// Table data containing displayed items and edit state
data class PersonTableData(
    val displayedPeople: List<Person> = emptyList(),
    val editState: PersonEditState = PersonEditState(),
)

// Per‑row edit state (validation, errors, etc.)
data class PersonEditState(
    val person: Person? = null,
    val nameError: String = "",
    val ageError: String = "",
)

enum class PersonColumn { NAME, AGE }

val settings = TableSettings(
    editingEnabled = true,
    rowHeightMode = RowHeightMode.Dynamic,
)

val state = rememberTableState(
    columns = PersonColumn.entries.toImmutableList(),
    settings = settings,
)

// Editable columns definition
val columns = editableTableColumns<Person, PersonColumn, PersonTableData> {
    column(PersonColumn.NAME, valueOf = { it.name }) {
        title { "Name" }
        cell { person, _ -> Text(person.name) }

        // Edit UI for the cell; table decides when to show it
        editCell { person, tableData, onComplete ->
            var text by remember(person) { mutableStateOf(person.name) }

            TableCellTextField(
                value = text,
                onValueChange = { text = it },
                isError = tableData.editState.nameError.isNotEmpty(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onComplete() }),
            )
        }

        // Footer with access to table data
        footer { tableData ->
            Text("Total: ${tableData.displayedPeople.size}")
        }
    }

    column(PersonColumn.AGE, valueOf = { it.age }) {
        title { "Age" }
        cell { person, _ -> Text(person.age.toString()) }

        editCell { person, tableData, onComplete ->
            var text by remember(person) { mutableStateOf(person.age.toString()) }

            TableCellTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter { it.isDigit() }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onComplete() }),
            )
        }
    }
}

// Somewhere in your screen
EditableTable(
    itemsCount = people.size,
    itemAt = { index -> people.getOrNull(index) },
    state = state,
    columns = columns,
    tableData = currentTableData, // your PersonTableData instance
    onRowEditStart = { person, rowIndex ->
        // Initialize edit state for the row
    },
    onRowEditComplete = { rowIndex ->
        // Validate and persist; return true to exit edit mode, false to keep editing
        true
    },
    onEditCancelled = { rowIndex ->
        // Optional: revert in‑memory changes
    },
)
```

#### Focus handling for custom edit implementations

If you build custom edit content that includes its own text field implementation or composite inputs, you should
integrate with the table focus handling. There are two options:

- **Use `TableCellTextField` directly**: this is the recommended and simplest way. It already calls
  `syncEditCellFocus()` on its `modifier`, so the cell participates in the table focus chain automatically.
- **Reuse the focus modifier in custom components**: if you must write your own text field wrapper, make sure to
  apply the same modifier:

```kotlin
@Composable
fun CustomCellEditor(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .syncEditCellFocus(),
    )
}
```

The `syncEditCellFocus()` modifier performs the following table‑specific work:

- **Tracks the active edit cell** and requests focus when its row/column become active.
- **Releases focus and clears selection** when editing ends or moves to another cell.
- **Coordinates keyboard navigation** so that `onComplete` in `editCell` moves to the next editable cell and
  eventually triggers `onRowEditComplete`.

By either using `TableCellTextField` or reusing `syncEditCellFocus()` in your own composables, custom edit UIs stay
consistent with the default table editing behavior.

Runtime behavior:

- Double‑click on an editable cell to enter **row edit mode**.
- All editable cells in the row render their `editCell` content.
- Press **Enter/Done** in a cell to call `onComplete()` and move to the next editable column.
- After the last editable cell, `onRowEditComplete` is invoked; returning `false` keeps the row in edit mode.
- Press **Escape** to cancel editing and trigger `onEditCancelled` (desktop targets).
