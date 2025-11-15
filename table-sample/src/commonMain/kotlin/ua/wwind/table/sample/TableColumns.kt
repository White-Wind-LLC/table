package ua.wwind.table.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

/**
 * Create column definitions with titles, cells and optional filters for header UI.
 */
fun createTableColumns(): ImmutableList<ColumnSpec<Person, PersonColumn>> =
    tableColumns<Person, PersonColumn> {
        // Real Person fields
        column(PersonColumn.NAME, { it.name }) {
            title { "Name" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.name, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.AGE, { it.age }) {
            title { "Age" }
            autoWidth()
            sortable()
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 0 to 100,
                ),
            )
            align(Alignment.CenterEnd)
            cell { item ->
                Text(
                    item.age.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.ACTIVE, { it.active }) {
            title { "Active" }
            autoWidth()
            sortable()
            filter(TableFilterType.BooleanTableFilter())
            cell { item -> Text(if (item.active) "Yes" else "No", modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.ID, valueOf = { it.id }) {
            title { "ID" }
            autoWidth()
            sortable()
//            filter(
//                TableFilterType.NumberTableFilter(
//                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
//                    rangeOptions = 1 to 1000,
//                ),
//            )
            align(Alignment.CenterEnd)
            cell { item ->
                Text(
                    item.id.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.EMAIL, valueOf = { it.email }) {
            title { "Email" }
            autoWidth()
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.email, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.CITY, { it.city }) {
            title { "City" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.city, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.COUNTRY, { it.country }) {
            title { "Country" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.country, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.DEPARTMENT, { it.department }) {
            title { "Department" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.department, modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        column(PersonColumn.SALARY, { it.salary }) {
            title { "Salary" }
            autoWidth()
            sortable()
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 0 to 200000,
                ),
            )
            align(Alignment.CenterEnd)
            cell { item ->
                Text(
                    "$${item.salary}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        column(PersonColumn.RATING, { it.rating }) {
            title { "Rating" }
            autoWidth()
            sortable()
            filter(
                TableFilterType.NumberTableFilter(
                    delegate = TableFilterType.NumberTableFilter.IntDelegate,
                    rangeOptions = 1 to 5,
                ),
            )
            align(Alignment.Center)
            cell { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    repeat(item.rating) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        // Multiline text field to demonstrate dynamic row height
        column(PersonColumn.NOTES, { it.notes }) {
            title { "Notes" }
            // Let the row grow by content; optionally set bounds in dynamic mode
            rowHeight(min = 48.dp, max = 200.dp)
            autoWidth(500.dp)
            filter(TableFilterType.TextTableFilter())
            cell { item ->
                Text(item.notes, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        // Computed fields
        val ageGroup = { item: Person ->
            when {
                item.age < 25 -> "<25"
                item.age < 35 -> "25-34"
                else -> "35+"
            }
        }
        column(PersonColumn.AGE_GROUP, ageGroup) {
            title { "Age group" }
            autoWidth(500.dp)
            sortable()
            cell { item ->
                Text(ageGroup(item), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
