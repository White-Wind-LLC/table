# Row blocks: managed drag redesign

Supersedes `2026-07-15-row-group-drag-design.md`. That design shipped a correct render core behind a
positional public contract; an independent five-reviewer audit traced every reported integration
failure to that contract. This document keeps the proven core and replaces the contract.

## Why the redesign

The v1 public API was `TableRowGroups(ranges: List<IntRange>, onMove: (IntRange, IntRange) -> Unit,
header)`: positional ranges over the currently rendered list, live move callbacks during the drag,
and a hard requirement that the consumer mutates its list synchronously mid-gesture. The audit
confirmed, by executed experiments:

- **Sorting fragments blocks.** Ranges collapse adjacency; a plain `sortedBy` over a grouped list
  renders one logical group as several bands with duplicated headers. Formally proven: under an
  active injective sort, the view is invariant to any permutation of the source — **no drag
  semantics exist while a sort is applied**. This is a theorem, not a policy choice.
- **Filtered moves cannot be expressed correctly.** View-space ranges applied to the source list
  silently move hidden rows the user never touched; the id-mapping workaround (as in the v1 sample)
  either tears groups in the source or silently drops the move — and a dropped move is not neutral:
  the reorder engine predicts the offset and waits on a mutex up to 1000 ms per attempt, so the drag
  visibly freezes and snaps.
- **The synchronous-mutation contract is unimplementable in ordinary architectures.** The v1 sample
  itself routed moves through `MutableStateFlow -> combine -> stateIn` — an asynchronous chain —
  violating the contract its own spec stated. If the reference consumer cannot satisfy the contract,
  the contract is wrong.
- **A one-frame lag between the rendered list and the declared ranges crashes the table**
  (`require` in `RowUnitIndex.of`) — reachable by any consumer whose filter pipeline is async.
- **Positional runtime state goes stale after a block move**: `selectedIndex`, `selectedCell`,
  `editingRow`, `checkedIndices`, `rowHeightsPx` keep pointing at old positions.
- **The embedded path renders the band header at width 0** (`horizontalState.viewportSize == 0`;
  the scroll node is only attached by `draggableTable`, which the embedded path skips).
- **Naming collides**: "group" already means column-value grouping (`groupBy`,
  `groupContainerColor`, `ColumnSpec.groupHeader`); the drag feature reused the word one token away
  (`rowGroupContainerColor`, `TableRowGroups.header`).

The feature is unreleased, so breaking its API costs nothing now.

## Locked decisions

1. **The render core stays.** A block renders as ONE `LazyColumn` item; `RowUnitIndex` maps row
   indices to unit indices and is the identity without blocks; scroll/keyboard/prefetch translate
   through it. All of it is property-tested (bijection, coverage, footer boundary) and survives
   unchanged. The reorder engine stays `sh.calvin.reorderable` — dragging one lazy item keeps its
   swap-prediction geometry correct by construction.
2. **Blocks are declared by identity, not by position.** The consumer supplies
   `blockOf: (T) -> Any?`; the library derives adjacency runs itself, from the same snapshot it
   renders. Stale or mismatched ranges become impossible by construction — the crash class above
   disappears with them.
3. **The drag is managed.** During a gesture the library mutates its own internal view permutation,
   satisfying the engine's synchronous-layout expectation itself. The consumer's data does not
   change mid-drag. On drop the library emits ONE semantic event. Lazy and embedded paths behave
   identically (one event per completed gesture).
4. **Naming: "block".** "group" remains exclusively the `groupBy` vocabulary. New API uses
   `rowBlocks`, `RowBlocks`, `RowBlockMove`, `blockOf`, `rowBlockSpacing`,
   `rowBlockContainerColor`.
5. **Sorting and free drag are mutually exclusive** (theorem above). Sorting *within* blocks is a
   library helper over consumer data. The existing reorder interaction lock is extended to be
   consistent (see Behavior contract).
6. **A filtered drag moves the whole block in the source list.** Hidden members travel with the
   block, preserving relative order. This is the canonical semantics, implemented by a library
   helper whose algorithm is property-proven (see Helpers).

