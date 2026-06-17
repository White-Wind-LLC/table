# Core API reference

- **Composable `Table<T, C>`**: renders header and virtualized rows for read-only tables (tableData = Unit).
    - **Required**: `itemsCount`, `itemAt(index)`, `state: TableState<C>`, `columns: List<ColumnSpec<T, C, Unit>>`.
    - **Slots**: `placeholderRow()`.
    - **UX**: `onRowClick`, `onRowLongClick`, `onRowMove`, `contextMenu(item, pos, dismiss)`.
    - **Look**: `customization`, `colors = TableDefaults.colors()`, `icons = TableHeaderDefaults.icons()`, `strings`,
      `shape`, `border` (outer border; `null` = theme default, `TableDefaults.NoBorder` = no border).
    - **Scroll**: optional `verticalState`, `horizontalState`.
    - **Embedded content**: `embedded` flag and `rowEmbedded` slot let you render nested detail content or even a
      secondary table inside each row, while still reusing the same table state, filters and formatting rules.
- **Composable `Table<T, C, E>`**: overload that accepts custom table data for headers, footers, and edit cells.
    - **Additional parameter**: `tableData: E` - shared state accessible in headers, footers, custom filters, and edit
      cells.
    - All other parameters same as read-only variant.
- **Composable `EditableTable<T, C, E>`**: renders header and virtualized rows with editing support.
    - **Additional parameters**: `tableData: E`, `onRowMove`, `onRowEditStart`, `onRowEditComplete`, `onEditCancelled`.
    - Columns must use `ColumnSpec<T, C, E>` with `E` matching the tableData type.
- **Columns DSL**:
    - `tableColumns<T, C, E> { ... }` produces `List<ColumnSpec<T, C, E>>` for read-only tables.
    - `editableTableColumns<T, C, E> { ... }` produces `List<ColumnSpec<T, C, E>>` for editable tables.
    - Column configuration:
        - Cell: `cell { item, tableData -> ... }` for regular cell content with access to table data (use `_` if table
          data is not needed). Cell content uses `context(TableCellScope)`, enabling helpers such as
          `Modifier.draggableHandle()` and `Modifier.longPressDraggableHandle()`.
        - Header: `header("Text")` or `header(tableData) { ... }`; optional `title { "Name" }` for active filter chips.
        - Footer: `footer(tableData) { ... }` for custom footer cell content with access to table data.
        - Editing: `editCell { item, tableData, onComplete -> ... }` for custom edit UI.
        - Sorting: `sortable()`, `headerClickToSort(Boolean)`.
        - Filters UI: `filter(TableFilterType.*)`.
        - Sizing: `width(min, pref)`, `autoWidth(max)`, `resizable(Boolean)`, `align(Alignment.Horizontal)`.
        - Row height hints: `rowHeight(min, max)` used when `rowHeightMode = Dynamic`.
        - Decorations: `headerDecorations(Boolean)` to hide built‑ins when fully customizing header.
- **Header customization**
    - When `headerDecorations = true` (default), the table places sort and filter icons automatically.
    - For a fully custom header, set `headerDecorations(false)` and use helpers inside `header { ... }`:

```kotlin
column(PersonField.Name, valueOf = { it.name }) {
    headerDecorations(false)
    header {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Name", modifier = Modifier.padding(end = 8.dp))
            TableHeaderSortIcon()
            TableHeaderFilterIcon()
        }
    }
    sortable()
    filter(TableFilterType.TextTableFilter())
}
```

- **State**: `rememberTableState(columns, initialSort?, initialOrder?, initialWidths?, settings?, dimensions?)`.
    - Compatibility normalization: when `settings.rowReorderEnabled = true`,
      `initialSort` is ignored (warning is logged).
    - Sorting: `state.setSort(column, order?)`; current `state.sort`.
    - Grouping: `state.groupBy(column)` to enable grouping; `state.groupBy(null)` to disable.
    - Column order/size: `state.setColumnOrder(order)`, `state.resizeColumn(column, Set/Reset)`,
      `state.setColumnWidths(map)`.
    - Auto-width recalculation: `state.recalculateAutoWidths()` to manually recompute column
      widths based on current content measurements. Useful for deferred/paginated data loading where initial auto-width
      calculation happened on empty data.
    - Filters: `state.setFilter(column, TableFilterState(...))`; current per‑column `state.filters`.
    - Selection: `state.toggleSelect(index)`, `state.toggleCheck(index)`, `state.toggleCheckAll(count)`,
      `state.selectCell(row, column)`.
- **Settings and geometry**
    - `TableSettings`: `rowReorderEnabled` (new name; deprecated alias `isDragEnabled` is still supported),
      `autoApplyFilters`, `autoFilterDebounce`, `stripedRows`,
      `showActiveFiltersHeader`, `selectionMode: None/Single/Multiple`, `groupContentAlignment`,
      `rowHeightMode: Fixed/Dynamic`, `enableDragToScroll` (controls whether drag-to-scroll is enabled; when disabled,
      traditional scrollbars are used instead), `editingEnabled` (master switch for cell editing mode), `showFooter` (
      enable footer row display), `footerPinned` (pin footer at bottom or scroll with content),
      `enableTextSelection` (wrap table body in `SelectionContainer` to allow text selection; defaults to `false`),
      `showVerticalDividers` (show/hide vertical dividers between columns; defaults to `true`),
      `showRowDividers` (show/hide horizontal dividers between rows; defaults to `true`),
      `showHeaderDivider` (show/hide horizontal divider below header; defaults to `true`),
      `showFastFiltersDivider` (show/hide horizontal divider below fast filters row; defaults to `true`).
    - Row reorder mode notes: while `rowReorderEnabled = true`, sorting and grouping UI is disabled.
      Filtering stays available; fast filters and active filters header continue to work.
    - `TableDimensions`: `defaultColumnWidth`, `defaultRowHeight`, `footerHeight`, `checkBoxColumnWidth`,
      `verticalDividerThickness`, `verticalDividerPaddingHorizontal`.
    - `TableColors`: via `TableDefaults.colors(...)`.

For the full generated API, see the [API Reference](../api/).
