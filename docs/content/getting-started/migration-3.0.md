# Migrating from 2.x to 3.0

3.0.0 collects the accessibility, keyboard, right-to-left and visual work done after 2.4.1, and
removes what 2.x deprecated. Everything that changed is listed here, in the order you are likely to
hit it. The compiler flags every change except the new visual defaults in the
[last section](#new-visual-defaults).

## Icons are Compose resources

The library ships its icons as Compose resource files instead of `ImageVector` code, so it carries
no icon dependency of its own.

- `TableIcons` values are `DrawableResource`; draw one with `painterResource`.
- `TableHeaderIcons` and `TableHeaderDefaults.icons(...)` take `Painter` instead of `ImageVector`.
- `LocalTableHeaderIcons` has no default outside a `Table`: a resource painter exists only inside
  composition. Reading it outside a `Table` throws.

```kotlin title="Before"
val icons = TableHeaderDefaults.icons(
    sortAsc = Icons.Default.KeyboardArrowUp,
    filterActive = TableIcons.FilterAltFilled,
)
```

```kotlin title="After"
val icons = TableHeaderDefaults.icons(
    sortAsc = rememberVectorPainter(Icons.Default.KeyboardArrowUp),
    filterActive = painterResource(TableIcons.FilterAltFilled),
)
```

Android and desktop apps get the icon files inside the library's aar and jar. iOS and web apps get
them through Compose Multiplatform resources packaging, which the `org.jetbrains.compose` Gradle
plugin sets up; a build that skips it, such as a hand-made XCFramework, fails when a `Table`
renders. See [Custom header icons](../guides/custom-header-icons.md).

## No more `ExperimentalTableApi`

The marker has been deprecated since 2.0.0 and is now gone. Delete every opt-in:

```kotlin title="Before"
@OptIn(ExperimentalTableApi::class)
@Composable
fun PeopleTable(items: List<Person>) { /* ... */ }
```

```kotlin title="After"
@Composable
fun PeopleTable(items: List<Person>) { /* ... */ }
```

## `TableState` forwarders are gone

In 2.1.0 column, selection and editing state moved to `state.columns`, `state.selection` and
`state.editing`, and the old members on `TableState` stayed as deprecated forwarders. They are
removed. If you still have warnings from 2.x, apply the IDE's *Replace with* quick fix before
upgrading; otherwise use this table.

| Removed                                        | Use instead                                      |
|------------------------------------------------|--------------------------------------------------|
| `columnOrder`                                  | `columns.order`                                  |
| `columnWidths`                                 | `columns.widths`                                 |
| `columnContentMaxWidths`                       | `columns.contentMaxWidths`                       |
| `columnHeaderWidths`                           | `columns.headerWidths`                           |
| `autoWidthAppliedForEmpty`                     | `columns.autoWidthAppliedForEmpty`               |
| `autoWidthAppliedForData`                      | `columns.autoWidthAppliedForData`                |
| `resolveColumnWidth(key, spec)`                | `columns.resolveWidth(key, spec)`                |
| `moveColumn(fromIndex, toIndex)`               | `columns.move(fromIndex, toIndex)`               |
| `setColumnOrder(newOrder)`                     | `columns.setOrder(newOrder)`                     |
| `resizeColumn(column, action)`                 | `columns.resize(column, action)`                 |
| `setColumnWidths(widths)`                      | `columns.setWidths(widths)`                      |
| `updateMaxContentWidth(column, width, source)` | `columns.updateMaxContentWidth(column, width, source)` |
| `setColumnWidthToMaxContent(column)`           | `columns.fitToContent(column)`                   |
| `recalculateAutoWidths()`                      | `columns.recalculateAutoWidths()`                |
| `selectedIndex`                                | `selection.selectedIndex`                        |
| `checkedIndices`                               | `selection.checkedIndices`                       |
| `selectedCell`                                 | `selection.selectedCell`                         |
| `toggleSelect(index)`                          | `selection.toggleRow(index)`                     |
| `focusRow(index)`                              | `selection.focusRow(index)`                      |
| `toggleCheck(index)`                           | `selection.toggleCheck(index)`                   |
| `toggleCheckAll(count)`                        | `selection.toggleCheckAll(count)`                |
| `selectCell(rowIndex, column)`                 | `selection.selectCell(rowIndex, column)`         |
| `editingRow`                                   | `editing.rowIndex`                               |
| `editingColumn`                                | `editing.column`                                 |
| `onRowEditStart`                               | `editing.onRowEditStart`                         |
| `onRowEditComplete`                            | `editing.onRowEditComplete`                      |
| `onEditCancel`                                 | `editing.onEditCancel`                           |
| `startEditing(item, rowIndex, column)`         | `editing.start(item, rowIndex, column)`          |
| `tryCompleteEditing()`                         | `editing.tryComplete()`                          |
| `completeCurrentCellEdit(visibleColumns)`      | `editing.completeCurrentCell(visibleColumns)`    |
| `cancelEditing()`                              | `editing.cancel()`                               |

## New `UiString` keys

Screen-reader names, the column menu, the empty states, filter labels and the format dialog add
about ninety `UiString` keys (`HeaderSort`, `ColumnMenuPinLeft`, `EmptyNoResults`,
`FilterErrorInvalidNumber`, `FormatRuleSave`, …). A custom `StringProvider` whose `when` has an
`else` branch keeps compiling and shows that branch's text for the new keys. One with an exhaustive
`when` and no `else` stops compiling until it covers them. To translate only some keys, fall back
to the English defaults:

```kotlin
object UkStrings : StringProvider {
    @Composable
    override fun get(key: UiString): String =
        when (key) {
            UiString.FilterClear -> "Очистити"
            // ...
            else -> DefaultStrings.get(key)
        }
}
```

## Deprecated in 3.0, removed in 4.0

These still exist but are deprecated at ERROR level: a call site no longer compiles, and
`@Suppress("DEPRECATION")` does not silence it. The IDE quick fix still applies where a direct
replacement exists.

| Deprecated                                 | Use instead                                                                                       |
|--------------------------------------------|---------------------------------------------------------------------------------------------------|
| `PinnedSide.Left`                          | `PinnedSide.Start` (the leading side, right in RTL)                                               |
| `PinnedSide.Right`                         | `PinnedSide.End`                                                                                  |
| `TableActiveFilters` without `tableData`   | pass `tableData`, so a chip click opens its filter panel                                          |
| `handleLoadState`                          | the paged `Table`'s `loadingContent`, `errorContent`, `emptyContent`, `loadingIndicator` and `errorBar` |

## Recompile

New parameters change the JVM signatures of `Table`, `EditableTable`, the paged `Table`, and the
constructors and `copy` of `TableDimensions` and `TableSettings`. A library compiled against 2.x
that calls them fails at runtime with `NoSuchMethodError` until it is recompiled against 3.0.

## New visual defaults

These compile unchanged but look different. Each comes from the theme, so a Material theme of your
own carries through; to get the 2.x look back, pass the old value explicitly.

| Change                                                                          | Old look                                                                                         |
|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| Selected row is `secondaryContainer` with a leading indicator bar               | `TableDefaults.colors(rowSelectedContainerColor = MaterialTheme.colorScheme.tertiary)` and `TableDimensions(selectionIndicatorWidth = 0.dp)` |
| Striped rows are `surfaceContainerLow`                                          | `TableDefaults.colors(stripedRowContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest)` |
| Header, body, footer and group rows have their own text styles                  | `Table(typography = TableDefaults.typography(...))`                                              |
| Headers follow the column's `alignment`                                         | `headerAlign(Alignment.CenterStart)` on the column                                               |
| Row, header, footer, group and fast filter heights grow with the system font size | `TableDimensions(scaleWithFontSize = false)`                                                   |
| Rows animate to their new place after a sort                                    | `TableSettings(motion = TableMotion.Reduced)`                                                    |
