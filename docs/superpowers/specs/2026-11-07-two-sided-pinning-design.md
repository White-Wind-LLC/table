# Two-sided column pinning — design

Refs: GitHub #168 ("why only left or right side pin? why not support both side pin")

## Intent

Real tables pin columns on both edges at once: an id/name block at the start and totals or actions at
the end. Today `table-core` pins one block on one side (`pinnedColumnsCount` + `pinnedColumnsSide`).

## Goals

- Pin a start block and an end block simultaneously, from settings and at runtime (state + column menu).
- Source compatible: existing code compiles (with deprecation warnings) and renders exactly as before.
  Ships as a minor release (3.7.0).

## Non-goals / anti-goals

- No per-column pin model (`pinned = End` on a column spec): the pinned blocks stay position based.
- No change to how the scrolling middle, footer, fast filters or resizers behave apart from also
  honouring the second block.
- Must not break the documented layout persistence recipe (`order`, `hidden`, `pinnedCount`).

## Approaches

1. **Two counts, positional model kept (picked).** `pinnedStartCount` + `pinnedEndCount`; old API
   deprecated and mapped until 4.0. Mechanical, compatible.
2. Per-column pin side (AG Grid style). More flexible for consumers, but replaces the positional model
   and the persistence recipe: breaking, a 4.0 change.

The user picked 1.

## Design

### Public API

- `TableSettings`: new `pinnedStartCount: Int = 0`, `pinnedEndCount: Int = 0`.
  `pinnedColumnsCount` / `pinnedColumnsSide` become `@Deprecated("… Removed in 4.0")`. Seeding:
  start = `pinnedStartCount` + (legacy count if legacy side is Start), end likewise.
- `TableColumnsState`:
  - live `pinnedStartCount`, `pinnedEndCount` (mutable state, coerced ≥ 0);
  - `pin(column, side: PinnedSide)`, `canPin(column, side)`, `pinnedSide(column): PinnedSide?`;
  - `isPinned(column)` true on either side; `unpin(column)` works for either side;
  - deprecated `pinnedCount` (get/set) and `pin(column)` / `canPin(column)` operate on the legacy side
    (`settings.pinnedColumnsSide`, default Start), so the persistence recipe keeps working.
- Rule: at least one visible column stays unpinned. If start + end covers every visible column, none is
  pinned (same as the current single-side rule).

### Behaviour

- Column menu: an unpinned column offers "Pin left" and "Pin right" (physical labels, RTL aware); a
  pinned one offers "Unpin". Pin is disabled with the existing "last unpinned" reason.
- Rendering: each block gets its own divider and edge shadow — start edge shadow when scrolled away from
  the start, end edge shadow when content remains past the end. Applies to rows, header, footer, fast
  filters, resizers overlay and viewport math.
- `moveBy` keeps a column within its block (start / middle / end). `show()` lands a column outside both
  blocks; `hide()` on a pinned column unpins it first.

### Around

- Sample app: two steppers ("pinned start N", "pinned end N") replace count + side switch.
- Docs: column-menu guide (state API, persistence recipe), core API reference, CHANGELOG (Unreleased).
- Tests: two-sided cases in `PinnedColumnStateTest`, `TableColumnsStatePinTest`, move/show/hide/divider
  tests and `ColumnMenuModelTest`.
- Verification: run the JVM desktop sample and drive it with the `ui-drive` skill (pin both sides via
  sample settings and column menu, scroll horizontally, check both blocks stay put).

## Left to the builder

Naming of new menu entry ids, internal helper shapes, exact deprecation messages.