## Domain model

- **S** — the consumer's source list (their single source of truth).
- **V** — the list the consumer renders: `V = filter(S)` (sorting composes only via the
  within-blocks helper; free sort is out of contract — theorem). V ⊆ S and order-preserving.
- **Block** — a maximal run of adjacent rows in V sharing a non-null `blockOf(item)`. A row with
  `blockOf == null` is a standalone unit. Block identity is the `blockOf` value; block *extent* in
  V is derived by the library every time V changes.
- **Drag unit** — a block or a standalone row; rendered as one lazy item (unchanged from v1).
- **Move** — during the gesture, an internal permutation of units over V, owned by the library.
  On drop, one `RowBlockMove` describing the result in stable keys.
- **Lift** — translating the drop into S. Performed by the consumer with the provided helper
  `applyRowBlockMove`, which needs only keys and `blockOf` — never positions.

## Public API

```kotlin
// ua.wwind.table.RowBlocks.kt — ONE declaration, identical for every table kind

@Stable
public class RowBlocks<T : Any>(
    /** Block identity; null = standalone row. Adjacent equal ids form one block. */
    public val blockOf: (T) -> Any?,
    /** Exactly one event per completed drag gesture; null = display-only blocks (no drag). */
    public val onCommit: ((RowBlockMove) -> Unit)? = null,
    /** Band content above a block. */
    public val blockHeader: (@Composable (blockId: Any, rows: IntRange) -> Unit)? = null,
)

public class RowBlockMove(
    /** Block id of the dragged unit; null when a standalone row moved. */
    public val blockId: Any?,
    /** Keys of the VISIBLE moved rows, in order. */
    public val movedKeys: List<Any>,
    /** Key of the row the unit now sits after, in the rendered order; null = start. */
    public val afterKey: Any?,
    /** Key of the row the unit now sits before; null = end. Redundant anchor for edge cases. */
    public val beforeKey: Any?,
)

/**
 * Applies [move] to the SOURCE list: relocates ALL rows whose blockOf equals move.blockId —
 * including rows hidden by the current filter — preserving their relative order. The insertion
 * point is expanded to the nearest whole-block boundary in the source, so no other block is ever
 * split. Standalone rows (blockId == null) relocate just the moved keys.
 */
public fun <T> MutableList<T>.applyRowBlockMove(
    move: RowBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
)

// ua.wwind.table.RowBlockOrdering.kt — ordering helpers over consumer data

/** Sorts rows within each block; blocks (and standalone rows) order by their minimal element.
 *  Blocks never fragment, by construction. */
public fun <T> List<T>.sortedWithinRowBlocks(
    blockOf: (T) -> Any?,
    comparator: Comparator<in T>,
): List<T>

/** Filters rows; optionally keeps every member of a block whose any member matches. */
public fun <T> List<T>.filteredWholeRowBlocks(
    blockOf: (T) -> Any?,
    predicate: (T) -> Boolean,
): List<T>
```

`RowBlocks` compares by identity — hold it in `remember`. The managed view permutation lives in an
*internal* `RowBlocksState`, derived on every snapshot from `itemAt`/`blockOf`; consumers never
construct or see it. Handle placement reads `TableCellScope.isRowBlockLeader: Boolean` (true on the
first visible row of a block and on standalone rows) — replacing the v1 sample's O(n) leader
detection. A stable `rowKey` is a documented hard requirement of block tables:
`movedKeys`/`afterKey`/`beforeKey` are built from it, and the default index key is rejected with a
warning when `rowBlocks` is passed.

`Table`, `EditableTable` and the `table-paging` adapter all take the same parameter:
`rowBlocks: RowBlocks<T>? = null`. The declaration, the semantics and the event type are identical
everywhere; tables differ only in where their data lives, and therefore in what the consumer's
`onCommit` does with the event — an in-memory consumer calls `applyRowBlockMove` on its list, a
paged consumer forwards the event to its data layer. In the paged table, drag additionally follows
the paged drop policy (anchor must be loaded, else the gesture snaps back — see Behavior contract /
Paging); without `onCommit` blocks are display-only in every table kind.

