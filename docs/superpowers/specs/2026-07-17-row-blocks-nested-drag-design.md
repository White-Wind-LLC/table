# Row blocks: nested drag (within-block reorder + header block handle)

**Status:** design, awaiting review. This is an increment on top of the row-blocks feature
(`RowBlocks`, `RowBlocksState`, `RowUnit`) that is not yet released — so its public API may be
reshaped freely, without breaking-change concerns. Lands as a single commit on top of the existing
two branch commits (width fix + row-blocks feature); the spec rides in that same commit (no
standalone `docs(...)` commit).

## Goal

Split row dragging into two independent levels:

1. **Whole-block drag** moves from the block's **leader row** to the block's **header band**. The
   header carries the drag handle for the whole block.
2. **Within-block drag** is new: every row inside a block carries its own handle and can be
   reordered, but only **within its own block** — a row can never leave its block.

Standalone rows (no block id) are unchanged: they reorder as top-level units via their own row
handle.

## Interaction model

Two nested reorder levels:

| Level | Reorders | Trigger | Commit |
|---|---|---|---|
| Outer | units: standalone rows + whole blocks | standalone row → its row handle; block → **header handle** | `onCommit(RowBlockMove)` (unchanged) |
| Inner | rows within one block | **every** row's handle | new `onRowReorderWithinBlock(RowWithinBlockMove)` |

Behavior changes vs. the current feature:

- The whole-block handle **moves off the leader row onto the header**. A block with no `blockHeader`
  cannot be dragged as a whole (only its rows reorder internally). A dev warning fires when
  `onCommit != null` but `blockHeader == null` (whole-block moves are impossible).
- **Every** row of a block gets a handle now (within-block reorder), leader included.
- `TableCellScope.isRowBlockLeader` is **removed**. It existed only to gate the leader-row handle,
  which no longer exists as a concept; keeping a meaningless "leader" flag in the first release of
  this API would only mislead. `TableRowContext.isInRowBlock` (used for row styling) stays.

## Why nested engines (rejected alternatives)

- **Flat row-level engine that rejects cross-block moves.** Re-introduces the v1 positional
  geometry the current design deliberately escaped: vetoing individual moves in `onMove` desyncs the
  reorder engine's predicted offset. Rejected.
- **A separate managed state per block.** Duplicates the hard-won `reconcile` / optimistic-hold /
  `settle` machinery of `RowBlocksState`. Rejected in favor of reusing one permutation.

A nested `ReorderableColumn` gives the "row never leaves its block" constraint for free: the inner
column contains only that block's rows, so the engine physically cannot move a row out of it.

## Public API (`table-core/src/commonMain/kotlin/ua/wwind/table/RowBlocks.kt`)

```kotlin
@Stable
public class RowBlocks<T : Any>(
    public val blockOf: (T) -> Any?,
    public val onCommit: ((RowBlockMove) -> Unit)? = null,
    // Header slot now receives a scope carrying the whole-block drag handle:
    public val blockHeader: (
        @Composable context(RowBlockHeaderScope) (blockId: Any, rows: IntRange) -> Unit
    )? = null,
    // NEW: one event per completed within-block gesture; null disables within-block reorder.
    public val onRowReorderWithinBlock: ((RowWithinBlockMove) -> Unit)? = null,
)

/** One completed within-block gesture: a single row moved among its block-mates. Anchors are the
 *  visible neighbours inside the same block; null anchors mean the block's edge. */
public class RowWithinBlockMove(
    public val blockId: Any,
    public val movedKey: Any,
    public val afterKey: Any?,   // key of the visible row it now sits after within the block; null = block start
    public val beforeKey: Any?,  // redundant anchor; null = block end
)
```

`RowBlockHeaderScope : TableItemScope` is a distinct scope type (NOT a supertype of
`TableCellScope`) so that a `Modifier.draggableHandle()` call inside the header binds unambiguously
to the outer unit engine, while a `draggableHandle()` inside a cell binds to whatever engine that
row is rendered under. It exposes a `context(RowBlockHeaderScope) fun Modifier.draggableHandle(...)`
convenience mirroring the cell one.

