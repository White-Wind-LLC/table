package ua.wwind.table.sample

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

/**
 * Create column definitions with titles, cells and optional filters for header UI.
 */
fun createTableColumns(): List<ColumnSpec<Person, PersonColumn>> =
    tableColumns<Person, PersonColumn> {
        // Real Person fields
        column(PersonColumn.NAME) {
            header { Text("Name", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Name" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.name, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.AGE) {
            header { Text("Age", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Age" }
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 0 to 100,
                ),
            )
            align(Alignment.End)
            cell { item ->
                Text(
                    item.age.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.ACTIVE) {
            header { Text("Active", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Active" }
            filter(TableFilterType.BooleanTableFilter())
            cell { item -> Text(if (item.active) "Yes" else "No", modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.ID) {
            header { Text("ID", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "ID" }
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 1 to 1000,
                ),
            )
            align(Alignment.End)
            cell { item ->
                Text(
                    item.id.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.EMAIL) {
            header { Text("Email", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Email" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.email, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.CITY) {
            header { Text("City", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "City" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.city, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.COUNTRY) {
            header { Text("Country", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Country" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.country, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.DEPARTMENT) {
            header { Text("Department", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Department" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.department, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.SALARY) {
            header { Text("Salary", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Salary" }
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 0 to 200000,
                ),
            )
            align(Alignment.End)
            cell { item ->
                Text(
                    "$${item.salary}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.RATING) {
            header { Text("Rating", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Rating" }
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 1 to 5,
                ),
            )
            align(Alignment.CenterHorizontally)
            cell { item ->
                Text(
                    "★".repeat(item.rating),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Computed fields
        column(PersonColumn.AGE_GROUP) {
            header { Text("Age group", modifier = Modifier.padding(horizontal = 16.dp)) }
            title { "Age group" }
            cell { item ->
                val group =
                    when {
                        item.age < 25 -> "<25"
                        item.age < 35 -> "25-34"
                        else -> "35+"
                    }
                Text(group, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
