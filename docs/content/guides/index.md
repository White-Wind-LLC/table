# Guides

Task-oriented walkthroughs of the table's features. For the parameter-by-parameter surface, see the
[Core API reference](../reference/core-api.md).

## Data entry

- **[Cell editing](cell-editing.md)** — row-scoped editing with custom edit UI, validation and
  keyboard navigation via `EditableTable`.

## Organizing rows

- **[Data grouping](grouping.md)** — group by any column, with sticky and customizable group headers.
- **[Row blocks](row-blocks.md)** — adjacent rows that render and drag as one unit, with a header
  band, within-block reorder and key-based commit events.
- **[Row reordering](row-reordering.md)** — drag rows to new positions with `onRowMove` and
  `draggableHandle()`.
- **[Embedded tables](embedded.md)** — render at full intrinsic height inside an already-scrollable
  container, and nest a table inside a parent row.

## Filtering

- **[Filters](filters.md)** — the built-in filter types for text, numbers, booleans, enums and dates.
- **[Fast Filters](fast-filters.md)** — inline filtering in a dedicated row below the header.

## Selection

- **[Selection](selection.md)** — the built-in `SelectionMode.Single` / `Multiple` modes.
- **[Checkbox selection](checkbox-selection.md)** — custom checkbox selection sharing state through
  `tableData`.

## Layout & appearance

- **[Row height & auto-width](row-height-auto-width.md)** — dynamic row height and content-measured
  column widths.
- **[Footer row](footer.md)** — a summary row at the bottom, optionally pinned.
- **[Custom header icons](custom-header-icons.md)** — replace the sort and filter icons.
- **[Drag-to-scroll](drag-to-scroll.md)** — pan the content by dragging, tuned per platform.
