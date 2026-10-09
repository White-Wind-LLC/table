# Selection

- `SelectionMode.None` (default), `Single`, `Multiple`.
- In Multiple mode, you can handle selection programmatically:

```kotlin
Table(
    itemsCount = items.size,
    itemAt = { index -> items[index] },
    state = state,
    columns = columns,
    onRowClick = { _ -> state.selection.toggleCheck(/* row index comes from key or context */) }
)
```

### Keyboard

While the table body has focus, **Space** toggles the selected row: its checkmark in `Multiple`, its selection in
`Single`. **Enter** calls `onRowClick` for the row, unless the selected cell can be edited — then it starts
editing (see [Cell editing](cell-editing.md#keyboard)).
