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
- Column resize via drag with per‑column min width.
- Filters: text, number (int/double, ranges), boolean, date, enum (single/multi; IN/NOT IN/EQUALS) with built‑in
  `FilterPanel`.
- Active filters header above the table (chips + "Clear all").
- Row selection modes: None / Single / Multiple; optional striped rows.
- Embedded (nested) tables via the `embedded` flag and `rowEmbedded` slot for building master–detail layouts inside
  a single table.
- Extensive customization via `TableCustomization` (background/content color, elevation, borders, typography,
  alignment). Outer table border is configurable via `border` parameter (custom stroke or disabled entirely).
- i18n via `StringProvider` (default `DefaultStrings`).
- Targets: Android / JVM (Desktop) / JS (Web) / iOS (KMP source sets present; targets enabled via project conventions).
- Pinned columns with configurable side (left/right) and count.