Theming: `TableDimensions.rowBlockSpacing` (default 8.dp) and `TableColors.rowBlockContainerColor`
with default `Color.Unspecified`, resolved to `surfaceContainerHighest` at draw — the v1 required
constructor parameter (a source break) is removed.

v1 API (`TableRowGroups`, `rowGroupsOf`, `moveRowGroup`, `rowGroupSpacing`,
`rowGroupContainerColor`) is deleted, not deprecated — it never shipped.

## Behavior contract

**Drag lifecycle.** Handle goes down → the engine swaps units → `RowBlocksState` applies each swap
to its internal permutation synchronously (the engine's `layoutInfo` expectation is satisfied
in-library; no timeout stalls are reachable). Drop → the state diffs the permutation against the
pre-gesture order and emits ONE `RowBlockMove`; the permutation is kept optimistically until
`items` changes (no snap-back). If `items` changes mid-gesture (external update), the gesture is
cancelled and the permutation reset — documented policy.

**Commit point side effects (in-library, at emit time):** `selectedIndex`, `selectedCell.rowIndex`,
`editingRow` and `checkedIndices` are remapped through the same permutation; `rowHeightsPx` entries
for shifted rows are cleared (cheapest correct option — heights re-measure). An active cell edit is
completed/cancelled when a drag starts.

**Sorting.** Header sort UI stays locked while reorder is enabled (as in v1) — and the lock becomes
consistent: `state.setSort()` is normalized to a warning no-op under reorder, matching `initialSort`.
Consumer-side sorting composes only through `sortedWithinRowBlocks`; the guide states the theorem
plainly ("a free sort makes drag history unobservable — they cannot both apply"). If the derived
runs show the same block id in disjoint runs (a fragmented block — evidence of a foreign sort or a
member-splitting filter), the library logs one warning naming the id.

**Filtering.** Allowed and first-class. The rendered list is the consumer's filtered V;
`isBlockLeader` and band rendering follow V. A drag of a partially hidden block emits keys of
visible members only; `applyRowBlockMove` relocates the whole block in S. Consumers who want
match-whole-block filtering use `filteredWholeRowBlocks`.

**groupBy.** Mutually exclusive, `groupBy` wins, warning logged (unchanged mechanics). Two
additions: a public read-only `TableState.rowBlocksSuppressedByGroupBy: Boolean` so consumers can
surface the conflict, and the column menu's group-by item is disabled while blocks are non-empty —
the silent-disappearance path is closed in the UI. `onCommit` is not invoked while suppressed.

**Paging.** Blocks work with `table-paging` in both display and drag modes; what paging cannot have
is the *client-side list apply* — `applyRowBlockMove` needs the materialized source, which a paged
consumer does not hold. The commit event itself is semantic (`blockId` + anchor keys) and needs no
full list: paged consumers forward it to their data layer, which knows full block membership.

- *Derivation*: the paging adapter accepts `blockOf: (T) -> Any?` (+ `blockHeader`, + optional
  `onCommit`) and derives bands over loaded adjacent runs on each snapshot. An unloaded placeholder
  breaks a run, so a partially loaded block renders a partial band that extends as pages arrive.
  `isInRowBlock` is set for members, so they can be styled via `customization`.
- *Drag*: allowed when `onCommit` is set. While the user holds the drag, pages under the pointer
  load as usual, so anchors resolve naturally. **Drop policy**: if the landing neighbour is loaded,
  the anchor keys resolve and the move commits; if it is still a placeholder, the gesture cancels
  and the block snaps back to its origin — no event is emitted. A partially loaded dragged block
  moves visually as its loaded fragment; `movedKeys` carries loaded members only, and the data
  layer applies the move by `blockId`, covering members the client never loaded.
- *Must-verify risk (test plan item)*: a page loading mid-gesture replaces placeholder index-keys
  with item keys under the engine. The dragged row's own key is stable (it is loaded), but neighbour
  key churn during a drag must be proven harmless in the compose harness with a fake paged source
  before this mode ships; if it is not, the fallback policy is to cancel the gesture on page load.

