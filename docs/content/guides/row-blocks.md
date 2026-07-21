# Row blocks

Adjacent rows sharing an id can render and drag as one **block**: a single drag carries all of
them, and an optional header band names the block. Blocks are declared **by identity** — you tell
the table which rows belong together, never where they are — with a `RowBlocks` passed as
`rowBlocks` to `Table`, `EditableTable`, or the [table-paging](../modules/table-paging.md) adapter:

```kotlin
@Stable
public class RowBlocks<T : Any>(
    /** Block identity; null = standalone row. Adjacent equal ids form one block. */
    public val blockOf: (T) -> Any?,
    /** One event per completed whole-block drag; null = a block cannot be dragged as a unit. */
    public val onCommit: ((RowBlockMove) -> Unit)? = null,
    /** Band above a block. Its `RowBlockHeaderScope` receiver carries the whole-block drag handle:
     *  a `draggableHandle()` here drags the entire block. A block with no header is not draggable whole. */
    public val blockHeader: (
        @Composable context(RowBlockHeaderScope) (blockId: Any, rows: IntRange) -> Unit
    )? = null,
    /** One event per within-block row reorder; null = within-block reorder disabled. */
    public val onRowReorderWithinBlock: ((RowWithinBlockMove) -> Unit)? = null,
)
```

Whether a block exists, what it is called, and how it is created or dissolved stay in your own data
and UI. The table derives block extents, moves the block, and draws the band.

Blocks build on the drag machinery described in [Row reordering](row-reordering.md), so
`TableSettings(rowReorderEnabled = true)` and the `-Xcontext-parameters` compiler flag apply here
too. Once `rowBlocks` is declared, it takes over: gestures report through `RowBlocks.onCommit`
instead of `onRowMove`.

## The mental model: two lists, one contract

There are two lists in play:

- **the source** — your single source of truth;
- **the rendered list** — what you pass through `itemsCount`/`itemAt`, usually a filtered
  projection of the source.

Everything the table tells you is expressed in terms of the rendered list; everything you own stays
in the source. The contract between them has three parts:

1. **Blocks derive from what you render.** The table applies `blockOf` to the same snapshot it
   renders: a maximal run of *adjacent* rows sharing a non-null id is one block, a row with a
   `null` id is a standalone unit. There are no index ranges to compute, keep in sync, or get
   stale — an asynchronously filtered pipeline cannot desynchronize what is derived from its own
   output.
2. **The drag is managed.** While a gesture is in progress the library permutes its own internal
   view; your data does not change mid-drag, and nothing is required to happen synchronously in
   your code. At the drop the table also remaps its positional runtime state — selection, checked
   rows, the row being edited, cached row heights — so they keep pointing at the rows they pointed
   at.
3. **You get one event per gesture.** On drop, a whole-block drag reports a single `RowBlockMove`
   via `onCommit`; a within-block row reorder reports a single `RowWithinBlockMove` via
   `onRowReorderWithinBlock`. Both describe the result in **stable row keys** — identical semantics
   in lazy and embedded tables:

```kotlin
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

public class RowWithinBlockMove(
    /** Block the row belongs to (unchanged by the move). */
    public val blockId: Any,
    /** Key of the moved row. */
    public val movedKey: Any,
    /** Key of the visible block-mate it now sits after; null = block start. */
    public val afterKey: Any?,
    /** Key of the visible block-mate it now sits before; null = block end. Redundant anchor. */
    public val beforeKey: Any?,
)
```

Applying either event to the source list is one call — `applyRowBlockMove` /
`applyRowReorderWithinBlock` — see below.

## Declaring blocks

- **Hold the `RowBlocks` in `remember`.** It is `@Stable` and compares **by identity** — a fresh
  instance on every recomposition makes the `rowBlocks` argument look changed and stops `Table`
  from skipping. Nothing in the declaration depends on the current list, so no keys are needed.
- **A stable `rowKey` is a hard requirement.** `movedKeys`, `afterKey` and `beforeKey` are built
  from it, and the default positional key cannot survive the move it describes. The table logs a
  warning when `rowBlocks` is passed with the default key.
- **Keep block members adjacent in the source.** Blocks form over adjacent rows only; the library
  derives, warns about fragmented ids (the same id in non-adjacent runs — evidence of a foreign
  sort or a member-splitting mutation), but cannot repair your source order.
