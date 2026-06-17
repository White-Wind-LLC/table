# Footer row

Display a summary footer row at the bottom of the table with custom content per column. Footer receives table data as a
parameter, allowing access to displayed items and other table state:

```kotlin
data class PersonTableData(
    val displayedPeople: List<Person>,
    val editState: PersonEditState,
)

val columns = tableColumns<Person, PersonField, PersonTableData> {
    column(PersonField.Name, valueOf = { it.name }) {
        header("Name")
        cell { person, _ -> Text(person.name) }

        // Footer content with access to table data (Unit for non-editable tables)
        footer { tableData ->
            Text(
                text = "Total: ${tableData.displayedPeople.size}",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

Configure footer behavior via table settings:

```kotlin
val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(
        showFooter = true,      // Enable footer display
        footerPinned = true,    // Pin footer at bottom (default)
        // ... other settings
    )
)
```

Footer options:

- **showFooter**: Enable or disable footer row display.
- **footerPinned**: When `true` (default), footer stays visible at the bottom of the table viewport, similar to a sticky
  header. When `false`, footer scrolls with table content.
- **footerHeight**: Customize footer height via `TableDimensions.footerHeight`.
- **footerColors**: Customize footer colors via `TableColors.footerContainerColor` and `TableColors.footerContentColor`.

The footer:

- Respects column widths and alignment settings from the main table.
- Supports pinned columns just like header and body rows.
- Synchronizes horizontal scrolling with the rest of the table.
- For embedded tables, footer is always non-pinned and scrolls with content.
