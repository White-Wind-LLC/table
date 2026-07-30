# Viewport-Pinned Active-Filters Header — Design

## Goal

Keep the active-filters chips row readable at every horizontal scroll offset. The chips report which
filters are currently applied, so a table wide enough to scroll must not be able to carry them off
screen.

## Motivation

`Table` applies `horizontalScroll(horizontalState)` to the single `Box` that wraps everything it
renders, and `ActiveFiltersHeader` was the first child of the `Column` inside that `Box`, sized with
`Modifier.width(state.tableWidth)`.

Two consequences followed from being scrolled content rather than a sibling of the scroll container:

1. The chips are laid out from the left edge of the **content**, so scrolling a wide table to the
   right translated them past the left edge of the viewport. The row was still there — it rendered as
   an empty strip — and the only way to read the applied filters was to scroll all the way back.
2. The row was as wide as the table, not as wide as the viewport, so the `‹` / `›` overflow buttons in
   `TableActiveFilters` measured against the wrong width and effectively never appeared.

A separate defect lived in the same file: `TableActiveFilters` returns early when no filter is active,
but the surrounding `Column` still emitted its `HorizontalDivider`, leaving a stray rule above the
header of an unfiltered table.

## Non-goals

- The fast-filters row stays scrolled with the columns. Each of its fields belongs to one column and
  has to stay aligned with it; pinning it would break that correspondence.
- No change to `TableActiveFilters` itself — it is public, and consumers place it on their own.
- No horizontal scrollbar. The table still exposes no scrollbar of its own on any platform.

## Approach

Move the header out of the scroll container so it is a sibling of it rather than its content:

```kotlin
Surface(shape, border, modifier) {
    Column {
        if (settings.showActiveFiltersHeader) ActiveFiltersHeader(…)
        Box(modifier = scrollAreaModifier(embedded, innerModifier)) {
            Column { TableHeader(…); body }
            if (showPinnedFooter) PinnedFooterOverlay(…)
        }
    }
}
```

`innerModifier` — which carries `horizontalScroll`, drag-to-scroll and `clipToBounds` — now applies
only to the `Box`, so nothing above it can be translated by the scroll state.

Two width decisions come out of this, both extracted into private helpers so `EditableTable` keeps its
cyclomatic budget:

- `activeFiltersModifier(embedded, tableWidth)` — `fillMaxWidth()` normally, `width(tableWidth)` when
  `embedded`. An embedded table is measured with an unbounded width by the scrolling parent that hosts
  it, so `fillMaxWidth` has nothing finite to fill there.
- `scrollAreaModifier(embedded, innerModifier)` — `weight(1f)` for a bounded table so the scroll area
  claims the height the header left over; the bare modifier when `embedded`, where the table renders at
  its intrinsic height.

`ActiveFiltersHeader` gains a `modifier` parameter, drops its own `state.tableWidth` reads, and returns
early when no filter is active so the divider goes with the chips.

## Testing

`ActiveFiltersHeaderTest` (`runComposeUiTest`) renders a table twice as wide as its viewport, applies a
text filter, scrolls `horizontalState` to the end and asserts the chip is still displayed. The test
fails on the previous layout and passes on this one. A second case asserts no chips row exists while no
filter is active.

## Out of scope

Column resizing at the right edge of the table is a separate change; see
`2026-07-30-last-column-resize-autoscroll-design.md`.
