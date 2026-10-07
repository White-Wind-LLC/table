# Filters (built-in types)

- **TextTableFilter**: contains/starts/ends/equals/is_null/is_not_null.
- **NumberTableFilter(Int/Double)**: gt/gte/lt/lte/equals/not_equals/between/is_null/is_not_null + optional range slider
  via `rangeOptions`.
- **BooleanTableFilter**: equals; optional `getTitle(BooleanType)`.
- **DateTableFilter**: gt/gte/lt/lte/equals/between/is_null/is_not_null (uses `kotlinx.datetime.LocalDate`).
- **EnumTableFilter<T: Enum<T>>**: in/not_in/equals with `options: List<T>` and `getTitle(T)`.
- **CustomTableFilter<T, E>**: fully custom filter UI and state with access to table data. Implement
  `CustomFilterRenderer<T, E>` for main panel and
  optional fast filter (both receive `tableData: E` parameter), and `CustomFilterStateProvider<T>` for chip text.
  Supports data visualizations of any complexity, including dynamic histograms and statistics based on current table
  data.
- **DisabledTableFilter**: special marker filter type that completely disables filtering for a column while keeping
  the API contract (no filter UI is rendered for such columns in filter panels and conditional formatting dialogs).

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

## Empty state

While `itemsCount` is 0 the body shows `emptyContent`; the header, fast filters and footer stay visible.
The default (`TableDefaults.EmptyContent`) says "No data", or — while a filter is active — "No results match the
current filters" with a **Clear filters** button. Replace it through `TableEmptyScope`, which exposes `isFiltered`
and `clearFilters()` (removes every entry from `state.filters`):

```kotlin
Table(
    itemsCount = filtered.size,
    itemAt = { filtered.getOrNull(it) },
    state = state,
    columns = columns,
    emptyContent = {
        if (isFiltered) {
            TextButton(onClick = ::clearFilters) { Text("Nothing found — reset filters") }
        } else {
            Text("No orders yet")
        }
    },
)
```

An empty count cannot tell "no data" from "still loading": pass `emptyContent = {}` while your data loads. The
paging `Table` does this for you while `items` is null. Texts come from the `UiString.EmptyNoData`,
`EmptyNoResults` and `EmptyClearFilters` keys.
