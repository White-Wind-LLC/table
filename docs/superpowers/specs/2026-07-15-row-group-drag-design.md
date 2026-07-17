# Dragging a Row Group as a Single Unit — Design

**Date:** 2026-07-15
**Status:** Approved
**Topic:** Let a consumer declare that a range of adjacent rows moves as one draggable unit.

## Problem

Row reordering in `table-core` is single-row only:

```kotlin
onRowMove: ((fromIndex: Int, toIndex: Int) -> Unit)? = null
```

`TableBody` emits one `LazyColumn` item per row and wraps each in `ReorderableItem`, so the drag
handle exposed through `TableCellScope.draggableHandle()` always drags exactly one row.

Consumers that let users merge arbitrary rows into groups need the opposite: a list where some
adjacent rows form a group, the drag handle sits on the group's first row, and dragging it carries
the whole group. Whether a group exists, what it is called, and how it is created or dissolved are
domain concerns owned by the consumer; the only thing the table lacks is the ability to move N
adjacent rows as one unit and report the move in terms of a range.

This is a UX improvement rather than a capability gap: a consumer whose backend relocates the whole
group when the leader row is dragged already gets the right result today — the group simply jumps
into place after the drop instead of following the cursor.

## Goals

- Declare that rows `[i..j]` form one drag unit; dragging any handle inside it moves the block.
- Report moves with row ranges, not single indices.
- Visually separate a group from surrounding rows.
- No behavior change for consumers that do not use groups.
- No fork of, or patch to, `sh.calvin.reorderable`.

## Non-Goals

- Library-side grouping model (`groupBy`) — it groups by column value and draws a header at each
  value change, whereas these groups are arbitrary, header-less, and interleaved with ungrouped
  rows. Group creation / renaming / dissolution stay in consumer UI.
- Row indentation — done with cell padding by the consumer.
- Lifting the "reorder excludes sorting and grouping" rule (`isInteractionLockByRowReorderEnabled`).
- "Drop between neighbours" insert semantics with a gap indicator (see Rejected alternatives).

## Decisions (locked)

| Decision | Choice |
|---|---|
| Drag model | A group is **one lazy item** containing its rows |
| Group declaration | One config object, `TableRowGroups(ranges, onMove, header)` — index ranges, derived from data |
| Move callback | `TableRowGroups.onMove(from: IntRange, to: IntRange)`; `onRowMove` kept unchanged |
| Callback index space | Row indices only — unit indices never leave the table |
| Visual separation | Tinted band around the block; rows keep their own dividers; no horizontal insets |
| Gap value | `TableDimensions.rowGroupSpacing: Dp = 8.dp`, `TableColors.rowGroupContainerColor` |
| Group label | Consumer's `header` slot in the band; the library adds no click handling |
| `groupBy` conflict | `groupBy != null` wins; row units fall back to identity + log warning |
| Embedded tables | Supported, with settle-time callback semantics documented |
| Tests | Apply `ua.wwind.convention.test` to `table-core` (its first tests) |

## Rejected alternatives

**Drag the leader row; let the consumer move the block.** The cheapest-looking option does not work.
`ReorderableLazyCollectionState.onDrag()` picks a target with `shouldItemMove`, invokes `onMove`
mid-drag, then predicts the dragged item's new offset as
`(target.offset + target.size) - dragging.size`. That formula holds only for a two-neighbour swap; if
the consumer moves N rows per callback, the leader jumps to a wrong offset until `layoutInfo`
catches up. Worse, the group's other rows remain separate reorderable items whose centres fall
inside the leader's drag rect, so the library offers them as targets — the group would reorder
against itself. Filtering them out requires `shouldItemMove`, which is a private constructor
parameter not exposed by `rememberReorderableLazyListState`, i.e. a fork. And the non-leader rows
still would not follow the cursor.

**A custom drag engine in `table-core`.** Full control, including insert semantics with a drop
indicator, at the price of reimplementing ~800 lines of edge autoscroll, dynamic heights, RTL, and
pointer handling across four platforms. Not justified by one non-blocking UX task. If insert
semantics are ever needed, they can be built on top of this design without undoing it.

## Architecture

### Row units

A **unit** is either a single row or a `rowGroups` range. `RowUnitIndex` translates between row
indices (the public index space) and unit indices (the `LazyColumn` index space):

```kotlin
@Immutable
internal class RowUnitIndex private constructor(
    private val starts: IntArray,           // group start rows, ascending
    private val ends: IntArray,             // group end rows, inclusive
    private val collapsedBefore: IntArray,  // sum of (len - 1) over groups to the left
    val unitCount: Int,
) {
    fun unitOf(rowIndex: Int): Int          // 3 -> 1  (row 3 inside group 2..4)
    fun rowsOf(unitIndex: Int): IntRange    // 1 -> 2..4
    fun isGroup(unitIndex: Int): Boolean
}
```

