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
