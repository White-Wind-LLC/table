### Data Table for Compose Multiplatform (Material 3)

[![Maven Central](https://img.shields.io/maven-central/v/ua.wwind/table-core)](https://central.sonatype.com/artifact/ua.wwind/table-core)

Compose Multiplatform data table with Material 3 look & feel. Includes a core table, a conditional formatting add‑on,
and paging integration.

### Example

Here's what the data table looks like in action:

![Data Table Example](docs/images/datatable-example.png)

### Modules

- `table-core`: core table (rendering, header, sorting, column resize and reordering, filtering, row selection, i18n,
  styling/customization).
- `table-format`: dialog and APIs for rule‑based conditional formatting for cells/rows.
- `table-paging`: adapter on top of the core table for `PagingData` (`ua.wwind.paging`).

### Key features

- Material 3 header with sort/filter icons (customizable via `TableHeaderDefaults.icons`).
- Per‑column sorting (3‑state: ASC → DESC → none).
- Drag & drop to reorder columns in the header.
- Column resize via drag with per‑column min width.
- Filters: text, number (int/double, ranges), boolean, date, enum (single/multi; IN/NOT IN/EQUALS) with built‑in
  `FilterPanel`.
- Active filters header above the table (chips + “Clear all”).
- Row selection modes: None / Single / Multiple; optional striped rows.
- Extensive customization via `TableCustomization` (background/content color, elevation, borders, typography,
  alignment).
- i18n via `StringProvider` (default `DefaultStrings`).
- Targets: Android / JVM (Desktop) / JS (Web) / iOS (KMP source sets present; targets enabled via project conventions).

### Installation

Add repository (usually `mavenCentral`) and include the modules you need:

```kotlin
dependencies {
    implementation("ua.wwind:table-core:1.0.2")
    // optional
    implementation("ua.wwind:table-format:1.0.2")
    implementation("ua.wwind:table-paging:1.0.2")
}
```

### Quick start

#### 1) Model and fields

```kotlin
data class Person(val name: String, val age: Int)

enum class PersonField { Name, Age }
```

#### 2) Columns (DSL `tableColumns`)

```kotlin
val columns = tableColumns<Person, PersonField> {
    column(PersonField.Name) {
        header("Name")
        cell { Text(it.name) }
        sortable()
        filter(TableFilterType.TextTableFilter())
    }

    column(PersonField.Age) {
        header("Age")
        cell { Text(it.age.toString()) }
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

Column options: `sortable`, `resizable`, `visible`, `width(min, pref)`, `align(...)`, `filter(...)`,
`headerDecorations(...)`, `headerClickToSort(...)`.

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

Useful parameters: `rowLeading`/`rowTrailing` (extra slots), `placeholderRow`, `contextMenu` (long‑press/right‑click),
`colors = TableDefaults.colors()`, `icons = TableHeaderDefaults.icons()`.

### Paging integration (`table-paging`)

```kotlin
@Composable
fun PeoplePagingTable(paging: PagingData<Person>) {
    Table(
        items = paging,
        state = state,
        columns = columns,
    )
}
```

There is also `LazyListScope.handleLoadState(...)` to render loading/empty states.

### Conditional formatting (`table-format`)

- Build a `TableCustomization` from rules via `rememberCustomization(rules, matches = ...)`.
- Use `FormatDialog(...)` to create/edit rules (Design / Condition / Fields tabs).

```kotlin
// 1) Rules
val rules = remember {
  listOf(
    TableFormatRule.new<PersonField, Person>(id = 1, filter = Person("", 0)))
}

// 2) Matching logic
val customization = rememberCustomization<Person, PersonField, Person>(
  rules = rules,
  matches = { item, filter -> item.age >= 65 },
)

// 3) Pass customization to the table
Table(
  itemsCount = items.size,
  itemAt = { index -> items.getOrNull(index) },
  state = state,
  columns = columns,
  customization = customization,
)

// 4) Optional: rules editor dialog
FormatDialog(
  showDialog = show,
  rules = rules,
  onRulesChanged = { /* persist */ },
  getNewRule = { id -> TableFormatRule.new<PersonField, Person>(id, Person("", 0)) },
  getTitle = { field -> field.name },
  filters = { rule, onApply -> /* return list of FormatFilterData for fields */ emptyList() },
  entries = PersonField.entries,
  key = Unit,
  strings = DefaultStrings,
  onDismissRequest = { /* ... */ },
)
```

`rememberCustomization` merges base styles with matching rules into a resulting `TableCustomization` (background,
content color, text style, alignment, etc.).

### Styling

- Colors: `TableDefaults.colors(...)` →
  `TableColors(headerContainerColor, headerContentColor, rowContainerColor, rowSelectedContainerColor, stripedRowContainerColor)`.
- Geometry:
  `TableDimensions(defaultColumnWidth, defaultRowHeight, checkBoxColumnWidth, verticalDividerThickness, verticalDividerPaddingHorizontal)`.
- Behavior:
  `TableSettings(isDragEnabled, autoApplyFilters, autoFilterDebounce, stripedRows, showActiveFiltersHeader, selectionMode)`.
- Customization: implement `TableCustomization<T, C>` or use `rememberCustomization(...)` from `table-format`.
- Localization: provide a custom `StringProvider` (see `DefaultStrings` and keys in `UiString`).

### Filters

Types: `TextTableFilter`, `NumberTableFilter` (Int/Double delegates, optional range slider), `BooleanTableFilter`,
`DateTableFilter`, `EnumTableFilter`.

State: `TableFilterState(constraint, values)`; set/reset via `TableState.setFilter(column, state)`.

Built‑in Material 3 UI: `FilterPanel` opens from the filter icon in the header. Active filters are shown as chips in the
optional header (enable via `showActiveFiltersHeader = true`).

### Supported targets

- Android, JVM (Desktop), JS (Web), iOS (KMP source sets present; targets enabled via project conventions).

### License

Licensed under the Apache License, Version 2.0. See `LICENSE` or `https://www.apache.org/licenses/LICENSE-2.0.txt`.
