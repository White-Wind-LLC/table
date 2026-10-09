# Dynamic row height and auto-width

- Dynamic height: set `rowHeightMode = RowHeightMode.Dynamic`. Use per‑column `rowHeight(min, max)` to hint bounds.
- Auto‑width: call `autoWidth(max?)` in column builder. The table measures header + first batch of rows and applies
  widths once per phase. Double‑click the header resizer to snap a column to its measured max content width.
- Auto‑width only sizes columns without an explicit width: widths from `initialWidths`, `state.columns.setWidths(...)`,
  `state.columns.resize(...)` or the resizer are kept. `state.columns.explicitWidths` lists them, without the widths
  auto‑width chose.
- Alternatively, use `state.columns.recalculateAutoWidths()` to manually trigger width recalculation based on
  current content measurements (useful for deferred/paginated data loading scenarios). The recalculation also
  replaces explicit widths of auto‑width columns.

## Large font scale

Above 1.0× system font scale, `rowHeight`, `headerHeight`, `footerHeight` and `fastFilterRowHeight` from
`TableDimensions` grow by as much as one line of their text grows, so single-line text is not clipped. At 1.0× and
below they are used as given. Group headers use the grown `rowHeight`.

Set `scaleWithFontSize = false` to keep the heights exactly as given:

```kotlin
rememberTableState(
    columns = columns,
    dimensions = TableDefaults.standardDimensions().copy(scaleWithFontSize = false),
)
```

Content taller than one line in the header, footer or group headers is still clipped; use `RowHeightMode.Dynamic`
for body rows that wrap.