Memory is `O(number of groups)`, not `O(rows)` — a 100k-row table pays nothing for having two
groups. Lookups are binary searches over `starts` (row -> unit) and over the derived unit-space
starts (unit -> rows).

With `rowGroups` null or empty the index is **identity**: `unitCount == itemsCount`,
`unitOf(i) == i`, `rowsOf(i) == i..i`, arrays empty, no binary search. Existing consumers execute
the same code path as before — the absence of regressions is structural, not a promise.

The index lives in `TableState` as `state.rowUnits`, assigned from `Table.kt` during composition the
same way `visibleColumns` is. This matters practically: `ViewportUtils` and `KeyboardNavigation`
already receive `state`, so the mapping reaches them without signature churn.

It is snapshot state, unlike the `visibleColumns` field it sits beside: the prefetcher reads the
index inside a `snapshotFlow`, which re-evaluates only when a *tracked* read changes. A plain field
would be untracked there and would ride on the `layoutInfo` read next to it — right only while
something else keeps scrolling.

`rowGroups` validation (ascending, non-overlapping, within `itemsCount`) is a `require` in the index
builder, matching `IndexedListAdapter` in `TableBody.kt`. The index is built under
`remember(itemsCount, rowGroups)`, so validation runs once per change, not per frame.

### Public API

```kotlin
// Table / EditableTable — ONE parameter for the whole feature
rowGroups: TableRowGroups? = null,

@Stable
public class TableRowGroups(
    public val ranges: ImmutableList<IntRange>,
    public val onMove: ((from: IntRange, to: IntRange) -> Unit)? = null,
    public val header: (@Composable (rows: IntRange) -> Unit)? = null,
)

// TableDimensions / TableColors
val rowGroupSpacing: Dp = 8.dp
val rowGroupContainerColor: Color

// RowGroups.kt
public fun <T> List<T>.rowGroupsOf(groupIdOf: (T) -> Any?): ImmutableList<IntRange>
public fun <T> MutableList<T>.moveRowGroup(from: IntRange, to: IntRange)
```

One config object rather than three parameters: `Table` already carries
`@Suppress("LongParameterList")`, and the codebase already has this vocabulary — `TableSettings`,
`TableColors`, `TableDimensions`, `TableCustomization`. `TableRowGroups` joins that row.

`@Stable`, not `@Immutable`. `@Immutable` buys only static-expression promotion, which can never fire
here because `ranges` is derived from rendered data inside a `remember`; and it would overclaim, since
a class holding a `@Composable` slot is not a fixed value and its equality is by identity. The repo
draws the same line already: `TableColors` / `TableSettings` are data-class values (`@Immutable`),
`TableCustomization` carries composables (`@Stable`).

Identity equality has a consequence worth stating: **hold the instance in `remember`**. A fresh
`TableRowGroups` each recomposition stops `Table` from skipping. This is stricter than `TableSettings`,
whose structural equality forgives a fresh instance.

`onRowMove` is untouched; existing code compiles and behaves as before. The contract is explicit:
when row reorder is enabled, `require(rowGroups?.onMove != null || rowGroups?.ranges.isNullOrEmpty())`
— passing groups without a move callback fails loudly instead of silently breaking the drag. With
reorder disabled, `rowGroups` is still honoured for its visual effect and no callback is required.

Ranges rather than leader indices: the table already knows the block extent it just moved, so
returning `IntRange` costs nothing and keeps the callback self-contained. `from.first` is the leader
index for consumers that only need it. Returning `T` values instead of indices is not an option —
`itemAt` may return `null` for not-yet-loaded rows.

**Do not collapse `to` to a single index.** A downward move inserts the block after `to.last`; passing
only the target's leader row drops the block *inside* the target group and splits it. Upward moves
insert at `to.first`, where the leader happens to be correct — so the mistake looks direction-specific
and survives casual testing. This is not hypothetical: the first version of the sample made exactly
this error.

There is deliberately no `onClick` for groups. A consumer that wants interaction puts its own
`Modifier.clickable` inside `header`, which avoids defining how a library-level group click would
compose with row clicks and drags.

Consumer usage:

```kotlin
val rows: List<PlanRow> = uiState.rows

Table(
    itemsCount = rows.size,
    itemAt = { rows.getOrNull(it) },
    rowGroups = remember(rows) {
        TableRowGroups(
            ranges = rows.rowGroupsOf { it.groupId },   // [2..4, 7..8]
            onMove = { from, to -> vm.moveBlock(from, to) },
            header = { range -> GroupHeader(rows[range.first].groupId) },
        )
    },
)
```