- **`rowBlocks` supersedes `onRowMove`.** Drag units are blocks, so per-row move semantics cannot
  apply: while `rowBlocks` is passed, every gesture — standalone rows included — reports through
  `onCommit` (a standalone move carries `blockId == null`), and `onRowMove` is never invoked.
- **`onCommit == null` disables whole-block drag**: a block cannot be dragged as a unit (standalone
  rows do not drag either, even when `onRowMove` is set). Within-block row reorder is independent —
  enable it via `onRowReorderWithinBlock`; a block with neither is display-only.

## Drag handles: the whole block from its header, rows within a block

Dragging is split across two levels:

- **The whole block** is dragged from a handle in its **header**. The `blockHeader` slot runs in a
  `RowBlockHeaderScope`, so a `draggableHandle()` there moves the entire block:

```kotlin
blockHeader = { blockId, _ ->
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.DragIndicator,
            contentDescription = "Drag block",
            modifier = Modifier.draggableHandle(),
        )
        Text(blockId.toString())
    }
}
```

- **A row within a block** is dragged from a handle in its own cell. Every row carries one; the
  drag is constrained to its block — a row can never leave it, and the move reports through
  `onRowReorderWithinBlock`. The same cell handle drives a standalone row's move among units:

```kotlin
cell { _, _ ->
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().draggableHandle(),
    ) {
        Icon(Icons.Default.Reorder, contentDescription = "Drag row")
    }
}
```

The table wires each `draggableHandle()` to the right engine automatically — the header to the
whole-block drag, a block row's cell to the within-block drag, a standalone row's cell to the
unit drag. A block needs a header handle to be movable as a whole.

## Applying the commit: `applyRowBlockMove`

`applyRowBlockMove` is the lift from the rendered view back to the source list:

```kotlin
onCommit = { move ->
    people = people.toMutableList().apply {
        applyRowBlockMove(
            move = move,
            keyOf = { it.id },        // must mirror the table's rowKey
            blockOf = { it.groupId }, // must mirror RowBlocks.blockOf
        )
    }
}
```

Its semantics are deliberately stronger than "move the visible rows":

- it relocates **every** row whose `blockOf` equals `move.blockId` — including rows hidden by the
  current filter, which `movedKeys` cannot name — preserving their relative order;
- the insertion point expands to the nearest whole-block boundary in the source, so no other block
  is ever split;
- `afterKey` is the primary anchor; when its row is gone from the list by apply time, `beforeKey`
  pins the destination instead; when neither anchor resolves, the list is left untouched — guessing
  a position would reorder data the user never dragged;
- standalone moves (`blockId == null`) relocate just the moved keys.

Consumers with exotic placement needs can implement their own lift from the `RowBlockMove` keys —
the event carries everything the library knows.

For a within-block reorder, the matching lift is `applyRowReorderWithinBlock`:

```kotlin
onRowReorderWithinBlock = { move ->
    people = people.toMutableList().apply {
        applyRowReorderWithinBlock(
            move = move,
            keyOf = { it.id },        // must mirror the table's rowKey
            blockOf = { it.groupId }, // must mirror RowBlocks.blockOf
        )
    }
}
```

It relocates the single moved row among its block-mates using `afterKey` (primary) / `beforeKey`
(fallback); hidden block members keep their relative order, and the list is left untouched when
neither anchor resolves.

## Example

