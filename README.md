### Data Table for Compose Multiplatform (Material 3)

[![Maven Central](https://img.shields.io/maven-central/v/ua.wwind.table-kmp/table-core)](https://central.sonatype.com/artifact/ua.wwind.table-kmp/table-core)

Compose Multiplatform data table with Material 3 look & feel. Includes a core table (`table-core`), a conditional
formatting add‑on (`table-format`), and paging integration (`table-paging`).

### Example

Here's what the data table looks like in action:

![Data Table Example](docs/images/datatable-example.png)
Live demo: [white-wind-llc.github.io/table](https://white-wind-llc.github.io/table/)

### Modules

- `table-core`: core table (rendering, header, sorting, column resize and reordering, filtering, row selection, i18n,
  styling/customization; dynamic or fixed row height).
- `table-format`: dialog and APIs for rule‑based conditional formatting for cells/rows.
- `table-paging`: adapter on top of the core table for `PagingData` (`ua.wwind.paging`).

### Key features

- Material 3 header with sort/filter icons (customizable via `TableHeaderDefaults.icons`).
- Per‑column sorting (3‑state: ASC → DESC → none).
- Data grouping by column with customizable group headers and sticky positioning.
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
    implementation("ua.wwind.table-kmp:table-core:1.2.0")
    // optional
    implementation("ua.wwind.table-kmp:table-format:1.2.0")
    implementation("ua.wwind.table-kmp:table-paging:1.2.0")
}
```

Opt‑in to experimental API on call sites that use the table:

```kotlin
@OptIn(ExperimentalTableApi::class)
@Composable
fun MyScreen() { /* ... */
}
```

### Compatibility

The following table lists compatibility information for released library versions.

| Version | Kotlin | Compose Multiplatform |
|---------|-------:|----------------------:|
| 1.2.0   | 2.2.10 |                 1.9.0 |

### Quick start

#### 1) Model and fields

```kotlin
data class Person(val name: String, val age: Int)

enum class PersonField { Name, Age }
```

#### 2) Columns (DSL `tableColumns`)

```kotlin
val columns = tableColumns<Person, PersonField> {
    column(PersonField.Name, valueOf = { it.name }) {
        header("Name")
        cell { Text(it.name) }
        sortable()
        // Enable built‑in Text filter UI in header
        filter(TableFilterType.TextTableFilter())
        // Auto‑fit to content with optional max cap
        autoWidth(max = 500.dp)
    }

    column(PersonField.Age, valueOf = { it.age }) {
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

Column options: `sortable`, `resizable`, `visible`, `width(min, pref)`, `autoWidth(max)`, `align(...)`,
`rowHeight(min, max)`, `filter(...)`, `groupHeader(...)`, `headerDecorations(...)`, `headerClickToSort(...)`.

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

### Data grouping

Group table data by any column to organize and visualize hierarchical relationships:

```kotlin
// Enable grouping programmatically
state.groupBy = PersonField.Department

// Or let users group via header dropdown menu
// (automatically available for all columns)
```

Customize group header appearance and content:

```kotlin
column(PersonField.Department, valueOf = { it.department }) {
    header("Department")
    cell { Text(it.department) }
    
    // Custom group header renderer
    groupHeader { groupValue ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Department: $groupValue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

Group headers are sticky and remain visible during scrolling. Configure group content alignment via table settings:

```kotlin
val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(
        groupContentAlignment = Alignment.CenterStart,
        // ... other settings
    )
)
```

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

### Core API reference (table-core)

- **Composable `Table<T, C>`**: renders header and virtualized rows.
    - **Required**: `itemsCount`, `itemAt(index)`, `state: TableState<C>`, `columns: List<ColumnSpec<T, C>>`.
    - **Slots**: `rowLeading(T)`, `rowTrailing(T)`, `placeholderRow()`.
    - **UX**: `onRowClick`, `onRowLongClick`, `contextMenu(item, pos, dismiss)`.
    - **Look**: `customization`, `colors = TableDefaults.colors()`, `icons = TableHeaderDefaults.icons()`, `strings`.
    - **Scroll**: optional `verticalState`, `horizontalState`.
- **Columns DSL**: `tableColumns { column(key, valueOf) { ... } }` produces `List<ColumnSpec<T, C>>`.
    - Header: `header("Text")` or `header { ... }`; optional `title { "Name" }` for active filter chips.
    - Sorting: `sortable()`, `headerClickToSort(Boolean)`.
    - Filters UI: `filter(TableFilterType.*)`.
    - Sizing: `width(min, pref)`, `autoWidth(max)`, `resizable(Boolean)`, `align(Alignment.Horizontal)`.
    - Row height hints: `rowHeight(min, max)` used when `rowHeightMode = Dynamic`.
    - Decorations: `headerDecorations(Boolean)` to hide built‑ins when fully customizing header.
- **Header customization**
    - When `headerDecorations = true` (default), the table places sort and filter icons automatically.
    - For a fully custom header, set `headerDecorations(false)` and use helpers inside `header { ... }`:

```kotlin
column(PersonField.Name, valueOf = { it.name }) {
    headerDecorations(false)
    header {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Name", modifier = Modifier.padding(end = 8.dp))
            TableHeaderSortIcon()
            TableHeaderFilterIcon()
        }
    }
    sortable()
    filter(TableFilterType.TextTableFilter())
}
```

- **State**: `rememberTableState(columns, initialSort?, initialOrder?, initialWidths?, settings?, dimensions?)`.
    - Sorting: `state.setSort(column, order?)`; current `state.sort`.
    - Grouping: `state.groupBy(column)` to enable grouping; `state.groupBy(null)` to disable.
    - Column order/size: `state.setColumnOrder(order)`, `state.resizeColumn(column, Set/Reset)`,
      `state.setColumnWidths(map)`.
    - Filters: `state.setFilter(column, TableFilterState(...))`; current per‑column `state.filters`.
    - Selection: `state.toggleSelect(index)`, `state.toggleCheck(index)`, `state.toggleCheckAll(count)`,
      `state.selectCell(row, column)`.
- **Settings and geometry**
    - `TableSettings`: `isDragEnabled`, `autoApplyFilters`, `autoFilterDebounce`, `stripedRows`,
      `showActiveFiltersHeader`, `selectionMode: None/Single/Multiple`, `groupContentAlignment`,
      `rowHeightMode: Fixed/Dynamic`.
    - `TableDimensions`: `defaultColumnWidth`, `defaultRowHeight`, `checkBoxColumnWidth`, `verticalDividerThickness`,
      `verticalDividerPaddingHorizontal`.
    - `TableColors`: via `TableDefaults.colors(...)`.

### Filters (built‑in types)

- **TextTableFilter**: contains/starts/ends/equals.
- **NumberTableFilter(Int/Double)**: gt/gte/lt/lte/equals/not_equals/between + optional range slider via `rangeOptions`.
- **BooleanTableFilter**: equals; optional `getTitle(BooleanType)`.
- **DateTableFilter**: gt/gte/lt/lte/equals (uses `kotlinx.datetime.LocalDate`).
- **EnumTableFilter<T: Enum<T>>**: in/not_in/equals with `options: List<T>` and `getTitle(T)`.

Applying filters to data is app‑specific. Example:

```kotlin
val filtered = remember(items, state.filters) {
    items.filter { item ->
        // Evaluate your domain against active state.filters
        // See `table-sample` for a full example
        true
    }
}
```

### Selection

- `SelectionMode.None` (default), `Single`, `Multiple`.
- In Multiple mode, you can render a leading checkbox column and wire actions:

```kotlin
Table(
    itemsCount = items.size,
    itemAt = { index -> items[index] },
    state = state,
    columns = columns,
  rowLeading = { _ -> Checkbox( /* ... */ ) },
  onRowClick = { _ -> state.toggleCheck(/* row index comes from key or context */) }
)
```

### Dynamic row height and auto‑width

- Dynamic height: set `rowHeightMode = RowHeightMode.Dynamic`. Use per‑column `rowHeight(min, max)` to hint bounds.
- Auto‑width: call `autoWidth(max?)` in column builder. The table measures header + first batch of rows and applies
  widths once per phase. Double‑click the header resizer to snap a column to its measured max content width.

### Custom header icons

Customize sort/filter icons:

```kotlin
val icons = TableHeaderDefaults.icons(
  sortAsc = MyUp,
  sortDesc = MyDown,
  sortNeutral = MySort,
  filterActive = MyFilterFilled,
  filterInactive = MyFilterOutline
)

Table(
    itemsCount = items.size,
    itemAt = { index -> items[index] },
    state = state,
    columns = columns,
    icons = icons
)
```

### Conditional formatting (table-format)

- Build `TableCustomization` from rules using `rememberCustomization(rules, matches = ...)`. Row‑wide rules have
  `columns = emptyList()`; cell‑specific rules list field keys in `columns`.
- Use `FormatDialog(...)` to let users create/edit rules.

Minimal example:

```kotlin
data class Person(val name: String, val age: Int, val rating: Int)
enum class PersonField { Name, Age, Rating }

// Rules
val rules = remember {
    val ratingFilter: Map<PersonField, TableFilterState<*>> =
        mapOf(
            PersonField.Rating to TableFilterState(
                constraint = FilterConstraint.GTE,
                values = listOf(4),
            ),
        )
    val ratingRule =
        TableFormatRule<PersonField, Map<PersonField, TableFilterState<*>>>(
            id = 1L,
            enabled = true,
            base = false,
            columns = listOf(PersonField.Rating),
            cellStyle = TableCellStyleConfig(
                contentColor = 0xFFFFD700.toInt(), // Gold
            ),
            filter = ratingFilter,
        )
    listOf(ratingRule)
}

// Matching logic (app‑specific)
val customization = rememberCustomization<Person, PersonField, Person>(
  rules = rules,
  matches = { person, ruleFilters ->
      for ((column, stateAny) in ruleFilters) {
          when (column) {
              PersonField.Rating -> {
                  val value = person.rating
                  val st = stateAny as TableFilterState<Int>
                  val constraint = st.constraint ?: continue
                  when (constraint) {
                      FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
                      FilterConstraint.BETWEEN -> {
                          val from = st.values?.getOrNull(0) ?: value
                          val to = st.values?.getOrNull(1) ?: value
                          from <= value && value <= to
                      }

                      else -> true
                  }
              }
              else -> true
          }
      }
  }
)

Table(
  itemsCount = items.size,
  itemAt = { index -> items[index] },
  state = state,
  columns = columns,
  customization = customization
)

// Optional dialog
FormatDialog(
  showDialog = show,
  rules = rules,
  onRulesChanged = { /* persist */ },
  getNewRule = { id -> TableFormatRule.new<PersonField, Person>(id, Person("", 0)) },
  getTitle = { it.name },
  filters = { rule, onApply -> emptyList() }, // build `FormatFilterData` list for your fields
  entries = PersonField.values().toList(),
  key = Unit,
  strings = DefaultStrings,
  onDismissRequest = { show = false }
)
```

Public API highlights:

- `rememberCustomization<T, C, FILTER>(rules, matches = ...) : TableCustomization<T, C>`.
- `TableFormatRule<FIELD, FILTER>` with `columns: List<FIELD>`, `cellStyle: TableCellStyleConfig`, `filter: FILTER`.
- `FormatDialog(...)` and `FormatDialogSettings` for UX tweaks.
- `FormatFilterData<E>` to describe per‑field filter controls in the dialog.

### Supported targets

- Android, JVM (Desktop), JS (Web), iOS (KMP source sets present; targets enabled via project conventions).

### License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

