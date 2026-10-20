# Unresolved filter conditions — design

Issue: [#157](https://github.com/White-Wind-LLC/table/issues/157). Follow-up to
[#152](https://github.com/White-Wind-LLC/table/issues/152) (filter tree, released in 3.3.0).

## Intent

An app loads a saved filter into `TableState.setFilterExpression`. That filter can hold a condition the app
can no longer express as `TableFilterNode.Condition<C>`:

- a field code that maps to no value of the app's column enum `C` (renamed or removed field);
- a known field whose stored predicates do not convert to a `TableFilterState` (no filter type, an operator
  the app cannot map, predicates the conversion would drop).

Today such a condition cannot enter the tree. The app keeps it outside, and that breaks as soon as the user
edits a group in the popover: `removeAt` collapses a one-child group and shifts sibling indices, so the app
cannot put the outside condition back where it was. `(A OR B OR U)` minus `B` becomes `A`, and `U` ends up
AND-ed at the root. The filter as a whole still looks valid, so the app sends it on and the server answers
"Unknown filter field". The user cannot see what to remove; only "Clear all" helps.

The first consumer is the WMS client (YouTrack WMS-2255, design
`docs/superpowers/specs/2026-10-20-wms-2255-filter-tree-design.md` in `wms-client`, branch
`wms-2255-filter-tree`). It makes the table's tree the source of truth, maps both cases above to the new leaf
with the raw predicates as payload, and refuses to load a list with a filter that holds one.

The fix: **a leaf the table carries but never interprets, kept in place by every edit, visible and removable
wherever it sits, and reported by `TableState` so the app can refuse the filter as a whole.**

## Goals

1. A new tree leaf `TableFilterNode.Unresolved(key: String, payload: Any? = null)`, not keyed by `C`.
2. The table never interprets, rewrites, reorders or drops it. Every tree operation (remove a sibling, collapse
   a group, switch AND/OR, toggle NOT elsewhere, header edits through `state.filters`) leaves it where it was,
   with the same `payload` instance.
3. It is visible and removable: a warning chip with × at the top level, a warning line with × inside the group
   popover. × removes exactly that leaf.
4. Apps can title it: `TableFilterChipStyle.unresolvedTitle(key, payload)`. The WMS client uses it to show a
   known field's localized name; for an unknown code it returns null and the raw `key` is shown.
5. `TableState` reports the unresolved leaves, and the active filters row says the filter cannot be applied
   until they are removed. All new text goes through `StringProvider`.
6. `TableEmptyScope.isFiltered`, `clearFilters()` and "Clear all" include unresolved leaves.

## Non-goals

- The table filtering rows itself. It does not today (the app or the server evaluates the tree), so "skip the
  table's own filtering while unresolved" reduces to the reported state in goal 5. No evaluator is added.
- Repairing or remapping an unresolved leaf (e.g. picking a replacement column). It can only be removed.
- Creating unresolved leaves from the UI. Only the app puts them in, through `setFilterExpression`.
- Serializing the tree or the payload.

## Anti-goals (a failure even if it works)

- Any edit that moves an unresolved leaf, merges it into another group, or changes its payload.
- An unresolved leaf the user cannot see on screen: at the top level, inside a group's popover, or hinted on
  the group chip that contains it.
- The row looking like a normal, working filter while an unresolved leaf exists.
- A flat-only app (never sets a tree) noticing any change.

## Decisions

These were agreed with the user (playback before writing):

- The leaf is `public data class Unresolved(val key: String, val payload: Any? = null) : TableFilterNode<Nothing>`.
  `TableFilterNode` is `out C`, so it fits in any `Group<C>` / `Not<C>`.
- Its chip has the same look as the existing `Unknown` chip (`TableFilterChipColors.unknown`, the warning icon
  with `UiString.FilterUnknownField` as its description, × only).
- The title hook is `TableFilterChipStyle.unresolvedTitle: ((key: String, payload: Any?) -> String?)?`; null or
  a null result shows `key`.
- `TableState` reports it (exact shape below is my call); the row shows a localized "remove them to apply"
  message.
- `state.filters` (the per-column view used by headers) does not see unresolved leaves, as it does not see
  groups.
- Release as 3.4.0. Adding a branch to the public sealed `TableFilterNode` (and `TableFilterChipNode`) breaks
  exhaustive `when`s in apps at compile time; this is accepted for a minor release and called out in the
  changelog.

My calls (the builder may refine the details but not the behavior):

### Tree

- `TableFilterNode<*>.isActive()` is `true` for `Unresolved`. An unresolved leaf always counts as a filter that
  is set: it shows in the row (the row skips inactive nodes), it makes `isFiltered` true, and "Clear all"
  removes it.
- `FilterTree.kt`: `nodeAt` treats it like a `Condition` (a leaf, no children). `removeAt`, `replaceAt`,
  `setOperatorAt`, `toggleNotAt` need no other change; tests prove the acceptance cases below.
- New public extension `TableFilterNode<*>.unresolvedLeaves(): List<TableFilterNode.Unresolved>`, in tree order,
  so an app can check a tree before or without a `TableState`.
- New `TableState.unresolvedFilters: List<TableFilterNode.Unresolved>`: `filterExpression.unresolvedLeaves()`,
  readable in snapshots (observe with `snapshotFlow { state.unresolvedFilters.isEmpty() }`). An empty list means
  the expression is resolved. No separate boolean.
- `Unresolved` equality includes `payload`. KDoc says the payload should have value equality (or be the same
  instance), because the row and popover compare nodes to keep a chip open.

### Chips

- New `TableFilterChipNode.Unresolved<C>(key, payload, title, placement, strings, onClear)` with an internal
  constructor, like `Unknown`. `DefaultFilterChip` handles it as it handles `Unknown`: not clickable, warning
  icon + `title`, `unknown` colors, × labelled `strings.removeFilterLabel(title)`.
- `TableFilterChipStyle` and `TableFilterChipDefaults.style` gain `unresolvedTitle` after `unknownTitle`
  (default null). This changes the JVM signatures of the data class constructor and `copy`; noted in the
  changelog as was done for `ColumnSpec` in 3.2.0 / 3.3.0.
- **Row.** A top-level unresolved leaf gets its own chip. `chipAtRemoved` and keyboard focus treat it like any
  other chip.
- **Group chip.** A group with an unresolved leaf anywhere under it is never a "shared column" chip. Its text
  (column titles) includes the unresolved leaves' titles in tree order, and the chip shows the warning icon
  before its badges so the problem is visible without opening it. Its colors stay the group colors. Its
  accessibility description reads unresolved leaves with `strings.unresolvedFilterDescription(title)`.
- **Custom chips.** `TableFilterChipNode.Group` gains `public val hasUnresolved: Boolean` (true when any leaf
  under the group is unresolved), so a `TableFilterChipStyle.chip` renderer can give the same hint.
- **Popover.** An unresolved leaf is a line drawn with the `Unresolved` chip node (`placement = Tree`). It cannot
  be drilled into and offers no NOT toggle; × removes exactly that path, with the popover's usual focus and
  collapse handling. A group left with one child collapses as today; the unresolved leaf stays where it was.
- **`Not(Unresolved)`.** An app may pass a negated unresolved leaf. The table never changes the meaning around
  it: wherever a NOT toggle would apply to a `Not` whose child is unresolved, the negation is shown (the `NOT`
  badge) but cannot be toggled. At the top level it is a NOT chip over a leaf, as for a condition today, and its
  popover shows the leaf with ×; removing the leaf removes the emptied `Not` (existing `removeAt` behavior).
- **Where the user sees it.** The library shows unresolved leaves only in the active filters row. An app that
  hides the row (`showActiveFiltersHeader = false`, no `TableActiveFilters`) hides every chip, not just these,
  and must check `unresolvedFilters` itself; the KDoc of `unresolvedFilters` and the docs say so. `Table` does
  not force the row on.
- **Deprecated row without popovers.** The deprecated `TableActiveFilters` overload without `tableData` has no
  popovers. There a nested unresolved leaf goes away only with its whole group (group ×) or "Clear all". This
  is accepted: the overload is deprecated and removed in 4.0; its KDoc gets one line saying so.

### Blocked state in the row

- While `state.unresolvedFilters` is not empty, the active filters row (inside `Table` and in standalone
  `TableActiveFilters`) shows one line under the chips: the warning icon (decorative) and
  `UiString.FilterUnresolvedBlocked`, default "Filter contains unknown fields — remove them to apply", in the
  error color (`MaterialTheme.colorScheme.error`). The line is a polite live region so screen readers announce
  it when it appears. It disappears when the last unresolved leaf is removed.
- No other styling of the row changes.

### Empty state

- `TableEmptyScope.hasUnresolvedFilters` (default `false` on the interface, so other implementations keep
  compiling). The default empty content shows the warning icon and `UiString.FilterUnresolvedBlocked` in place of
  "No results" while it is true, and keeps the "Clear filters" button.

### Strings

- `UiString.FilterUnresolvedBlocked` (new key, English default above, added to every locale default table the
  library ships, as other keys are).
- `StringProvider.unresolvedFilterDescription(title: String): String = "Unknown field: $title"` — an
  unresolved leaf read in a group's accessibility sentence.
- Reused: `UiString.FilterUnknownField` (icon description), `removeFilterLabel` (×).

### Sample

- `PersonFilterMatcher` handles the new branch. While `unresolvedFilters` is not empty the sample shows no rows
  at all, and the table's empty state says the filter contains unknown fields — the pattern the KDoc recommends.
  Rows appear again once the filter is valid.
- A new sample action next to the nested filter demo sets
  `(A OR B OR Unresolved("legacyField", payload)) AND C`, and `unresolvedTitle` in `MainTable` gives one
  payload a readable title, to show both title paths. The existing nested demo stays a working filter.

## Acceptance

- `setFilterExpression((A OR B OR Unresolved("old")) AND C)` shows a group chip (with the warning icon and `old`
  among its titles) and a `C` chip. The popover lists `A`, `B` and `old`; `old` has the warning look and ×.
- In the popover, removing `B` keeps `(A OR old)`; removing `old` keeps `(A OR B)`.
- Removing `A` and then `B` leaves `old` alone at the top level, as its own chip.
- Switching the group to AND, toggling NOT on the group, and editing `C` from its header all leave `old` in
  place.
- While `old` is present, `state.unresolvedFilters == listOf(old)` and the row shows the blocked line. After it
  is removed, the list is empty and the line is gone.
- The `payload` of an unresolved leaf is the same instance after every edit above.
- `isFiltered` is true for a tree holding only an unresolved leaf; `clearFilters()` and "Clear all" remove it.
- An app that never sets a tree sees no change (existing tests pass unchanged).

## Verification

- Common unit tests for the tree operations and `unresolvedLeaves` (`FilterTreeTest`), state reporting
  (`TableStateFilterExpressionTest`), row and popover UI (`ActiveFilterTreeRowTest`, `FilterGroupPopoverTest`,
  `FilterChipItemsTest`), strings (`FilterChipStringsTest`), empty state (`TableEmptyStateTest`).
- Run per the project's targeted Gradle tasks (quality check + compile + `jvmTest`), not a full build.

## Docs and release

- KDoc on every new public symbol.
- `docs/content/modules/table-core.md` and `docs/content/reference/core-api.md`: a short section on unresolved
  leaves and the "refuse the filter while `unresolvedFilters` is not empty" pattern.
- CHANGELOG entry under a new unreleased 3.4.0 section: Added (leaf, state, hook, strings), Changed (sealed
  branches; `TableFilterChipStyle` JVM signatures). Version numbering and tagging stay the user's step.

## Left to the builder

- Internal structure of chip items and texts (e.g. how `FilterGroupSummary` carries unresolved titles).
- Exact layout and spacing of the blocked line, within the row's existing paddings.
- Whether `unresolvedFilters` is cached (`derivedStateOf`) or computed on read.