### Apply helper (`RowBlocks.kt`)

```kotlin
/** Lifts a within-block move to the SOURCE list. Relocates the single moved row among its
 *  block-mates; block members hidden by the current filter keep their relative order, and the moved
 *  row lands adjacent to the resolved visible anchor. Leaves the list untouched when neither anchor
 *  resolves. */
public fun <T> MutableList<T>.applyRowReorderWithinBlock(
    move: RowWithinBlockMove,
    keyOf: (T) -> Any,
    blockOf: (T) -> Any?,
)
```

Anchor resolution mirrors `applyRowBlockMove`: `afterKey` is primary, `beforeKey` the fallback for
when the primary anchor is filtered out. Unlike `applyRowBlockMove` the moved set is a single key,
and the insertion point is **not** expanded to a whole-block boundary — the row lands immediately
adjacent to its in-block anchor.

## Managed permutation (`table-core/.../state/RowBlocksState.kt`)

Reuse the existing `viewOrder` / `upstream` / `reconcile` / optimistic-hold / `Derived` snapshot
machinery. Additions:

- A gesture-kind discriminator so `settle()` knows whether the completed gesture was a whole-block
  move (emit `RowBlockMove` via `onCommit`) or a within-block move (emit `RowWithinBlockMove` via
  `onRowReorderWithinBlock`).
- `applyRowMoveWithinBlock(fromView: Int, toView: Int)`: permutes `viewOrder` **only within a single
  run** (`moveRowGroup` with single-row ranges). Both endpoints must lie in the same run; a call that
  would cross a run boundary is dropped (defensive — the inner engine cannot generate one). Because
  every row keeps its block id, `derive()` recomputes identical runs — blocks never fragment from a
  within-block move.