Grouping, renaming and dissolving a group never touch ranges: those operations change a row's
`groupId` in the consumer's own data, a new list arrives, and `rowGroupsOf` recomputes. Ranges are
derived state, not state to maintain.

### Rendering

```kotlin
val rowUnits = state.rowUnits

items(
    count = rowUnits.unitCount,
    key = { unit -> val lead = rowUnits.rowsOf(unit).first; rowKey(itemAt(lead), lead) },
) { unit ->
    val rows = rowUnits.rowsOf(unit)
    ReorderableItem(state = reorderState, key = /* same key */) {
        val rowScope = remember(this) { TableItemDragScope(this) }
        context(rowScope) { RowUnit(rows, isGroup = rowUnits.isGroup(unit), ...) }
    }
}
```

The unit key is the **leader row's** `rowKey`. The block moves as a whole, so the leader stays the
leader and the key is stable across a drag — reorderable never loses the dragged item.

A group unit:

```kotlin
Column(
    modifier = Modifier
        .width(state.tableWidth)
        .background(colors.rowGroupContainerColor)
        .padding(
            top = if (rowGroupHeader != null) 0.dp else dimensions.rowGroupSpacing,
            bottom = if (nextIsGroup) 0.dp else dimensions.rowGroupSpacing,
        ),
) {
    rows.forEach { TableBodyRow(index = it, ...) }
}
```

Three details here were each settled by looking at the real thing on screen rather than at a mockup:

**Every row keeps its own divider, including the group's last one.** A divider means "this row ended";
suppressing it inside a group leaves the last row visually open. The block therefore draws no divider
of its own, and the rule stays uniform: rows draw dividers, blocks draw margin.

**The tint is what makes the group legible, and it can only live in the margin.** Each row paints its
own opaque `Surface`, so a background *behind* the rows is invisible. Painting it on the `Column` that
also carries the padding means the rows cover the middle while the gap bands show the tint — that band
is the group's boundary. A bare gap with no tint disappears on a dark theme.

**A block always opens with a top band and closes with a bottom gap only when the next unit is not a
group.** Symmetric padding stacks between adjacent blocks — 8dp + 8dp of tinted band merged into one
slab, erasing the boundary it exists to draw. The band that opens a block is the header when one is
configured, so it is the *bottom* gap that gives way: suppressing the top instead would leave the
second of two adjacent blocks nameless. `nextIsGroup` comes from `rowUnits.isGroup(unit + 1)`, so
exactly one band separates any two units and a block at either end of the list keeps its outer band.

A single-row unit takes the existing branch unchanged. The wrapper is pure vertical composition and
adds **no horizontal insets** — this is load-bearing, not cosmetic: horizontal padding would shift
group rows relative to the header. Column widths (`state.resolveColumnWidth`) and column order
(`state.columnOrder`) live in `TableState` and are read per row per render, so column resizing and
column reordering are unaffected. `ApplyAutoWidthEffect` only reads
`visibleItemsInfo.isNotEmpty()` — no index arithmetic — so units are invisible to auto-width.

A group of one row stays a group: gap and drag still apply.

Callback translation:

```kotlin
rememberReorderableLazyListState(verticalState) { from, to ->
    val fromRows = rowUnits.rowsOf(from.index)   // from.index is a unit index
    val toRows = rowUnits.rowsOf(to.index)
    onRowsMove?.invoke(fromRows, toRows) ?: onRowMove?.invoke(fromRows.first, toRows.first)
}
```

**Constraint:** `rowGroups` must be derived from the same data snapshot as `itemAt`. In a
`LazyColumn`, reorderable invokes `onMove` *during* the drag and waits for `layoutInfo` to change,
so the consumer mutates its list synchronously and `rowGroups` must recompute in the same
recomposition. Groups lagging the data by even one frame desynchronise unit boundaries from rows and
break the drag. `remember(rows) { rows.rowGroupsOf { it.groupId } }` is the supported pattern;
hand-maintained range state is not.

### Embedded path

`TableBodyEmbedded` renders every row eagerly through `ReorderableColumn`, which takes the backing
list and reports `onSettle(fromIndex, toIndex)` **on drop**, not during the drag. The same
`RowUnitIndex` applies: `IndexedListAdapter` is built over units instead of rows, each unit renders
its rows in a `Column`, and `onSettle`'s unit indices translate through `rowsOf(...)` exactly as in
the lazy path. `rowGroups` therefore means the same thing in both modes.

