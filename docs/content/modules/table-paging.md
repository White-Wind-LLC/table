# table-paging

The `table-paging` module provides an adapter on top of the core table for `PagingData` (`ua.wwind.paging`).

```kotlin
@Composable
fun PeoplePagingTable(paging: PagingData<Person>) {
    Table(
        items = paging,
        state = state,
        columns = columns,
    )
}
```

There is also `LazyListScope.handleLoadState(...)` to render loading/empty states.

## Row blocks

The adapter forwards the same `rowBlocks: RowBlocks<T>?` parameter as the core table, with the same
declaration and the same commit event — see the
[row blocks guide](../guides/row-reordering.md#row-blocks-dragging-adjacent-rows-as-one-unit).
What differs is what paging can and cannot know:

- **Bands derive over loaded runs.** Block extents are derived from loaded adjacent rows on each
  snapshot; an unloaded placeholder breaks a run, so a partially loaded block renders a partial
  band that extends as its pages arrive.
- **Display-only without `onCommit`.** Blocks render their bands; nothing drags as a block.
- **Paged drop policy.** With `onCommit` set, a drop commits only when the landing neighbours are
  loaded — their keys anchor the move. Against a placeholder the gesture cancels and the block
  snaps back to its origin; no event is emitted. Holding the drag over the landing spot is the
  natural retry: rendering the placeholders there is what makes their page load. Pages loading
  under the held pointer do not cancel the gesture.
- **A partially loaded block drags as its loaded fragment.** `RowBlockMove.movedKeys` carries
  loaded members only.
- **No client-side list apply.** `applyRowBlockMove` needs the materialized source list, which a
  paged consumer does not hold. The commit event is semantic — `blockId` plus key anchors — so
  forward it to your data layer and apply the move there by `blockId`: the data layer knows full
  block membership, including rows the client never loaded.

The adapter call has the same shape as the core table; only `onCommit` differs — it forwards to the
data layer instead of applying to a local list:

```kotlin
val rowBlocks =
    remember {
        RowBlocks<Person>(
            blockOf = { it.teamId },
            // No materialized list for applyRowBlockMove: the data layer relocates the whole
            // block by move.blockId — it knows full membership, including rows never loaded
            // here — and the new order arrives back as refreshed pages.
            onCommit = { move -> viewModel.moveBlock(move) },
            blockHeader = { blockId, _ -> Text(blockId.toString()) },
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
```

As in the core table, blocks require a stable `rowKey` (the default positional key triggers a
warning), and `RowBlocks` should be held in `remember`.
