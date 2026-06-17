# Dynamic row height and auto-width

- Dynamic height: set `rowHeightMode = RowHeightMode.Dynamic`. Use per‑column `rowHeight(min, max)` to hint bounds.
- Auto‑width: call `autoWidth(max?)` in column builder. The table measures header + first batch of rows and applies
  widths once per phase. Double‑click the header resizer to snap a column to its measured max content width.
- Alternatively, use `state.recalculateAutoWidths()` to manually trigger width recalculation based on
  current content measurements (useful for deferred/paginated data loading scenarios).
