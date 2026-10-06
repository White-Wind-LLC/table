# Column menu

Every column header has a menu with the actions the column supports.

- **Sort:** ascending, descending, clear.
- **Filter:** open, clear.
- **Layout:** pin or unpin, move left or right, auto-fit width, reset width.
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
                    ColumnMenuItem(ColumnMenuItemId("export"), "Export column", icon = null) { export(column) },
                ),
            )
    },
)
```

Return an empty list to remove the menu for a column. Built-in item and section ids are in
`ColumnMenuDefaults.Ids` and `ColumnMenuDefaults.Sections`.

The builder shapes the column-header menu only. The menu of a sticky group header (Sort and
Ungroup) is not customizable.

## Runtime state

The menu drives public state on `state.columns`:

- `pinnedCount`, `pin(key)`, `unpin(key)`
- `hidden`, `hide(key)`, `show(key)`, `showAll()`
- `order`, `moveBy(key, delta)`
- `state.clearSort()`

`TableSettings.pinnedColumnsCount` is only the initial count; `pinnedCount` is the live one. Change
visibility with `hide()` and `show()` rather than writing to `hidden`: direct writes skip the
last-visible-column and pinned-block rules.

To persist the layout, save `order`, `hidden` and `pinnedCount`. To restore it, pass the order as
`initialOrder`, then hide and pin through the state:

```kotlin
val state = rememberTableState(columns = columns, initialOrder = saved.order.toImmutableList())
LaunchedEffect(state) {
    saved.hidden.forEach { state.columns.hide(it) }
    // Pinned to the left: the pinned block is the first pinnedCount visible columns.
    saved.order.filterNot { it in saved.hidden }.take(saved.pinnedCount).forEach { state.columns.pin(it) }
}
```

With no hidden columns, `TableSettings(pinnedColumnsCount = saved.pinnedCount)` restores the pins
directly. With hidden ones, hide first and then pin: a hidden column can sit between pinned ones
in `order`, and `hide()` on a pinned column unpins it.
