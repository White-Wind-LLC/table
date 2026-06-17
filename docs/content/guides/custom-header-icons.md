# Custom header icons

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
