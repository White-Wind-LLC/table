# Row reordering with Reorderable

The library now provides a dedicated row reordering flow powered
by [Reorderable](https://github.com/Calvin-LL/Reorderable).

![Row reordering example](../images/row-reordering.gif)

- **Enable it**: set `TableSettings(rowReorderEnabled = true)`.
- **Handle moves**: pass `onRowMove = { fromIndex, toIndex -> ... }` to `Table` or `EditableTable`.
- **Enable compiler support**: add `-Xcontext-parameters` in the consuming module, because row drag handles are exposed
  through Kotlin context parameters.
- **How context is passed**: the `cell { ... }` DSL is backed by `context(TableCellScope)`, so inside a cell you can
  call `Modifier.draggableHandle()` or `Modifier.longPressDraggableHandle()` directly.
- **Interaction rules**: while row reorder mode is active, sorting and grouping interactions are disabled and
  `initialSort` is ignored.
- **Embedded support**: the same API works for embedded table bodies too.

Example:

```kotlin
data class Person(val id: Int, val name: String)

enum class PersonColumn { Handle, Name }

@Composable
fun ReorderablePeopleTable() {
    val people = remember {
        mutableStateListOf(
            Person(1, "Alice"),
            Person(2, "Bob"),
            Person(3, "Charlie"),
        )
    }

    val columns =
        remember {
            tableColumns<Person, PersonColumn, Unit> {
                column(PersonColumn.Handle, valueOf = { it.id }) {
                    width(48.dp, 48.dp)
                    resizable(false)
                    cell { _, _ ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = "Drag row",
                                modifier = Modifier.draggableHandle(),
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

    // Required in the consuming module:
    // compilerOptions { freeCompilerArgs.add("-Xcontext-parameters") }
    //
    // `cell { ... }` receives `context(TableCellScope)`, which is why
    // `Modifier.draggableHandle()` is available directly inside the cell lambda.
    val state =
        rememberTableState(
            columns = PersonColumn.entries.toImmutableList(),
            settings = TableSettings(rowReorderEnabled = true),
        )

    Table(
        itemsCount = people.size,
        itemAt = { index -> people.getOrNull(index) },
        state = state,
        columns = columns,
        onRowMove = { fromIndex, toIndex ->
            if (fromIndex !in people.indices || people.isEmpty()) return@Table

            val targetIndex = toIndex.coerceIn(0, people.lastIndex)
            if (fromIndex == targetIndex) return@Table

            val movedItem = people.removeAt(fromIndex)
            people.add(targetIndex, movedItem)
        },
    )
}
```

## Dragging a group of rows

A range of **adjacent** rows can be declared as one drag unit: dragging it carries the whole block
instead of a single row. Pass a `TableRowGroups` as `rowGroups` to `Table` or `EditableTable`:

```kotlin
@Stable
public class TableRowGroups(
    // Which rows form each block: sorted, non-empty, non-overlapping, within `itemsCount`.
    public val ranges: ImmutableList<IntRange>,
    // A block was moved; `from.first` is the leader row.
    public val onMove: ((from: IntRange, to: IntRange) -> Unit)? = null,
    // Content for the band above a block.
    public val header: (@Composable (rows: IntRange) -> Unit)? = null,
)
```

Whether a group exists, what it is called, and how it is created or dissolved stay in your own data
and UI. The table only moves the block and draws a band around it.

- **Ranges are derived state.** Compute them from the same data the table renders with
  `rowGroupsOf`; never maintain range state by hand.
- **Hold the `TableRowGroups` in `remember`.** It is `@Stable` and compares **by identity** — a
  fresh instance on every recomposition makes the `rowGroups` argument look changed and stops
  `Table` from skipping. This is stricter than `TableSettings`, which is a data class whose
  structural equality forgives a new instance.
- **`onMove` is required** when row reorder is enabled and `ranges` is non-empty; a `require` fails
  loudly otherwise, rather than letting the drag break silently. With row reorder disabled, groups
  still render their band and no callback is needed.
- **`onMove` supersedes `onRowMove`.** Once `onMove` is non-null, every drag — including one of a
  plain ungrouped row — is reported through it, and `onRowMove` is not invoked at all.

### Deriving the ranges

`rowGroupsOf` collapses each run of **adjacent** rows sharing the same non-null id into one range;
rows with a `null` id are never grouped. Two disjoint runs of the same id therefore produce **two**
ranges — a drag unit has to be contiguous on screen, so adjacency, not the id alone, defines a
block. This is why a filter that hides a row in the middle of a group splits it into two blocks.

Ranges must be derived from the same data snapshot as `itemAt`. In a scrolling table `onMove` fires
*during* the drag and the ranges must recompute in the same recomposition as the list, so key the
`remember` on the rendered data:

```kotlin
remember(people) { people.rowGroupsOf { it.groupId } }
```

Because `remember` keys compare by equality, this wants a list that is **replaced** on each change
(a new `List` from your state holder). A long-lived `mutableStateListOf` mutated in place never
invalidates such a key, and the ranges would silently lag the rows.

### Applying the move

Both ends of a move are ranges. **Do not collapse `to` to a single index.** A downward move inserts
the block *after* `to.last`; passing only the target's leader row drops the block *inside* the
target group and splits it. An upward move inserts at `to.first`, where the leader happens to be
correct — so the bug looks direction-specific and survives casual testing.

`moveRowGroup` implements the correct semantics for a `MutableList`:

```kotlin
onMove = { from, to ->
    people = people.toMutableList().apply { moveRowGroup(from = from, to = to) }
}
```

When the move must be applied depends on the table's mode, and the difference comes from the
underlying [Reorderable](https://github.com/Calvin-LL/Reorderable) library rather than from this
one — it already applies to `onRowMove` today:

- **Scrolling table** (`embedded = false`): `onMove` fires **repeatedly during** the drag, and the
  consumer must apply the move to its list **synchronously**. The block follows the cursor only
  because the data moves under it.
- **Embedded table** (`embedded = true`): `onMove` fires **once, on drop**, so the move is applied
  at settle time.

### Putting the handle on the leader row

The library does not place the drag handle — any handle inside the block drags the whole block. The
usual choice is to render it only on the group's first row, which you detect with the same adjacency
rule `rowGroupsOf` uses:

```kotlin
cell { person, rows ->
    val index = rows.indexOfFirst { it.id == person.id }
    val isLeader =
        person.groupId == null || index <= 0 || rows[index - 1].groupId != person.groupId
    if (isLeader) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().draggableHandle(),
        ) {
            Icon(Icons.Default.Reorder, contentDescription = "Drag group")
        }
    }
}
```

### The header band

`header` renders in the band above each block and is offset by the horizontal scroll, so it stays in
the viewport while the table scrolls sideways. The library adds **no click handling** of its own —
attach your own `Modifier.clickable` inside the slot for renaming, dissolving, or anything else:

```kotlin
header = { rows ->
    Text(
        text = people[rows.first].groupId.orEmpty(),
        modifier = Modifier
            .clickable { renameGroup(people[rows.first].groupId) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
```

### Visuals

- `TableDimensions.rowGroupSpacing` (default `8.dp`) — the vertical gap around a block.
- `TableColors.rowGroupContainerColor` — the tint painted behind it.

Rows keep their own dividers, the group's last row included; the block adds none. The tinted band is
purely the group's **margin**, which is what makes it legible — each row paints its own opaque
surface, so only the gap shows the tint.

A block always **opens** with a band — the header when configured, otherwise a `rowGroupSpacing`
gap — and **closes** with a gap only when the next unit is not a group. Two adjacent blocks
therefore get exactly one band between them instead of two stacked into a single fat slab, and a
block at either end of the list still keeps its outer band.

### Interaction with `groupBy`

`rowGroups` and `state.groupBy` describe two different structures over one list, so they do not
compose: while `groupBy` is active, `rowGroups` is **ignored**, a warning is logged, and rows render
ungrouped with no band. `groupBy` wins because refusing a grouping menu click with an exception
would be worse. Note that grouping interactions are already disabled while row reorder mode is
active, so the two only meet when reorder is off.

### Example

```kotlin
data class Person(val id: Int, val name: String, val groupId: String? = null)

enum class PersonColumn { Handle, Name }

@OptIn(ExperimentalTableApi::class)
@Composable
fun GroupedPeopleTable() {
    // Every change replaces the list, so `remember(people)` recomputes the ranges along with it.
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

    // The current list is passed as table data, so cells can locate their own row.
    val columns =
        remember {
            tableColumns<Person, PersonColumn, List<Person>> {
                column(PersonColumn.Handle, valueOf = { it.id }) {
                    width(48.dp, 48.dp)
                    resizable(false)
                    cell { person, rows ->
                        val index = rows.indexOfFirst { it.id == person.id }
                        val isLeader =
                            person.groupId == null ||
                                index <= 0 ||
                                rows[index - 1].groupId != person.groupId
                        if (isLeader) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize().draggableHandle(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Reorder,
                                    contentDescription = "Drag group",
                                )
                            }
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

    val rowGroups =
        remember(people) {
            TableRowGroups(
                // "Team A" is rows 1..2 — Alice and Dave stay single-row units.
                ranges = people.rowGroupsOf { it.groupId },
                onMove = { from, to ->
                    // Fires during the drag: apply it now, and keep both ranges intact.
                    people = people.toMutableList().apply { moveRowGroup(from = from, to = to) }
                },
                header = { rows ->
                    Text(
                        text = people[rows.first].groupId.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                },
            )
        }

    Table(
        itemsCount = people.size,
        itemAt = { index -> people.getOrNull(index) },
        state = state,
        columns = columns,
        tableData = people,
        rowKey = { item, index -> item?.id ?: index },
        rowGroups = rowGroups,
    )
}
```
