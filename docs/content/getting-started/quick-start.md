# Quick start

#### 1) Model and fields

```kotlin
data class Person(val name: String, val age: Int)

enum class PersonField { Name, Age }
```

#### 2) Columns (DSL `tableColumns`)

```kotlin
val columns = tableColumns<Person, PersonField, Unit> {
    column(PersonField.Name, valueOf = { it.name }) {
        header("Name")
        cell { person, _ -> Text(person.name) }
        sortable()
        // Enable built‑in Text filter UI in header
        filter(TableFilterType.TextTableFilter())
        // Auto‑fit to content with optional max cap
        autoWidth(max = 500.dp)
    }

    column(PersonField.Age, valueOf = { it.age }) {
        header("Age")
        cell { person, _ -> Text(person.age.toString()) }
        sortable()
        align(Alignment.End)
        filter(
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
                rangeOptions = 0 to 120
            )
        )
    }
}
```

Column options: `sortable`, `resizable`, `visible`, `width(min, pref)`, `autoWidth(max)`, `align(...)`,
`rowHeight(min, max)`, `filter(...)`, `groupHeader(...)`, `headerDecorations(...)`, `headerClickToSort(...)`,
`footer(...)`.

#### 3) Table state

```kotlin
val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(
        stripedRows = true,
        showActiveFiltersHeader = true,
        selectionMode = SelectionMode.Single,
    )
)
```

You can also provide `initialOrder`, `initialWidths`, `initialSort` and update from outside using
`state.setColumnOrder(...)`, `state.setColumnWidths(...)`.

#### 4) Rendering (core)

```kotlin
@Composable
fun PeopleTable(items: List<Person>) {
    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = state,
        columns = columns,
        onRowClick = { person -> /* ... */ },
    )
}
```

Useful parameters: `placeholderRow`, `contextMenu` (long‑press/right‑click),
`colors = TableDefaults.colors()`, `icons = TableHeaderDefaults.icons()`,
`border` (outer border stroke; `null` uses theme default, `TableDefaults.NoBorder` disables border).