(v1 silently didn't forward the feature to paging at all.)

**Selection/editing.** Remapped at commit (above). Documented residual: selection is positional
between commits, as everywhere else in the table.

**Keyboard/scroll/prefetch.** Unchanged from v1 (unit-aware paging, block-level `ensureRowFullyVisible`
approximation, footer guard) — all reviewer-verified correct.

## Rendering and internals

- `RowUnit`, band/gap rules, `RowUnitIndex`, index translations: unchanged.
- `TableBody` (lazy): `rememberReorderableLazyListState` callback becomes
  `rowBlocks.applyUnitMove(from.index, to.index)`; commit fires from the drag-stop wrapper in
  `TableItemDragScope`.
- `TableBodyEmbedded`: `onSettle` → `applyUnitMove` + `settle()`. The `rememberUpdatedState`
  trampoline remains (engine captures the callback once).
- **Embedded band width fix:** `RowUnit` measures the header box with
  `horizontalState.viewportSize.takeIf { it > 0 }?.toDp() ?: state.tableWidth` — in embedded mode
  the scroll node is absent and the viewport is the whole table. (Same fallback documented as a
  follow-up for the pre-existing `groupBy` header issue in embedded mode.)
- Dead flag `TableRowContext.isGroup` (always false in v1) becomes `isInRowBlock`, set for rows
  rendered inside a block — customization can finally style block members.
- `moveRowGroup` survives as an `internal` view-permutation primitive with an overlap `require`.

## What stays consumer-side, honestly

- **P1 invariant**: every block is a contiguous run in S. The helpers preserve it and
  `applyRowBlockMove` never breaks it, but the library cannot see S and cannot enforce it against
  arbitrary consumer mutations. The fragmentation warning is detection, not prevention.
- **The lift is a convention.** Where hidden members land relative to hidden neighbours of the
  insertion point is unobservable in V; `applyRowBlockMove` picks block-boundary insertion. Other
  placements are equally V-consistent; consumers with exotic needs implement their own lift from
  `RowBlockMove` keys.
- **Swap-based targeting** (no insertion indicator) is inherited from the engine. The commit event
  is engine-agnostic, so a future insert-mode engine slots in without an API break.

## Drag consistency: one atomic snapshot per frame

Decision 3 (managed drag) mutates an internal `viewOrder` permutation per engine swap. The first
desktop drag exposed a defect in *how the lazy path reads that permutation*, not in the permutation
itself: the dragged block flew off after the first swap at normal speed (slow drags sometimes
survived a few positions).

**Root cause — a temporal read split.** The lazy `LazyColumn` item key was
`rowKey(itemAt(leader), leader)`, where `leader` came from a `RowUnitIndex` captured at composition
time while `itemAt` read the live `viewOrder` when the key lambda ran at measure. A swap moves
`viewOrder` between those two reads, so the key of the *dragged* block changed for one frame →
`LazyColumn` disposed and rebuilt that item → the drag handle nested inside it was torn down →
`reorderable`'s handle `DisposableEffect` fired `onDragStopped` → the gesture ended. The single-row
(no-blocks) path never hit this because its units are identity and its key reads one list; the
embedded path never hit it because `ReorderableColumn` reports the net move once at drop, leaving
`viewOrder` still for the whole gesture.

**Fix — the derived view is one atomic object.** `RowBlocksState.derive()` now freezes the item at
every view position into the same `Derived` value it builds `runs`/`units` from, and exposes it as
`snapshot`. The lazy path reads `blocks.snapshot` once and draws unit boundaries, item, block id and
key all from that single object (`snapshot.keyAt`, `snapshot.itemAt`, `snapshot.blockIdAt`). A key is
now a pure function of a frozen snapshot: a stale measure-time invocation returns a stale-but-
consistent key, never a torn one, so the dragged item keeps its identity across the whole gesture.
The commit path, the semantic `RowBlockMove`, filtered-move correctness and the positional remap are
untouched — the change is strictly *where the lazy item reads its identity from*.

**Second consistency requirement — the leader test must ride the same snapshot.** With the key fixed,
a *fast* down drag still died after one swap: the block's lazy item survived (its key was stable), but
the drag handle nested inside it vanished. The handle is gated by `isRowBlockLeader`, which
`TableRowItem` recomputed from `state.rowUnits` — a copy `Table` publishes from the same derivation,
but read in a *different* scope that can lag the item's own snapshot by a frame under a fast gesture.
For one frame the leader row failed its own is-leader test, the handle's `if (isRowBlockLeader)`
branch left composition, and its `DisposableEffect` fired `onDragStopped`. Three independent reads of
the permutation (the item index, the key, the leader test) is two too many. The fix threads
`isInRowBlock`/`isRowBlockLeader` down from `RowUnit` — computed from the very `rows`/`isGroup` the
snapshot handed it — through `TableBodyRow` into `TableRowItem`, so leader detection can never come
from a different read than the row it gates. `state.rowUnits` stays only for offscreen prefetch and
keyboard/scroll math, none of which gate a live handle.

This is the v1 drag property ("one list, derived once") realized without the v1 contract: the single
consistent list is the internal snapshot, and every reader on the drag path — index, key, leader
test — resolves against it, while the drop still emits the id-based event.

## Migration from the v1 branch

Nothing shipped; this is an in-branch evolution, squashed to one feature commit at the end.
Deleted: `TableRowGroups.kt`, public `rowGroupsOf`/`moveRowGroup`, `rowGroups` parameters,
`rowGroupSpacing`/`rowGroupContainerColor`. Kept: `RowUnitIndex` (+tests), `RowUnit`,
`TableBody` unit rendering, index translations, band visuals, docs structure. The sample loses its
~90-line id-mapping ViewModel logic in favour of one `applyRowBlockMove` call, replaces the O(n)
leader detection in the handle column with `TableCellScope.isRowBlockLeader`, and gains demos for
within-block sorting and whole-block filtered moves. The sample also gains a column-visibility
toggle (unrelated gap found during the width-fix investigation; cheap to include while the sidebar
is open).

## Test plan

1. **Unit (pure):** run derivation from `blockOf` (incl. fragmented-id warning); `applyUnitMove`
   permutations (up/down/edges/no-op/adjacent blocks); `settle()` emits exactly one event with
   correct `movedKeys`/`afterKey`/`beforeKey` both directions; `applyRowBlockMove` property tests —
   whole-block relocation under random filters, anchor inside a hidden foreign-block span, null
   anchors, missing keys, S-permutation validity, no block ever fragments (port the reviewer's
   3000-iteration harness); `sortedWithinRowBlocks` never fragments and is stable;
   `filteredWholeRowBlocks`.
2. **Compose harness (headless, existing infra):** blocks render with bands from `blockOf`
   derivation; commit remaps selection/editing; groupBy suppression + flag + menu item disabled;
   embedded band header has non-zero width; reconcile-on-external-change cancels the gesture;
   recomposition-settle guard; **paged mode with a fake paged source** — bands extend as pages
   "load", drop onto a placeholder cancels and emits nothing, drop onto a loaded anchor emits
   correct keys, and neighbour key churn from a mid-gesture page load does not corrupt the drag
   (the go/no-go check for shipping paged drag).
3. **Real surface (desktop sample, by hand):** drag a block under an active filter with a hidden
   middle member — the whole block relocates in the source after clearing the filter; within-block
   sort demo; rename/ungroup flows; embedded demo — **the embedded path must be driven this time**;
   the reviewer-confirmed freeze/snap symptom must be unreproducible.

## Known limitations (carried or accepted)

- Paged blocks: `applyRowBlockMove` (client-side list apply) is unavailable — commits must be
  applied by the consumer's data layer via `blockId`; a drop onto an unloaded placeholder cancels
  the gesture; partially loaded blocks drag as their loaded fragment until the data layer confirms.
- A block taller than the viewport scrolls as a block; `ensureRowFullyVisible` targets the unit.
- Large blocks compose as one item (not lazy) — cost documented; mitigations out of scope.
- Striping (`index % 2`) runs through blocks by row index — cosmetic, unchanged.
- Free consumer sort remains detectable but not preventable (P1/theorem above).