```kotlin
data class Person(val id: Int, val name: String, val groupId: String? = null)

enum class PersonColumn { Handle, Name }

@OptIn(ExperimentalTableApi::class)
@Composable
fun BlockedPeopleTable() {
    var people by remember {
        mutableStateOf(
            listOf(
                Person(1, "Alice"),
                Person(2, "Bob", groupId = "Team A"),
                Person(3, "Carol", groupId = "Team A"),
                Person(4, "Dave"),
            ),
        )
    }

    val columns =
        remember {
            tableColumns<Person, PersonColumn, Unit> {
                column(PersonColumn.Handle, valueOf = { it.id }) {
                    width(48.dp, 48.dp)
                    resizable(false)
                    cell { _, _ ->
                        // Every row carries a handle: a block row reorders within its block, a
                        // standalone row reorders among units.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().draggableHandle(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = "Drag to reorder",
                            )
                        }
                    }
                }

                column(PersonColumn.Name, valueOf = { it.name }) {
                    header("Name")
                    cell { person, _ -> Text(person.name) }
                }
            }
        }

    val state =
        rememberTableState(
            columns = PersonColumn.entries.toImmutableList(),
            settings = TableSettings(rowReorderEnabled = true),
        )

    // Declared once, held by identity. "Team A" renders as one block; Alice and Dave stay
    // standalone drag units.
    val rowBlocks =
        remember {
            RowBlocks<Person>(
                blockOf = { it.groupId },
                onCommit = { move ->
                    people = people.toMutableList().apply {
                        applyRowBlockMove(
                            move = move,
                            keyOf = { it.id },
                            blockOf = { it.groupId },
                        )
                    }
                },
                onRowReorderWithinBlock = { move ->
                    people = people.toMutableList().apply {
                        applyRowReorderWithinBlock(
                            move = move,
                            keyOf = { it.id },
                            blockOf = { it.groupId },
                        )
                    }
                },
                // The header carries the whole-block drag handle.
                blockHeader = { blockId, _ ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Drag block",
                            modifier = Modifier.draggableHandle(),
                        )
                        Text(
                            text = blockId.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                },
            )
        }

    Table(
        itemsCount = people.size,
        itemAt = { index -> people.getOrNull(index) },
        state = state,
        columns = columns,
        rowKey = { person, index -> person?.id ?: index },
        rowBlocks = rowBlocks,
    )
}
```

## Sorting: within blocks only

A free sort cannot compose with drag reordering. Under an active sort the rendered order is a
function of row *values*, not of row *positions* — the view is invariant to any permutation of the
source, so a committed move would change nothing on screen. This is a property of sorting, not a
policy the library chose, and it is why the sort UI is locked while reorder is enabled (see
[Row reordering](row-reordering.md)). A sort your own pipeline applies over blocked data
additionally fragments blocks: a plain `sortedBy` interleaves rows of different blocks, tearing one
logical block into several bands with duplicated headers; the library logs a warning naming the
block id when it detects this.

What does compose is sorting **within** blocks:

```kotlin
// Rows sort inside their block; each unit — a block or a standalone row — takes its position
// from its minimal member, so blocks never fragment. The sort is stable.
val sorted = people.sortedWithinRowBlocks({ it.groupId }, compareBy { it.name })
```

Apply it **one-shot to the source list** — rewrite the list, then drop the sort — rather than
keeping it as a persistent render-time projection. A live projection re-pins unit order on every
emission, so the next committed drag is immediately "sorted back" and looks like it did nothing.
A pipeline that must keep the projection while a sort toggle is active has to clear the sort as
soon as a move commits — that clear is what makes the move visible.

## Filtering: hidden members travel with the block

Filtering stays fully available with blocks and drag. Blocks derive from the list you render, so a
filtered list simply produces blocks over its visible rows — hiding a middle member does not split
a block, because the remaining members are adjacent in the rendered list. When a filter hides part
of a block:

- the block renders — and drags — as its visible rows;
- `RowBlockMove.movedKeys` names the **visible** members only;
- `applyRowBlockMove` still relocates the **whole** block in the source: hidden members travel with
  it, and clearing the filter shows them at the destination.

To filter at block granularity instead — a block survives in full when *any* member matches — use:

```kotlin
val displayed = people.filteredWholeRowBlocks({ it.groupId }, { it.name.contains(query) })
```

## Paged tables

The `table-paging` adapter takes the same `rowBlocks` parameter with the same declaration and the
same commit event. Bands derive over loaded adjacent runs — an unloaded placeholder breaks a band,
and a partially loaded block extends as its pages arrive. With `onCommit` set, a drop commits only
when its landing neighbours are loaded; against a placeholder the gesture snaps back and nothing is
emitted. A paged consumer holds no materialized source list, so `applyRowBlockMove` does not apply:
forward the `RowBlockMove` to your data layer and apply it there by `blockId` — the data layer
knows full block membership, including rows the client never loaded. See
[table-paging](../modules/table-paging.md).

