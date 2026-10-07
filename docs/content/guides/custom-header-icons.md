# Custom header icons

Customize sort/filter icons. Each slot takes a `Painter`, so any icon source fits: a resource of your
own, an `ImageVector` through `rememberVectorPainter`, or a bitmap:

```kotlin
val icons = TableHeaderDefaults.icons(
    sortAsc = painterResource(Res.drawable.my_up),
    sortDesc = painterResource(Res.drawable.my_down),
    sortNeutral = rememberVectorPainter(MySortVector),
    filterActive = painterResource(Res.drawable.my_filter_filled),
    filterInactive = painterResource(Res.drawable.my_filter_outline)
)

Table(
    itemsCount = items.size,
    itemAt = { index -> items[index] },
    state = state,
    columns = columns,
    icons = icons
)
```

The defaults come from `TableIcons`, the icon set the library ships as Compose resource files so that
it needs no icon dependency of its own. Each `TableIcons` value is a `DrawableResource`; draw it with
`painterResource`. It is public, so you can reuse a `TableIcons` value even where it is not the
default for that slot — for example, showing the same solid funnel glyph for both filter states
instead of switching to the outline variant when a filter is inactive:

```kotlin
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.icon.TableIcons

val icons = TableHeaderDefaults.icons(
    sortAsc = painterResource(Res.drawable.my_up),
    sortDesc = painterResource(Res.drawable.my_down),
    sortNeutral = painterResource(Res.drawable.my_sort),
    filterActive = painterResource(TableIcons.FilterAltFilled),
    filterInactive = painterResource(TableIcons.FilterAltFilled)
)
```

!!! note
    `TableIcons` are Compose resource files loaded at runtime. Android and desktop apps get them
    inside the library's aar and jar. iOS and web apps get them through Compose Multiplatform
    resources packaging, which the `org.jetbrains.compose` Gradle plugin sets up; a build that skips
    it (for example a hand-made XCFramework) has no icon files and fails when a `Table` renders.

!!! note
    On web (wasm and JS) Compose resources load asynchronously, so an icon can be blank for the
    first frame after the table appears.
