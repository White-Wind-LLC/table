# table-core

The core table module (`table-core`) provides rendering, header, sorting, column resize and reordering, filtering,
row selection, i18n, styling/customization, and dynamic or fixed row height.

## Modules

- `table-core`: core table (rendering, header, sorting, column resize and reordering, filtering, row selection, i18n,
  styling/customization; dynamic or fixed row height).
- `table-format`: dialog and APIs for rule‑based conditional formatting for cells/rows.
- `table-paging`: adapter on top of the core table for `PagingData` (`ua.wwind.paging`).

## Key features

- Header with sort/filter icons (customizable via `TableHeaderDefaults.icons`).
- Per‑column sorting (3‑state: ASC → DESC → none).
- Data grouping by column with customizable group headers and sticky positioning.
- Footer row with customizable content per column (totals, averages, summaries); supports pinned and scrollable modes.
- Drag & drop to reorder columns in the header.
- Row reordering powered by Reorderable with custom drag handles inside cell content.
- Row blocks: adjacent rows sharing a block id render and drag as one unit, with an optional header band above the
  block and a single key-based commit event per drag.
- Column resize via drag with per‑column min width.
- Filters: text, number (int/double, ranges), boolean, date, enum (single/multi; IN/NOT IN/EQUALS) with built‑in
  `FilterPanel`.
- Active filters header above the table: a chip opens its filter, × removes it, "Clear all" resets all. The filter
  is a tree of AND/OR/NOT groups (`state.filterExpression`, `state.setFilterExpression(root)`); the row shows one
  chip per top-level node. A condition is a plain chip, a group or NOT is a summary chip ("ANY 2 · Type, Qty"), an
  OR of one column is a merged chip ("Status: Active or Pending"), and a key with no column in `columns` is an
  unknown-key chip (warning colors, × only). A group chip opens a popover: Any/All switch, NOT toggle, × on any
  node, a group left with one child dissolves, and a condition drills into its filter panel (‹ or Escape goes
  back). Nothing can be added from the row. `state.filters` is a view of the root's simple top-level conditions:
  header edits and `setFilter` touch only those.
- Unresolved conditions: a saved condition the app cannot map to a column (a renamed or removed field) goes into
  the tree as `TableFilterNode.Unresolved(key, payload)`. The table never interprets it and keeps it, with its
  payload, in place on every edit; it shows as a warning chip or popover line with × only, and the row says the
  filter is not applied until it is removed. Check `state.unresolvedFilters` and do not apply or send the filter
  while it is not empty; show no rows meanwhile, and the default empty state says why. The row is the only place
  the table shows the conditions themselves.
- Chip customization: a column draws the content of its chips with `filterChipContent(leadingIcon, label)`;
  `TableFilterChipDefaults.style(colors, unknownTitle, unresolvedTitle, chip)` passed as `filterChips` to `Table` or
  `TableActiveFilters` sets colors, titles of unknown keys and unresolved conditions, and a full chip renderer, which can call
  `DefaultFilterChip` to keep parts of the default. `activeFilterChip`, `TableFilterChipContext` and
  `DefaultActiveFilterChip` are deprecated and removed in 4.0.
- Row selection modes: None / Single / Multiple; optional striped rows.
- Embedded (nested) tables via the `embedded` flag and `rowEmbedded` slot for building master–detail layouts inside
  a single table.
- Extensive customization via `TableCustomization` (background/content color, elevation, borders, typography,
  alignment). Outer table border is configurable via `border` parameter (custom stroke or disabled entirely).
- i18n via `StringProvider` (default `DefaultStrings`). Dates and numbers in filter fields and chips follow the
  platform's format locale (region settings), not the UI language. On desktop the JDK reads macOS English with the
  Ukraine region as en-US and formats `10/08/2026`; set the format locale in the app's JVM arguments, e.g.
  `-Duser.language.format=uk -Duser.country.format=UA`, to get `08.10.2026`. Don't use
  `-Djava.locale.providers=HOST`: on macOS it returns full day names for short styles and breaks the date picker.
  Override `formatDate` / `formatNumber` for a fixed format:

  ```kotlin
  object AppStrings : StringProvider {
      @Composable override fun get(key: UiString): String = DefaultStrings.get(key)

      @Composable override fun formatDate(date: LocalDate): String = date.toString() // ISO 2026-10-08
  }
  ```
- Targets: Android / JVM (Desktop) / JS (Web) / iOS (KMP source sets present; targets enabled via project conventions).
- Pinned columns with configurable side (left/right) and count.
