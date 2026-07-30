# Resize Auto-Scroll at the Right Edge — Design

## Goal

Make the last column resizable in one gesture. Its boundary sits at the right edge of the content, so
widening it used to be invisible and would run out of pointer travel within a few pixels.

## Motivation

`ColumnResizersOverlay` places a grab strip at each column boundary inside a `Box` of
`state.tableWidth`, which lives in the horizontally scrolling content. The strip works — a drag on the
last boundary did resize the column — but nothing followed it:

- Widening moves the boundary further right in *content* coordinates while `horizontalState.value`
  stays where it was, so the growth happens beyond the right edge of the viewport. The visible width
  of the column does not change and the gesture reads as broken.
- The pointer can only travel from the boundary to the edge of the window. When the table is scrolled
  to the end that is a handful of pixels, and nothing at all when the table reaches the window edge.
- After releasing, the boundary is off screen by however much the column grew, so the strip cannot be
  grabbed again without scrolling right first.

The practical workaround was to narrow the neighbouring columns until the last one had room, which is
what the behaviour looked like from the outside: the last column "cannot be resized".

## Non-goals

- No trailing gutter after the last column. Reserved empty space would give a bounded amount of extra
  travel and change how every table looks at rest; the scroll follows the boundary instead.
- No change to `state.tableWidth`, which stays the sum of column widths and dividers.
- No change to the resize model: widths are still accumulated raw deltas coerced to `ColumnSpec.minWidth`.

## Approach

Two rules, both driven from the resize gesture itself.

**The boundary stays inside the viewport.** After every applied delta, `resizeScrollPullback` returns
how far the boundary overshot the right edge, and that much is handed to
`horizontalState.dispatchRawDelta`. The scroll range grows with the column, so the pull-back is always
available. This is what makes the growth visible: the boundary stops at the edge and the columns to
its left slide out of view instead.

**A pointer with nowhere to go keeps the column growing.** `shouldGrowAtResizeEdge` reports whether the
pointer is pushing right within 24dp of the right edge of the viewport. While it is, a `withFrameNanos`
loop adds 200dp per second to the column and pulls the boundary back each frame, so holding at the edge
grows the column continuously — the table scrolls under a pointer that has stopped moving. The loop is
keyed on that condition, so it runs only while the pointer is parked at the edge and stops the moment
the pointer moves away or the gesture ends.

The direction is part of the condition, not an afterthought. Grabbing the last boundary puts the pointer
inside the edge zone straight away, so a rule that looked only at position would read a small pull to the
left as a pointer out of travel and widen the column the user is trying to narrow. A negative drag amount
means shrinking, and shrinking is never out of room.

Getting the pointer's position is why the strip moves from `Modifier.draggable` to
`detectHorizontalDragGestures`: `draggable` reports deltas only, and a parked pointer produces none.
The pointer's viewport X is derived from the strip's own geometry — boundary, reach and scroll offset —
rather than from layout coordinates, so no extra measurement plumbing is needed.

That swap carries one obligation. `pointerInput` is keyed on the column, so its block is composed once
and never sees a later composition; the boundary and the current width a gesture starts from are read
through `rememberUpdatedState`. Captured directly, every drag after the first would restart from the
width the column had when the table was first laid out — the column would snap back before moving.

`ColumnResizersOverlay` takes `horizontalState` and delegates each boundary to a private `ResizeHandle`,
which owns the gesture and its `ColumnResizeDrag` state. The drag tracks the boundary in content pixels
from where it started, because the `cumulativeX` composed alongside it goes stale as the column grows.

**Settling.** Each pull-back dispatched during the drag is clamped by the scroll range the last
*measured* width allowed, and the width applied on the final event is measured only afterwards. The
released boundary can therefore sit a few pixels beyond the edge with no further event to correct it —
and a boundary beyond the edge takes its grab strip out of reach, so the next drag has nothing to grab.
Ending the gesture keeps the pull-back running for a few frames against the freshly composed boundary
until nothing is left to chase.

**Grab strips.** Inner boundaries keep their 3dp either side of the divider. The last one has no content
to its right to hang over, so it takes that width back on the left and reaches 6dp in — the same strip
width as every other boundary, rather than half of it.

## Testing

- `ColumnResizeScrollTest` covers both pure functions: a boundary inside, exactly at, and past the right
  edge, a boundary scrolled out to the left, the edge-zone threshold, a pointer beyond the edge, one held
  still, one pulling left, and an unmeasured viewport. The edge rule is tested here rather than through
  the UI because a parked pointer keeps the frame loop running, and a composition that never idles cannot
  finish a Compose UI test.
- `ColumnResizeTest` (`runComposeUiTest`) scrolls a table wider than its viewport to the end, drags the
  last boundary right with mouse input, and asserts three things: the column grew past its starting
  width, `horizontalState.value` followed, and nothing is left to pull back once the gesture settled.
  The first two fail without the pull-back, the third without the settling pass. A second case drags one
  boundary twice and asserts the column keeps growing, which fails when the gesture reads its starting
  geometry from the composition that first laid the strip out.

## Out of scope

The table still renders no scrollbar of its own, on any platform; horizontal scrolling remains a wheel
or drag gesture.
