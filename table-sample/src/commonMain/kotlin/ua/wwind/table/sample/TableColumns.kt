package ua.wwind.table.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.sample.filter.createSalaryRangeFilter
import ua.wwind.table.tableColumns

/**
 * Create column definitions with titles, cells and optional filters for header UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun createTableColumns(
    onToggleMovementExpanded: (personId: Int) -> Unit,
    allPeople: List<Person>,
    onEvent: (SampleUiEvent) -> Unit,
): ImmutableList<ColumnSpec<Person, PersonColumn, PersonEditState>> =
    editableTableColumns<Person, PersonColumn, PersonEditState> {
        column(PersonColumn.EXPAND, valueOf = { it.expandedMovement }) {
            title { "Movements" }
            autoWidth(40.dp)
            resizable(false)
            cell { item ->
                IconButton(onClick = { onToggleMovementExpanded(item.id) }) {
                    if (item.expandedMovement) {
                        Icon(
                            imageVector = Icons.Filled.ExpandLess,
                            contentDescription = "Collapse movements",
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Expand movements",
                        )
                    }
                }
            }
        }

        // Real Person fields
        column(PersonColumn.NAME, { it.name }) {
            title { "Name" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item -> Text(item.name, modifier = Modifier.padding(horizontal = 16.dp)) }

            // Editing configuration - table will manage when to show this
            editCell { person, editState, onComplete ->
                var text by remember(person) { mutableStateOf(person.name) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it
                        onEvent(SampleUiEvent.UpdateName(it))
                    },
                    errorMessage = editState.nameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                onComplete()
                            },
                        ),
                )
            }
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

            // Editing configuration
            editCell { person, editState, onComplete ->
                var text by remember(person) { mutableStateOf(person.age.toString()) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it.filter { char -> char.isDigit() }
                        it.toIntOrNull()?.let { age -> onEvent(SampleUiEvent.UpdateAge(age)) }
                    },
                    errorMessage = editState.ageError,
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                onComplete()
                            },
                        ),
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
        column(PersonColumn.POSITION, { it.position }) {
            title { "Position" }
            autoWidth(500.dp)
            sortable()
            filter(
                TableFilterType.EnumTableFilter(
                    options = Position.entries.toImmutableList(),
                    getTitle = { it.displayName },
                ),
            )
            cell { item -> Text(item.position.displayName, modifier = Modifier.padding(horizontal = 16.dp)) }

            // Editing configuration with dropdown
            editCell { person, editState, onComplete ->
                var expanded by remember { mutableStateOf(false) }
                var selectedPosition by remember(person) { mutableStateOf(person.position) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TableCellTextField(
                        value = selectedPosition.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier =
                            Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        Position.entries.forEach { position ->
                            DropdownMenuItem(
                                text = { Text(position.displayName) },
                                onClick = {
                                    selectedPosition = position
                                    onEvent(SampleUiEvent.UpdatePosition(position))
                                    expanded = false
                                    onComplete()
                                },
                            )
                        }
                    }
                }
            }
        }
        column(PersonColumn.SALARY, { it.salary }) {
            title { "Salary" }
            width(350.dp, 350.dp)
            sortable()
            // Custom visual range filter with histogram
            filter(createSalaryRangeFilter(allPeople))
            align(Alignment.CenterEnd)
            cell { item ->
                Text(
                    "$${item.salary}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Editing configuration
            editCell { person, editState, onComplete ->
                var text by remember(person) { mutableStateOf(person.salary.toString()) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it.filter { char -> char.isDigit() }
                        it.toIntOrNull()?.let { salary -> onEvent(SampleUiEvent.UpdateSalary(salary)) }
                    },
                    errorMessage = editState.salaryError,
                    singleLine = true,
                    prefix = { Text("$") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                onComplete()
                            },
                        ),
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
        column(PersonColumn.HIRE_DATE, { it.hireDate }) {
            title { "Hire Date" }
            autoWidth()
            sortable()
            filter(TableFilterType.DateTableFilter())
            cell { item ->
                Text(
                    item.hireDate.format(
                        LocalDate.Format {
                            day()
                            chars(".")
                            monthNumber()
                            chars(".")
                            year()
                        },
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
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

fun createMovementColumns(): ImmutableList<ColumnSpec<PersonMovement, PersonMovementColumn, Unit>> =
    tableColumns<PersonMovement, PersonMovementColumn> {
        column(PersonMovementColumn.DATE, valueOf = { it.date }) {
            title { "Date" }
            autoWidth()
            cell { movement ->
                Text(
                    movement.date.format(
                        LocalDate.Format {
                            day()
                            chars(".")
                            monthNumber()
                            chars(".")
                            year()
                        },
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        column(PersonMovementColumn.FROM_POSITION, valueOf = { it.fromPosition }) {
            title { "From" }
            autoWidth()
            cell { movement ->
                Text(
                    movement.fromPosition?.displayName ?: "-",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        column(PersonMovementColumn.TO_POSITION, valueOf = { it.toPosition }) {
            title { "To" }
            autoWidth()
            cell { movement ->
                Text(
                    movement.toPosition.displayName,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
