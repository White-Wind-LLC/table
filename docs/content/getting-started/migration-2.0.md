# Migrating from 1.x to 2.0

2.0.0 stabilizes the public API. Everything that changed is listed here, in the order you are likely
to hit it. Most call sites need one or two edits; the compiler flags all of them except the one noted
under [Parameter order](#parameter-order).

## No more opt-in

`Table`, `EditableTable` and the `table-paging` adapter no longer carry `@ExperimentalTableApi`.
Delete the annotation from your call sites:

```kotlin title="Before"
@OptIn(ExperimentalTableApi::class)
@Composable
fun PeopleTable(items: List<Person>) { /* ... */ }
```

```kotlin title="After"
@Composable
fun PeopleTable(items: List<Person>) { /* ... */ }
```

The `ExperimentalTableApi` marker is still shipped, deprecated, so a forgotten opt-in compiles with a
warning rather than breaking your build. It is removed in the next major release.

## Removed deprecated members

Each of these carried a `ReplaceWith` migration during 1.x; the IDE's *Replace with* quick fix
handles them.

| Removed                                    | Use instead                          |
|--------------------------------------------|--------------------------------------|
| `TableSettings.isDragEnabled`               | `TableSettings.rowReorderEnabled`    |
| `TableSettings.fixedColumnsCount`           | `TableSettings.pinnedColumnsCount`   |
| `TableSettings.fixedColumnsSide`            | `TableSettings.pinnedColumnsSide`    |
| `FixedSide`                                 | `PinnedSide`                         |
| `TableDimensions.fixedColumnDividerThickness` | `TableDimensions.pinnedColumnDividerThickness` |
| `TableRowContext.isGroup`                   | `TableRowContext.isInRowBlock`       |
| `TableSettings.isRowReorderEnabled`         | `TableSettings.rowReorderEnabled`    |

The last one is new to this release. The extension existed only to OR the new and deprecated reorder
flags together, so with `isDragEnabled` gone it is exactly the property it reads.
`isInteractionLockByRowReorderEnabled` is unchanged and still available.

## Renamed callbacks

Event callbacks are named for the event in present tense, per the Compose convention. This breaks
**named**-argument callers, which the compiler reports at every site.

| Before                          | After                        |
|---------------------------------|------------------------------|
| `EditableTable(onEditCancelled = …)` | `EditableTable(onEditCancel = …)` |
| `TableState.onEditCancelled`    | `TableState.onEditCancel`    |
| `FormatDialog(onRulesChanged = …)` | `FormatDialog(onRulesChange = …)` |

## Parameter order

Five public composables now order their parameters the Compose way — required parameters, `modifier`,
optional parameters, trailing lambda. **Callers using named arguments are unaffected**; positional
callers must reorder.

- `FilterDropdownField`, `FilterDropdownAnyField` — `values` and `onClick` move ahead of `getTitle`
  and `placeholder`:

    ```kotlin title="Before"
    FilterDropdownField(currentValue, { it.label }, "Pick one", values, onClick)
    ```

    ```kotlin title="After"
    FilterDropdownField(currentValue, values, onClick, getTitle = { it.label }, placeholder = "Pick one")
    ```

- `TableActiveFilters` — `modifier` moves ahead of `strings`.
- `FormatDialogTabRow` — `createTab` moves ahead of `modifier`; the `content` trailing lambda stays last.
- `rememberCustomization` — `matches` moves ahead of `key`.

!!! warning "Check `rememberCustomization` by hand"
    `key` is `Any?`, so a positional call that passed `matches` third still compiles after the
    reorder — it just binds your lambda to `key` and leaves `matches` unfilled or mismatched. The
    compiler cannot catch this one. Switch to named arguments:

    ```kotlin
    rememberCustomization(rules = rules, matches = ::matchesFilter, key = tableKey)
    ```

## Three format tabs gained a `modifier`

`FormatDialogDesignTab`, `FormatDialogConditionTab` and `FormatDialogFieldTab` now take a `modifier`,
applied to each component's own root node — previously the panels could not be sized, padded or
positioned by their caller at all. It sits as the first optional parameter, so it lands ahead of the
existing `scrollbarRenderer`: named-argument callers are unaffected, a positional call passing
`scrollbarRenderer` must update. Default behaviour is unchanged.

Relatedly, `FilterDropdownField`, `FilterDropdownAnyField` and `FormatDialogTabRow` now apply their
`modifier` to their own root node instead of to an inner element. This is source-compatible, but a
positioning, sizing or padding modifier you were passing now affects the whole component rather than
just the text field — check any that looked like a workaround for the old behaviour.

## Custom filter renderers

`CustomFilterRenderer.RenderPanel` emits UI, so returning a value from it broke the Compose
naming/emitter convention. It now returns `Unit` and receives a `CustomFilterPanelActions` handle
instead. Add the `panelActions` parameter (after `tableData`, ahead of the `onDismiss`/`onChange`
lambdas), drop the return type, and publish through `panelActions.set(...)` where the old `return`
was:

```kotlin title="Before"
@Composable
override fun RenderPanel(
    currentState: TableFilterState<MyState>?,
    tableData: MyTableData,
    onDismiss: () -> Unit,
    onChange: (TableFilterState<MyState>?) -> Unit,
): TableFilterType.CustomFilterActions {
    // ... panel UI ...
    return object : TableFilterType.CustomFilterActions {
        override fun applyFilter() { onChange(buildState()) }
        override fun clearFilter() { onChange(null) }
    }
}
```

```kotlin title="After"
@Composable
override fun RenderPanel(
    currentState: TableFilterState<MyState>?,
    tableData: MyTableData,
    panelActions: CustomFilterPanelActions,
    onDismiss: () -> Unit,
    onChange: (TableFilterState<MyState>?) -> Unit,
) {
    // ... panel UI ...
    panelActions.set(
        object : TableFilterType.CustomFilterActions {
            override fun applyFilter() { onChange(buildState()) }
            override fun clearFilter() { onChange(null) }
        },
    )
}
```

A panel that never calls `set` leaves the host's Apply/Clear buttons as no-ops. Clear still closes the
panel, because `FilterPanelActions` dismisses it itself.

## Filter behaviour

The built-in filters used to implement their emit rule twice — once in the debounced auto-apply
effect, once in `applyFilter` — and the two copies had drifted, so the same input produced different
filters depending on whether the debounce fired or the user pressed **Apply**. Both paths now run one
shared function. Two behaviours changed as a result:

- **Number filter**: invalid, incomplete or inverted input (a `to` below the `from`) is treated as an
  error. The filter is not applied, your input is preserved so it can be corrected, and the field is
  flagged. Only empty input clears the filter. If you were relying on a partially-typed range being
  applied on debounce, it no longer is.
- **Enum filter**: an empty selection combined with `IS_NULL` / `IS_NOT_NULL` stays applied. The
  debounced path used to drop it.

Date, text and boolean filters are unchanged. An incomplete date range still clears the filter — that
is deliberate.