```kotlin
// Person and the column definitions are the ones from the example above;
// the Table here is the ua.wwind.table.paging adapter.
@Composable
fun PagedBlockedPeopleTable(
    paging: PagingData<Person>?,
    columns: ImmutableList<ColumnSpec<Person, PersonColumn, Unit>>,
    viewModel: PeopleViewModel,
) {
    val state =
        rememberTableState(
            columns = PersonColumn.entries.toImmutableList(),
            settings = TableSettings(rowReorderEnabled = true),
        )

    // The declaration is identical to the in-memory table — only what onCommit does differs.
    val rowBlocks =
        remember {
            RowBlocks<Person>(
                blockOf = { it.groupId },
                // No materialized source list here, so applyRowBlockMove cannot run: forward the
                // event instead. The data layer relocates the whole block by move.blockId — it
                // knows full membership, including rows this client never loaded — and the new
                // order flows back as refreshed pages.
                onCommit = { move -> viewModel.moveBlock(move) },
                blockHeader = { blockId, _ ->
                    Text(
                        text = blockId.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                },
            )
        }

    Table(
        items = paging,
        state = state,
        columns = columns,
        // Placeholder keys must not collide with loaded ids, so namespace the fallback.
        rowKey = { person, index -> person?.id?.toString() ?: "_$index" },
        rowBlocks = rowBlocks,
    )
}
```

## The header band

`blockHeader` renders in the band above each block. It receives the block id and the block's
current row range, and is offset by the horizontal scroll so it stays in the viewport while the
table scrolls sideways. The slot runs in a `RowBlockHeaderScope`, so a `Modifier.draggableHandle()`
inside it is the whole block's drag handle. The library adds **no click handling** of its own —
attach your own `Modifier.clickable` inside the slot for renaming, dissolving, or anything else:

```kotlin
blockHeader = { blockId, _ ->
    Text(
        text = blockId.toString(),
        modifier = Modifier
            .clickable { renameGroup(blockId.toString()) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
```

## Visuals

- `TableDimensions.rowBlockSpacing` (default `8.dp`) — the vertical gap around a block.
- `TableColors.rowBlockContainerColor` — the tint painted behind it. Defaults to
  `Color.Unspecified`, which resolves to `surfaceContainerHighest` at draw time, so a `TableColors`
  built without the parameter still gets a themed band.

Rows keep their own dividers, the block's last row included; the block adds none. The tinted band
is purely the block's **margin**, which is what makes it legible — each row paints its own opaque
surface, so only the gap shows the tint.

A block always **opens** with a band — the header when configured, otherwise a `rowBlockSpacing`
gap — and **closes** with a gap only when the next unit is not a block. Two adjacent blocks
therefore get exactly one band between them instead of two stacked into a single fat slab, and a
block at either end of the list still keeps its outer band.

To style the rows *inside* a block, use `TableRowContext.isInRowBlock` from your
`TableCustomization` — it is true exactly for block member rows.

## Interaction with `groupBy`

`rowBlocks` and `state.groupBy` describe two different structures over one list, so they do not
compose: while `groupBy` is active, `rowBlocks` is **suppressed** — rows render with no block
structure or band, a warning is logged, and row drag is disabled entirely: `onCommit` is not
invoked, and `onRowMove` stays superseded rather than taking over. `groupBy` wins because refusing
a grouping menu click with an exception would be worse. The conflict is surfaced on both sides:

- `TableState.rowBlocksSuppressedByGroupBy` is true while the suppression is active, so your UI can
  explain why blocks disappeared instead of leaving them to vanish silently;
- the column menu's group-by item is disabled while the table derives at least one block, so the
  suppression cannot be triggered by a stray menu click.

See [Data grouping](grouping.md) for the grouping feature itself.

## External changes during a drag

If the rendered list changes mid-gesture — an external update arriving through your pipeline — the
gesture is cancelled and the view resets to the new list: swap geometry computed against the old
list is meaningless over the new one. After a drop, the table keeps the moved order optimistically
(no snap-back) until your applied move flows back through the data you render.

## Checklist

- `TableSettings(rowReorderEnabled = true)`; a cell `draggableHandle()` on every row (within-block /
  unit drag) and a `draggableHandle()` in `blockHeader` for whole-block drag.
- A **stable `rowKey`** — move anchors are row keys; the default positional key triggers a
  warning and cannot survive a move.
- `RowBlocks` held in `remember` — it compares by identity.
- Block members contiguous in the source list; the library warns on fragmented ids but cannot
  repair them.
- `onCommit` applies the whole-block move to the source with `applyRowBlockMove`, and
  `onRowReorderWithinBlock` applies the within-block move with `applyRowReorderWithinBlock` — both
  with `keyOf`/`blockOf` mirroring the table's `rowKey`/`RowBlocks.blockOf`, or, for paged tables,
  forwarded to the data layer to apply by `blockId`.
- No free sort over blocked data: `sortedWithinRowBlocks` one-shot on the source when needed.
- `groupBy` off — blocks are suppressed while it is active.
