# Column menu

Every column header has a menu with the actions the column supports.

- **Sort:** ascending, descending, clear.
- **Filter:** open, clear.
- **Layout:** pin left or right, unpin, move left or right, auto-fit width, reset width.
- **Group:** group by, ungroup.
- **Hide column**, and **Show hidden columns** once something is hidden.

Items a column can never do are left out. Items the current state blocks stay visible, disabled,
with the reason under the label.

## Opening it

- Right-click or long-press a header.
- From the keyboard: Tab to the header row, move with ←/→, then press Shift+F10, the Menu key or
  Alt+↓. Shift+F10 in the body opens the menu of the selected cell's column.
- Set `TableSettings(showColumnMenuButton = true)` to show a ⋮ button in every header that has
  `headerDecorations` on.

Screen readers get the enabled items as custom actions on the header cell.

## Keyboard

The table has two Tab stops: the header row and the body.

| In the header | |
|---|---|
| ← / → , Home / End | Move between columns |
| Enter / Space | Sort |
| Shift+F10, Menu, Alt+↓ | Open the column menu |
| ↓ | Move into the body |

In the body, ↑ on the first row returns to the header.

When the menu is opened from the keyboard, focus goes to its first enabled item, and Esc closes it
and returns focus to the header.

## Customizing

Pass a `ColumnMenuBuilder` to `Table`. It receives the default sections for each column and returns
what to show:

```kotlin
Table(
    // ...
    columnMenu = ColumnMenuBuilder { column, defaults ->
        defaults.filterNot { it.id == ColumnMenuDefaults.Sections.Visibility } +
            ColumnMenuSection(
                id = "export",
                items = listOf(
                    ColumnMenuItem(
                        ColumnMenuItemId("export"),
                        "Export column",
                        icon = painterResource(Res.drawable.export),
                    ) { export(column) },
                    ColumnMenuItem(ColumnMenuItemId("copy"), "Copy values", icon = null) { copy(column) },
                ),
            )
    },
)
```

`icon` is any `Painter`, or `null` for none: `painterResource(...)` for a Compose resource (a
library icon too, e.g. `painterResource(TableIcons.Close)`), or `rememberVectorPainter(...)` for an
`ImageVector`. The builder runs in composition, so both can be called there.

Return an empty list to remove the menu for a column. Built-in item and section ids are in
`ColumnMenuDefaults.Ids` and `ColumnMenuDefaults.Sections`.

The builder shapes the column-header menu only. The menu of a sticky group header (Sort and
Ungroup) is not customizable.

## Runtime state

The menu drives public state on `state.columns`:

- `pinnedStartCount`, `pinnedEndCount`, `pinnedSide(key)`, `pin(key, side)`, `unpin(key)`
- `hidden`, `hide(key)`, `show(key)`, `showAll()`
- `order`, `moveBy(key, delta)`
- `state.clearSort()`

Columns can be pinned at both edges at once: `pinnedStartCount` leading columns stay at the start
edge (left in LTR) and `pinnedEndCount` trailing ones at the end edge. The menu of an unpinned column
offers **Pin left** and **Pin right** (`ColumnMenuDefaults.Ids.Pin` pins to the start edge,
`Ids.PinEnd` to the end edge); a pinned column offers **Unpin**. At least one column always stays
unpinned, and if the two counts together cover every visible column, none is pinned.

`TableSettings.pinnedStartCount` and `pinnedEndCount` are read once, when the state is created, and
never again: they seed the live counts on `state.columns`. Change visibility with `hide()` and
`show()` rather than writing to `hidden`: direct writes skip the last-visible-column and
pinned-block rules.

`hidden` holds only the columns hidden at runtime. A column whose spec has `visible = false` is not
in it and is never shown by `show()` or `showAll()`; the spec stays in your control. The pinned
counts count rendered columns only, so spec-hidden columns are not part of a pinned block.

To persist the layout, save `order`, `hidden`, `pinnedStartCount` and `pinnedEndCount`. To restore
it, pass the order as `initialOrder`, keep both pinned counts in `TableSettings` at `0`, and hide
and pin through the state. Non-zero settings would pin columns before the recipe starts, and
`pin()` would then add to those blocks instead of rebuilding them:

```kotlin
val state = rememberTableState(columns = columns, initialOrder = saved.order.toImmutableList())
LaunchedEffect(state) {
    // Only runtime-hidden keys: spec.visible = false columns are not in `saved.hidden`.
    saved.hidden.forEach { state.columns.hide(it) }
    // Rendered columns in saved order; the pinned blocks are their first and last N.
    val specVisible = columns.filter { it.visible }.map { it.key }.toSet()
    val visible = saved.order.filter { it in specVisible && it !in saved.hidden }
    visible.take(saved.pinnedStartCount).forEach { state.columns.pin(it, PinnedSide.Start) }
    // pin() grows a block inwards, so pin the outermost end column first.
    visible.takeLast(saved.pinnedEndCount).asReversed().forEach { state.columns.pin(it, PinnedSide.End) }
}
```

Hide first and then pin: a hidden column can sit between pinned ones in `order`, and `hide()` on a
pinned column unpins it.

!!! note "Before 3.7.0"
    `TableSettings.pinnedColumnsCount` / `pinnedColumnsSide` and `state.columns.pinnedCount`,
    `pin(key)`, `canPin(key)` pinned a single block on one side. They still work, deprecated, and are
    removed in 4.0: the old count is added to its side's new count, and the single-side calls act on
    `pinnedColumnsSide`.
