# Divider-Aware Column Geometry — Design

## Goal

Make every horizontal measurement agree with what the table paints, so a column boundary is where the
resize strip is. Hiding the vertical dividers used to move the two apart.

## Motivation

A column boundary is computed in three places, and each one added `dimensions.dividerThickness` after
every column unconditionally:

- `TableState.computeTableWidth`, which the rows, header, footer and scroll range are all sized from,
- `ColumnResizersOverlay`, which places the grab strips,
- `ensureColumnFullyVisible`, which scrolls a column into view.

`HeaderCell` and `TableCell` emit that divider only when `TableSettings.showVerticalDividers` is on.
With it off the arithmetic counts dividers nobody draws, and the error accumulates: the strip for column
*i* lands `i × dividerThickness` to the right of the boundary it belongs to. On a wide table the later
strips sit on top of the next column's header — aiming at the visible boundary grabs nothing — and the
scroll range carries a tail of empty space that is never rendered.

Two settings escape the rule and are drawn whatever the flag says: the divider at the edge of a pinned
block, either between the last left-pinned column and the scrolling ones, or before the first
right-pinned one.

## Non-goals

- No change to what is drawn. This is arithmetic catching up with the painting, not a visual change.
- No change to `TableSettings` or to any public signature.

## Approach

One internal function, `dividerWidthAfterColumn(columnIndex, totalVisibleColumns, settings, dimensions)`,
returns the width the divider after a column actually occupies, mirroring the conditions the header and
rows render under. All three call sites fold it in instead of a bare `dividerThickness`.
`computeTableWidth` collapses to a single `foldIndexed` in the process — the pinned-column special case
lives in the shared function now.

A second defect surfaced while driving this: a resize gesture kept writing into a `TableState` that had
been replaced. `rememberTableState` keys on `settings`, so changing any setting hands the table a new
state holder, while `Modifier.pointerInput` is keyed on the column alone and its block outlives the
composition that started it — the captured `onResize` went on updating the discarded state. Every value
the gesture needs is now read through `rememberUpdatedState`: the boundary and width it starts from, and
each callback.

## Testing

- `DividerWidthTest` covers the shared function: dividers shown, hidden, a left-pinned block, a
  right-pinned block, and pinning every column (which pins none).
- `ColumnGeometryTest` drives the table with dividers hidden — a drag at the visible boundary of a late
  column must resize it, the last column must still resize, and scrolling a column into view must stop
  at its real edge. All three fail on the previous arithmetic.
- `ColumnResizeTest` gains a case asserting a drag lands on the state that replaced the one composed
  first. It passes either way, so it documents the contract rather than covering the defect; the
  stale-callback fix was confirmed by instrumenting the gesture and watching writes land in three
  different state instances before, and one after.
