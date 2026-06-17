# table-paging

The `table-paging` module provides an adapter on top of the core table for `PagingData` (`ua.wwind.paging`).

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
