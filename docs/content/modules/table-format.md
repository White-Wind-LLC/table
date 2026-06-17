# Conditional formatting

The `table-format` module provides dialog and APIs for rule‑based conditional formatting for cells/rows.

- Build a `TableCustomization` from rules via `rememberCustomization(rules, matches = ...)`. Row‑wide rules have
  `columns = emptyList()`; cell‑specific rules list field keys in `columns`.
- Use `FormatDialog(...)` to create/edit rules (Design / Condition / Fields tabs).

Simple example:

```kotlin
// 1) Rules
val rules = remember {
    listOf(
        TableFormatRule.new<PersonField, Person>(id = 1, filter = Person("", 0))
    )
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
- `FilterConstraint.isNullCheck()` extension function to check for IS_NULL/IS_NOT_NULL constraints.
- `TableFilterState.isActive()` extension function to determine if a filter is active.
- `VerticalScrollbarRenderer` and `VerticalScrollbarState` for custom scrollbar rendering in formatting dialogs.