- Within-block `settle()`: reads the dragged row's current view position, derives `blockId` and the
  in-block neighbour keys (or nulls at the block's first/last visible row), emits the move, keeps the
  optimistic order (no snap-back) until the consumer's applied move flows back through `reconcile`.
- Paged policy for within-block mirrors the whole-block one: if a needed in-block landing neighbour is
  still a placeholder, refuse the drop, snap back, bump `refusedDropCount`, emit nothing.

The inner reorder is "managed" exactly as the outer embedded path is: the nested `ReorderableColumn`
keys its engine state on the block's row list; `applyRowMoveWithinBlock` mutates `viewOrder`
synchronously so the block's rows recompose in the new order and the engine's post-move layout read
finds its prediction already satisfied.

## Rendering (`table-core/.../component/body/RowUnit.kt`, `TableBody.kt`)

For a **block** unit `RowUnit` renders:

1. The **header band** under the **outer** unit scope (the ambient `TableItemScope` from the outer
   `ReorderableItem` / `ReorderableColumn` item), passed into the `blockHeader` slot as
   `RowBlockHeaderScope`. `draggableHandle()` there drives the whole-block (outer) gesture.
2. A nested **`ReorderableColumn`** over the block's rows. Each row is rendered under the **inner**
   scope; a `draggableHandle()` in a row cell drives the within-block (inner) gesture. The inner
   column's `onSettle` calls `blocks.applyRowMoveWithinBlock(...)` then `blocks.settle()`, with the
   same `gestureRemap`/height-eviction handling the outer settle uses.

For a **standalone** unit `RowUnit` is unchanged: one row under the outer scope, its cell handle
drives the outer gesture.

Both outer paths get the nested column: the lazy `TableBody` (outer = `ReorderableItem` in a
`LazyColumn`) and the embedded `TableBodyEmbedded` (outer = `ReorderableColumn`). Nesting a
`ReorderableColumn` inside an outer `ReorderableItem` is the primary technical risk (see below).

Inner autoscroll is bounded to the block's own height and is not expected to be needed; if the nested
engine's autoscroll fights the outer list scroll it will be disabled for the inner column.

The `isInRowBlock` / `isRowBlockLeader` params threaded through `TableBodyRow` / `TableRowItem` /
`TableViewportPrefetcher` are simplified: `isInRowBlock` stays (row styling); the `isRowBlockLeader`
param and its plumbing are removed.

## Filtering & paging semantics (within-block)

- **Filtering:** a within-block move reorders the moved **visible** row relative to its visible
  block-mates. Hidden members of the block keep their relative order; on lift-to-source the moved row
  is inserted immediately adjacent to the source position of the resolved visible anchor. This is the
  within-block analogue of `applyRowBlockMove`'s anchor handling.
- **Paging:** placeholder rows cannot anchor a within-block drop (their key does not exist yet); the
  drop is refused and the view snaps back, same as the whole-block policy. Holding over the landing
  spot loads the page and lets the retry land.

## Sample (`table-sample`)

- `TableColumns.kt`: drop the `if (isRowBlockLeader)` gate on the reorder-handle cell — render the
  handle on every row when reorder is enabled. The scope wiring binds it to the correct engine.
- `SampleApp.kt`: add `Modifier.draggableHandle()` to the `blockHeader` content so the whole block
  drags from its header; wire `onRowReorderWithinBlock = { move -> viewModel.onEvent(RowWithinBlockMove(move)) }`.
- `SampleViewModel.kt` + `SampleUiEvent`: new event that applies the within-block move to the master
  list via `applyRowReorderWithinBlock`.

## Docs

Public docs are edited to the new API/logic directly (no v1/v2 archaeology):

- `docs/content/guides/row-reordering.md`: whole-block drag is triggered from the header; document
  within-block reorder, its handle-on-every-row model, and `applyRowReorderWithinBlock`.
- `docs/content/reference/core-api.md`: add `onRowReorderWithinBlock`, `RowWithinBlockMove`,
  `applyRowReorderWithinBlock`, the header scope; remove `isRowBlockLeader`.
- `CHANGELOG.md`: fold into the unreleased row-blocks entry (still `1.11.0`) — the header-handle +
  within-block reorder are part of the same unreleased feature, not a separate release line.

## Testing

- **jvmTest (unit):** `applyRowMoveWithinBlock` permutes only within a run and never fragments a
  block; cross-run calls are dropped; within-block `settle()` emits the right `RowWithinBlockMove`
  (anchors, edges, no-op when the row ends where it started); `applyRowReorderWithinBlock` lifts
  correctly with hidden members and with unresolved anchors (no-op).
- **Drive the real desktop surface** (`./gradlew :table-sample:run -PenableIos=false`) — mandatory,
  green tests are not sufficient:
  - Drag a row within a block: order changes inside the block, the row cannot leave the block, and
    the commit lands the change after the pipeline applies it.
  - Drag the block header: the whole block moves as one unit and lands intact.
  - Standalone row drag still works; a table without blocks behaves exactly as before.

## Risks

1. **Nested reorder engines (primary).** `ReorderableColumn` nested inside an outer `ReorderableItem`
   / `ReorderableColumn` may have gesture-capture or autoscroll conflicts. **Mitigation:** the first
   implementation step is a minimal desktop spike — one block with a nested reorderable column, drag
   an inner row, confirm the inner gesture is not hijacked by the outer engine and the block still
   drags from its header. Validate on the real surface before building the managed within-block
   permutation.
2. **Within-block settle under async pipelines.** Reuses the whole-block reconcile/optimistic model;
   covered by the paged-refusal policy above and by unit tests.

## Out of scope

- Cross-block row moves (explicitly disallowed).
- Multi-row within-block selection drag (one row per within-block gesture).
- Grouping (`groupBy`) interaction with blocks — unchanged; still mutually exclusive as documented.

## Decisions (open to veto at spec review)

- **(a)** Names: `onRowReorderWithinBlock` / `RowWithinBlockMove` / `applyRowReorderWithinBlock`.
- **(b)** Remove `TableCellScope.isRowBlockLeader` entirely (unreleased; now meaningless).
- **(c)** Within-block filtering: hidden members keep relative order; moved row lands adjacent to the
  visible anchor.
