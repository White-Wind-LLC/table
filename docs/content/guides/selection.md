# Selection

- `SelectionMode.None` (default), `Single`, `Multiple`.
- In Multiple mode, you can handle selection programmatically:

```kotlin
Table(
    itemsCount = items.size,
    itemAt = { index -> items[index] },
    state = state,
    columns = columns,
    onRowClick = { _ -> state.toggleCheck(/* row index comes from key or context */) }
)
```
