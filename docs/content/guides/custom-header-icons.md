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

The defaults come from `TableIcons`, the icon set the library vendors so that it needs no icon
dependency of its own. It is public, so you can reuse the defaults you are not overriding:

```kotlin
import ua.wwind.table.icon.TableIcons

val icons = TableHeaderDefaults.icons(
    sortNeutral = MySort,
    filterActive = TableIcons.FilterAltFilled,
    filterInactive = TableIcons.FilterAltOutlined
)
```
