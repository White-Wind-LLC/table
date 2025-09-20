package ua.wwind.table.sample

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

/**
 * Create column definitions with titles, cells and optional filters for header UI.
 */
fun createTableColumns(): List<ColumnSpec<Person, PersonColumn>> {
    return tableColumns<Person, PersonColumn> {
        // Real Person fields
        column(PersonColumn.NAME) {
            header("Name")
            title { "Name" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.name) }
        }
        column(PersonColumn.AGE) {
            header("Age")
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
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        column(PersonColumn.ACTIVE) {
            header("Active")
            title { "Active" }
            filter(TableFilterType.BooleanTableFilter())
            cell { item -> Text(if (item.active) "Yes" else "No") }
        }
        column(PersonColumn.ID) {
            header("ID")
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
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        column(PersonColumn.EMAIL) {
            header("Email")
            title { "Email" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.email) }
        }
        column(PersonColumn.CITY) {
            header("City")
            title { "City" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.city) }
        }
        column(PersonColumn.COUNTRY) {
            header("Country")
            title { "Country" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.country) }
        }
        column(PersonColumn.DEPARTMENT) {
            header("Department")
            title { "Department" }
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.department) }
        }
        column(PersonColumn.SALARY) {
            header("Salary")
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
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        column(PersonColumn.RATING) {
            header("Rating")
            title { "Rating" }
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 1 to 5,
                ),
            )
            align(Alignment.End)
            cell { item ->
                Text(
                    "${"★".repeat(item.rating)}",
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Computed fields
        column(PersonColumn.AGE_GROUP) {
            header("Age group")
            title { "Age group" }
            filter(TableFilterType.TextTableFilter())
            cell { item ->
                val group = when {
                    item.age < 25 -> "<25"
                    item.age < 35 -> "25-34"
                    else -> "35+"
                }
                Text(group)
            }
        }
    }
}