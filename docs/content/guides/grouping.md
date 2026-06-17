# Data grouping

Group table data by any column to organize and visualize hierarchical relationships:

```kotlin
// Enable grouping programmatically
state.groupBy = PersonField.Department

// Or let users group via header dropdown menu
// (automatically available for all columns)
```

Customize group header appearance and content:

```kotlin
column(PersonField.Department, valueOf = { it.department }) {
    header("Department")
    cell { person, _ -> Text(person.department) }

    // Custom group header renderer
    groupHeader { groupValue ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(Icons.Default.Group, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Department: $groupValue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

Group headers are sticky and remain visible during scrolling. Configure group content alignment via table settings:

```kotlin
val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(
        groupContentAlignment = Alignment.CenterStart,
        // ... other settings
    )
)
```