The live-vs-settle difference is reorderable's, not ours, and it already applies to `onRowMove`
today: in a `LazyColumn` the callback fires repeatedly mid-drag and the consumer must mutate
synchronously; in an embedded table it fires once, after the drop. The design documents this
asymmetry rather than hiding it, because for `TableRowGroups.onMove` it decides whether the consumer
needs an optimistic in-drag move at all.

### Index mapping

Three places currently assume lazy-item index == row index. Each is fixed with one `state.rowUnits`
call:

- `ViewportUtils.ensureRowFullyVisible` — `visibleItemsInfo.firstOrNull { it.index == index }` and
  `animateScrollToItem(index)` take `unitOf(index)`. Height estimates come from per-row
  `rowHeightsPx` and under-count a group's gaps; the function already corrects the scroll from
  actual `info.offset` afterwards, so this is a deliberate approximation, not a defect.
- `KeyboardNavigation` PageUp/PageDown — `fullyVisible` counts visible lazy items (units) but is
  added to a row index. Correct target: `rowsOf(unitOf(currentRow) ± fullyVisible).first`, clamped.
- `TableViewportPrefetcher` — `lastVisible + 1` is the next *unit*; use
  `rowsOf(lastVisibleUnit).last + 1`.

`TableActiveFilters` needs no change: its `LazyRow` scrolls filter chips and clamps to
`activeFilters.size`, an index space of its own that never touched rows.

`rowGroups` and `state.groupBy` describe two different structures over one list. While reorder is
on, the grouping menu is locked; with reorder off a user could group by column on top of
`rowGroups`. Throwing from a menu click handler is unacceptable, so `groupBy != null` wins: row
units fall back to identity, the gap disappears, rows render normally, and a warning is logged.

## Testing and verification

`table-core` has no tests today. Applying the existing `ua.wwind.convention.test` plugin is one line
in `table-core/build.gradle.kts` and adds no new catalog entries.

`commonTest` covers where the off-by-ones live:

- `RowUnitIndexTest` — identity without groups; group at start / end / adjacent groups; single-row
  group; round-trip `rowsOf(unitOf(row)).contains(row)` for every row; `require` failures on
  overlap, wrong order, out-of-bounds.
- `RowGroupsTest` — `rowGroupsOf` collapses only *adjacent* runs of an equal non-null `groupId`
  (two disjoint runs of the same id yield two ranges — documented behavior, not a bug); `null` never
  groups.
- `MoveRowGroupTest` — block up, block down, to either edge, no-op, swapping two groups.

Unit tests only prove the arithmetic. The feature is done when the real surface is driven: a
`table-sample` demo with two groups among plain rows, run on the desktop target, dragging a group by
its leader handle and observing that the block follows the cursor rather than jumping, lands between
neighbours intact, shows the gap — and that a table *without* `rowGroups` behaves exactly as before.
The same demo is driven a second time with `embedded = true` to exercise the settle-time path. The
demo doubles as the API showcase for the docs.

## Rollout

- Minor version bump; `CHANGELOG.md` entry.
- `docs/content/guides/row-reordering.md` gains a group section: `TableRowGroups` and its three
  members, the `remember` requirement, the derived-state constraint, the "don't collapse `to`"
  warning, the live-vs-settle callback difference, and the `groupBy` conflict rule.
- KDoc on the new public API (Dokka publishes it).

## Known limitations

- Keyboard navigation to a row inside a group scrolls to the block, not the row.
- A group's rows are not individually lazy: a very large group composes as one item.
- No drop indicator between neighbours; drops remain swap-based (see Rejected alternatives).
- **The embedded-table group path is verified by compilation only.** `TableBodyEmbedded` routes groups
  through `ReorderableColumn`'s settle-time `onSettle`, and the index translation is the same
  `RowUnitIndex` the scrolling path uses — but no test covers it and nobody has driven it on screen.
  The scrolling path was verified by hand and produced four rendering defects that no test caught, so
  this is a real gap, not a formality. It is also the less defended of the two paths:
  `ReorderableColumn` captures `onSettle` once and keys its state on the list by structural equality,
  where `rememberReorderableLazyListState` wraps the callback in `rememberUpdatedState`. Anything the
  callback reads must therefore be routed through state rather than captured — review found exactly
  this defect here, since a list of group leaders can compare equal across a change to `RowUnitIndex`.
- `TableColors.rowGroupContainerColor` has no default value, so constructing `TableColors` directly is
  source-breaking. Nothing in this repo does — only `TableDefaults.colors()` builds it, with named
  arguments — but external code need not be so lucky, which is what the CHANGELOG note is for. The
  factory takes the new colour as a *trailing* optional parameter: every parameter it has is a
  `Color`, so inserting one in the middle would leave positional calls compiling while silently
  rebinding the colours after it. `TableDimensions.rowGroupSpacing` defaults and is therefore
  compatible.
