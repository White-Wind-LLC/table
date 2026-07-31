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
dependency of its own. It is public, so you can reuse a `TableIcons` value even where it is not the
default for that slot — for example, showing the same solid funnel glyph for both filter states
instead of switching to the outline variant when a filter is inactive:

```kotlin
import ua.wwind.table.icon.TableIcons

val icons = TableHeaderDefaults.icons(
    sortAsc = MyUp,
    sortDesc = MyDown,
    sortNeutral = MySort,
    filterActive = TableIcons.FilterAltFilled,
    filterInactive = TableIcons.FilterAltFilled
)
```
