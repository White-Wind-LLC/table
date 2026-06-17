### Data Table for Compose Multiplatform (Material 3)

[![Maven Central](https://img.shields.io/maven-central/v/ua.wwind.table-kmp/table-core)](https://central.sonatype.com/artifact/ua.wwind.table-kmp/table-core)

Compose Multiplatform data table with Material 3 look & feel. Includes a core table (`table-core`), a conditional
formatting add‑on (`table-format`), and paging integration (`table-paging`).

![Data Table Example](docs/content/images/datatable-example.png)

## Documentation

Full documentation lives at **https://white-wind-llc.github.io/table/** —
[Getting started](https://white-wind-llc.github.io/table/getting-started/installation/) ·
[Guides](https://white-wind-llc.github.io/table/guides/cell-editing/) ·
[API reference](https://white-wind-llc.github.io/table/api/) ·
[Live demo](https://white-wind-llc.github.io/table/demo/)

## Installation

Add the modules you need from Maven Central:

```kotlin
dependencies {
    implementation("ua.wwind.table-kmp:table-core:1.9.0")
    // optional
    implementation("ua.wwind.table-kmp:table-format:1.9.0")
    implementation("ua.wwind.table-kmp:table-paging:1.9.0")
}
```

## Quick start

```kotlin
data class Person(val name: String, val age: Int)
enum class PersonField { Name, Age }

val columns = tableColumns<Person, PersonField, Unit> {
    column(PersonField.Name, valueOf = { it.name }) {
        header("Name")
        cell { person, _ -> Text(person.name) }
        sortable()
        filter(TableFilterType.TextTableFilter())
    }
    column(PersonField.Age, valueOf = { it.age }) {
        header("Age")
        cell { person, _ -> Text(person.age.toString()) }
        sortable()
    }
}

val state = rememberTableState(
    columns = columns.map { it.key },
    settings = TableSettings(stripedRows = true),
)

@OptIn(ExperimentalTableApi::class)
@Composable
fun PeopleTable(items: List<Person>) {
    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = state,
        columns = columns,
    )
}
```

## License

See [LICENSE](LICENSE).
