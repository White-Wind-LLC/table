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

## Row keys never move the pager

`PagingMap.get` is what tells a pager where the viewport is, and a lazy list asks for row keys over a
range far wider than the viewport — Compose's nearest range is 130 rows from the top of the list,
against the twenty or so on screen. A table that resolved a row to build each key therefore reported
a viewport that wide, and the pager streamed and cached around it: with a window narrower than the
range being read, the two ends evicted each other and the table reloaded forever.

Nothing to configure — the adapter keys rows without touching the accessor the pager acts on:

- the default positional key needs no row at all;
- a `rowKey` of your own is answered from the rows already loaded in the paging map, so it sees
  exactly what the accessor would return, minus the access.

Keying the list no longer reports a viewport at all. The other half of that loop was a pager bug and
is fixed in `paging-core` 2.3.1 ([paging-kmp#45](https://github.com/White-Wind-LLC/paging-kmp/issues/45));
either fix alone stops the reload loop, and `table-paging` is built against 2.3.1.

## Row blocks

The adapter forwards the same `rowBlocks: RowBlocks<T>?` parameter as the core table, with the same
declaration and the same commit event — see the
[row blocks guide](../guides/row-blocks.md).
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

## Compose stability

`paging-core` is built without the Compose compiler plugin — that is what keeps it usable from
plain Kotlin and free of a Compose runtime dependency. The compiler only infers stability for
classes it compiles itself, so left alone it treats `PagingData` and everything around it as
**unstable**, and strong skipping then compares such a parameter by instance instead of by value.
The pagers publish a fresh snapshot per state change, so a composable taking one recomposes on
every emission however little of the window actually changed.

`table-paging` declares those types stable for its own compilation, so the `Table` overloads above
are already covered. A stability configuration only governs the module doing the compiling, so if
your own composables take a `PagingData` — a screen, a view-model-bound wrapper, a `LazyColumn`
using `handleLoadState` — you need the same file in your build.

`paging-core` ships it as
[`compose_compiler_config.conf`](https://github.com/White-Wind-LLC/paging-kmp/blob/main/compose_compiler_config.conf)
since 2.3.0. Copy it into your repository and point the Compose compiler at it from every module
that touches a pager:

```kotlin
composeCompiler {
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("compose_compiler_config.conf"),
    )
}
```

Your row type still has to be stable in its own right for a row to be skippable, which it is by
default for a `data class` of `val`s compiled in your own module. To check what the compiler makes
of it, set `composeCompiler { reportsDestination.set(...) }` and read the generated
`*-composables.txt`: `items` should read `stable items: PagingData<T>?`.
