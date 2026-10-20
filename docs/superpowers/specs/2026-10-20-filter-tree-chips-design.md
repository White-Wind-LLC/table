# Filter tree in the active filters row — design

Issue: [#152](https://github.com/White-Wind-LLC/table/issues/152)

## Intent

`TableState.filters` is a flat `Map<C, TableFilterState<*>>`: one condition per column, joined with
AND. Apps whose users build richer filters (OR groups, NOT, several conditions on one column) keep
the real tree outside the table. Three things go wrong:

1. Echoing the table state back to the app flattens `(A OR B) AND C` into `A AND B AND C`.
2. The chips row shows the tree as a flat list, as if every condition were required.
3. A condition whose key has no `ColumnSpec` gets no chip, so the user can neither see nor remove it.

This work puts the tree into `TableState`, shows it faithfully in the chips row, and lets the user
change or remove what is already set. The chips row is for **seeing and quickly changing existing
filters, not for building new ones.** This design departs from the issue where brainstorming found
a better shape; the issue's acceptance criteria are restated below in their adjusted form.

## Goals

- The tree lives in `TableState` and round-trips without loss.
- The row shows one chip per top-level node and makes complex nodes readable without opening them.
- From the row, the user can: change any condition (at any depth), remove any node, switch a
  group's operator (Any/All), and toggle NOT on a group.
- Conditions on unknown keys are always visible and removable.
- Apps can customize chips: the common way by parts, and a full replacement when they need it.
- Apps that never set a tree see no visual or behavioral change.

## Non-goals

- Adding conditions or groups, from the row, the popover or anywhere else in the library.
- A separate full-screen condition editor or an "Edit conditions" entry. The group popover replaces
  it.
- Evaluating or serializing the tree. The app or the server does that, as today.
- Header cells showing OR structure. A header edits only its column's simple top-level condition.

## Anti-goals

- Several identical "Complex condition" chips that say nothing until opened.
- A flat-only app noticing anything, including a change to how `state.filters` behaves.
- A custom chip silently breaking keyboard navigation or the popover.
- Any edit that flattens or reorders the tree.

## Decisions taken during brainstorming

### Row: one chip per top-level node (option D)

The root is an AND group. The row shows its children in order, one chip each, after "Clear all".

| Top-level node | Chip | Tap | × |
|---|---|---|---|
| Simple condition on a known column | today's chip, `Title: text` | that column's panel, anchored under the chip, as today | resets it as today |
| OR group whose children are all simple conditions on **one** column | reads like a plain chip: `Status: Active or Pending` | the group popover (a panel edits one condition, so it cannot edit two) | removes the group |
| Any other group, or a NOT | summary chip: `NOT` tag when negated, an operator badge with the item count (`ANY 2`, `ALL 3`), the distinct column titles of all leaves in tree order, and `▾` | the group popover | removes the node |
| Condition on an unknown key | warning colors, `⚠` + raw key (or the app's title), × only | nothing | removes it |

The summary chip names columns, not values. Its accessibility description reads the whole node as
a sentence (e.g. "Any of: Type equals A and Qty greater than 5, or Vip equals yes"), built from
`StringProvider` strings.

### Group popover with drill-in (option 1)

Tapping a group chip opens a popover anchored under it, in the same place a column panel opens
today. It shows the node as an indented tree:

- Each group line has an **Any / All** segmented switch and a **NOT** toggle. Both apply at once and
  update the row chip. The root (the row itself) is always "all of" and has no switch.
- Each condition is a chip. A tap **drills in**: the column's filter panel replaces the tree inside
  the same popover, with a header `‹ Column` and a small path (`Any › All`). `‹`, Esc or system back
  returns to the tree. One popover at a time; this also works on narrow screens.
- × on any line removes that node at once.
- A condition on an unknown key appears in the tree with warning colors and × only; it cannot be
  drilled into.
- No add buttons anywhere.

Mockups: `.superpowers/brainstorm/*/content/full-flow.html` (not committed; described fully above).

### Structure edits that are allowed

Allowed: change a condition's state, remove any node, switch a group's operator, toggle NOT on a
group. Not allowed: creating conditions, groups or branches.

### Customization: parts by default, one full-replacement hatch (option C)

- **Parts.** The library owns the chip shell everywhere (row and popover): click, focus, keyboard
  order, the × and its localized label, the popover and drill-in. Apps fill in:
  - per column: the content of that column's condition chip (leading icon, label; may be
    interactive, like the sample's rating stars, through `ctx.onChange`). Drawn in both the row and
    the popover.
  - per table: colors and shape for each chip kind (condition, group, negated group, unknown), and a
    readable title for unknown keys (`unknownTitle(key)`).
- **Full replacement.** An optional table-level renderer receives a sealed node context and draws
  any chip in the row and in the popover. A public default renderer, with overridable parts, is
  always available to delegate to. The renderer never replaces the popover or its navigation.
- **Compatibility does not need to be kept.** `ColumnSpec.activeFilterChip` and
  `TableFilterChipContext` are deprecated, removed in 4.0. Until then `activeFilterChip` keeps
  working as a full replacement for that column's condition chips.

## Your call (decided by me during brainstorming, flagged for review)

- Group dissolving: a group left with one child is replaced by that child; an empty group is
  removed. When the dissolved group was top-level and its child is a simple condition, it becomes a
  plain chip and the popover closes.
- "Clear all" clears the whole tree.
- A NOT wrapping a single condition renders as a line `NOT [chip]` whose toggle unwraps it; leaves
  themselves have no NOT toggle (keeps the tree uncluttered).
- While a condition's panel is open (row or drill-in), its chip stays even if the user edits it down
  to no value, as today.
- **Empty conditions.** Top-level simple conditions keep today's semantics: × and "Clear all" reset
  them to "first constraint, no value" (they stay in `filters` and in the tree as inactive
  conditions, exactly as today). Inside a group, a condition is a node: × removes it, and a leaf left
  with no value when its drill-in is left (‹, Esc, closing the popover) is removed, after which the
  dissolve rule applies. So "Clear all" = reset every simple top-level condition as today + remove
  every complex top-level node. Inactive conditions are skipped by chips and summary counts, as
  today; apps ignore them through `isActive()`, as today.
- **Other "is filtered" signals.** `TableEmptyScope.isFiltered` and `clearFilters()` look at the whole
  tree (an active complex node counts; `clearFilters()` behaves like "Clear all"), otherwise a table
  emptied by a group shows "No data" and cannot be cleared. The header cell's filter indicator and
  the column menu's clear-filter item stay bound to the column's simple top-level condition, matching
  "a header edits only its column's simple top-level condition". A column used only inside a group
  shows no header indicator.
- **Custom content on the merged same-column OR chip.** Per-column chip content and the deprecated
  `activeFilterChip` apply only to single-condition chips (top-level and in the popover). The merged
  chip (`Status: Active or Pending`) is a group: it uses the library's text, and the full-replacement
  renderer receives it as a group node that also carries the shared column.
- **Non-AND root.** `setFilterExpression` accepts any node; a root that is not an AND group is wrapped
  as the single child of an AND root. Reading back always returns an AND root; the tree is equivalent,
  and this is documented on the function. An AND root is stored as given.

## Constraints

- Kotlin Multiplatform / Compose Multiplatform, `table-core` `commonMain`. Material 3 components
  (`InputChip`, `SegmentedButton`, `FilterChip` or equivalent), existing `TableIcons`, existing motion
  specs (`rememberTableMotionSpecs`), RTL already supported by the row.
- Every new label is a new `UiString` key with a default English string: operators (Any / All /
  "of these"), NOT, the group summary sentence pieces, back, the × descriptions naming what each
  removes ("Remove group Type, Qty, Vip", "Remove filter Qty"), the unknown-key description.
- Keyboard: the popover has a logical focus order matching the visual tree; Esc goes back from a
  drill-in, then closes. Focus returns to the chip that opened it.
- Both entry points share one implementation: `TableActiveFilters` and the in-table header
  (`TableSettings.showActiveFiltersHeader` → `ActiveFiltersHeader`).
- Detekt / compose-rules and the project's quality checks pass; public API gets KDoc.

## State model

From the issue, kept:

- A sealed node type, e.g.
  ```kotlin
  public sealed interface TableFilterNode<out C> {
      public data class Condition<C>(val column: C, val state: TableFilterState<*>) : TableFilterNode<C>
      public data class Group<C>(val operator: Operator, val children: List<TableFilterNode<C>>) : TableFilterNode<C>
      public data class Not<C>(val child: TableFilterNode<C>) : TableFilterNode<C>
  }
  ```
- `TableState` holds the root (an AND group), with `setFilterExpression(...)` and an observable
  read of the current tree (snapshot-backed, so `snapshotFlow { state.filterExpression }` works).
  Every user edit (chips, popover, header) produces a new tree; it is never flattened.
- `filters` stays and behaves exactly as today for flat-only apps. It is the view of the root's
  simple top-level conditions: writing to it (`setFilter`, or direct map writes, which apps do
  today) adds/replaces/removes that column's top-level condition and leaves the rest of the tree
  intact. Header cells read and write through it.

## Acceptance

- Apps that never set an expression see no visual or behavioral change (existing tests pass
  unchanged, apart from the deprecation).
- With `(A OR B) AND C`, A and B on different columns, the row shows one summary chip and a `C`
  chip; editing `C` in the header leaves `(A OR B)` intact.
- With `(Status = x OR Status = y)`, the row shows `Status: x or y`; a tap opens the popover.
- × on a summary chip removes the whole node.
- In the popover, removing one leaf of an OR group emits a tree, never flattened; a group left with
  one child dissolves.
- Switching Any/All and toggling NOT in the popover emit the expected tree.
- Drilling into a leaf, changing it and going back emits a tree with only that leaf changed.
- A condition on an unknown key shows a chip at top level and inside the popover; × removes it; it
  never opens a panel.
- With only a complex node set, the empty state reports filtered results and its `clearFilters()`
  removes the node.
- A custom full-replacement renderer draws chips in the row and the popover; the popover still
  navigates; the default renderer can be delegated to.
- The deprecated `activeFilterChip` still draws its column's chips.
- The sample app shows a nested filter and the popover, and its filtering honors the tree.

## Left to the builder

- Exact public names (`filterExpression`, `setFilterExpression`, the style/renderer types, the
  `ColumnSpec` builder function for chip content, where the table-level chip settings are passed to
  `Table` and `TableActiveFilters`), as long as they fit the existing API's style.
- The internal representation that keeps `filters` and the tree in sync, including how a simple
  top-level condition is ordered relative to complex nodes, and what `filters` shows when the root
  holds two simple conditions on one column (it must stay a lossless view for flat-only apps, and a
  header must edit one well-defined condition).
- Whether to add a change callback on `Table` in addition to the observable property.
- Popover sizing, max height and scrolling for deep trees; the exact summary sentence grammar.
- The sample's tree evaluator (in the sample, not the library) and how the sample seeds a tree.
- Docs (`docs/content/modules/table-core.md`, `docs/content/reference/core-api.md`) and a CHANGELOG
  entry; the release number is the maintainer's call.
