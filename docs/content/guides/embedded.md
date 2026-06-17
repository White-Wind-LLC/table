# Embedded tables

By default a `Table` owns a bounded area and scrolls its rows internally with its own
`LazyListState`. Set `embedded = true` to make the table render at its **full intrinsic height**
with **no internal vertical scrolling** — every row is laid out at once. Use this when the table
lives inside an already‑scrollable container, so a single outer scrollbar drives everything instead
of nesting a scroll area inside another scroll area.

```kotlin
Table(
    itemsCount = items.size,
    itemAt = { index -> items.getOrNull(index) },
    state = state,
    columns = columns,
    embedded = true, // render full height, no internal scroll
)
```

## When to use it

- The table is one section of a longer screen that already scrolls (e.g. a `Column` with
  `verticalScroll`, or a `LazyColumn` item).
- You are building a **master–detail** layout where a child table is shown inside a parent row via
  the [`rowEmbedded`](#relation-to-rowembedded) slot.
- You want the whole table to be captured as one block (printing, screenshots, export).

If instead the table should stay a fixed size and scroll its own rows, keep the default
`embedded = false`.

## What changes in embedded mode

When `embedded = true`:

- **No internal scrolling.** All rows are composed immediately and the table grows to fit its
  content; the outer container is responsible for scrolling.
- **Footer is never pinned.** A footer row (when `showFooter = true`) renders inline at the end of
  the table rather than being pinned to the bottom, since there is no fixed viewport to pin to.
- **Auto‑width is measured eagerly.** Because every row is present from the start, auto‑width
  columns are measured across all rows at once instead of from the first visible batch.
- **Drag‑to‑scroll is inert.** With no internal scroll area there is nothing to pan; the outer
  container handles gestures.

## Example: a nested detail table

This master–detail section renders a person's movements as a full‑height table inside a scrollable
detail pane. Note the compact dimensions and dynamic row height, which pair well with embedding.

```kotlin
@OptIn(ExperimentalTableApi::class)
@Composable
fun PersonMovementsSection(person: Person, modifier: Modifier = Modifier) {
    val columns = remember { createMovementColumns() }
    val settings = remember {
        TableSettings(
            selectionMode = SelectionMode.None,
            rowHeightMode = RowHeightMode.Dynamic,
            enableDragToScroll = false,
            showFooter = true,
        )
    }
    val state = rememberTableState(
        columns = PersonMovementColumn.entries.toImmutableList(),
        settings = settings,
        dimensions = TableDefaults.compactDimensions(),
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text("HR movements", style = MaterialTheme.typography.headlineMedium)

        Table(
            itemsCount = person.movements.size,
            itemAt = { index -> person.movements.getOrNull(index) },
            state = state,
            tableData = person,
            columns = columns,
            rowKey = { item, index -> item?.date ?: index },
            embedded = true,
        )
    }
}
```

Because the table is embedded, the surrounding `Column` (or whatever scrollable parent hosts this
section) provides the single scroll axis for the whole pane.

## Relation to `rowEmbedded`

Two related parameters cover nested layouts; do not confuse them:

- **`embedded: Boolean`** — described here: controls how the table *itself* is sized and scrolled.
- **`rowEmbedded: @Composable (rowIndex: Int, item: T) -> Unit`** — a per‑row slot for rendering
  extra detail content (or even a nested child table) *underneath* a given row, enabling
  expandable master–detail rows.

They compose: a parent table can use `rowEmbedded` to host a child table, and that child table is
typically created with `embedded = true` so it grows to its full height inside the parent row.
