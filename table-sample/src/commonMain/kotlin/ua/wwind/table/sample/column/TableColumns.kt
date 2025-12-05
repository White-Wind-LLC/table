package ua.wwind.table.sample.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import ua.wwind.table.sample.config.CellPadding
import ua.wwind.table.sample.filter.createSalaryRangeFilter
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.PersonMovement
import ua.wwind.table.sample.model.PersonMovementColumn
import ua.wwind.table.sample.model.PersonTableData
import ua.wwind.table.sample.model.Position
import ua.wwind.table.sample.viewmodel.SampleUiEvent
import ua.wwind.table.tableColumns

/** Create column definitions with titles, cells and optional filters for header UI. */
@OptIn(ExperimentalMaterial3Api::class)
fun createTableColumns(
    onToggleMovementExpanded: (personId: Int) -> Unit,
    onEvent: (SampleUiEvent) -> Unit,
    useCompactMode: Boolean = false,
): ImmutableList<ColumnSpec<Person, PersonColumn, PersonTableData>> =
    editableTableColumns {
        val cellPadding = if (useCompactMode) CellPadding.compact else CellPadding.standard
        val checkboxSize = if (useCompactMode) 36.dp else 48.dp

        // Selection checkbox column - visibility controlled via width in SampleApp
        column(PersonColumn.SELECTION, valueOf = { it.id }) {
            title { "" }
            width(checkboxSize, checkboxSize)
            resizable(false)
            cell { item, tableData ->
                if (tableData.selectionModeEnabled) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Checkbox(
                            checked = item.id in tableData.selectedIds,
                            onCheckedChange = {
                                onEvent(SampleUiEvent.ToggleSelection(item.id))
                            },
                        )
                    }
                }
            }
            header { tableData ->
                if (tableData.selectionModeEnabled) {
                    val displayedIds = tableData.displayedPeople.map { it.id }.toSet()
                    val selectedDisplayedCount = displayedIds.count { it in tableData.selectedIds }
                    val toggleState =
                        when (selectedDisplayedCount) {
                            0 -> ToggleableState.Off
                            displayedIds.size -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TriStateCheckbox(
                            state = toggleState,
                            onClick = { onEvent(SampleUiEvent.ToggleSelectAll) },
                        )
                    }
                }
            }
        }

        column(PersonColumn.EXPAND, valueOf = { it.expandedMovement }) {
            val iconButtonSize = if (useCompactMode) 36.dp else 48.dp
            title { "Movements" }
            width(iconButtonSize, iconButtonSize)
            resizable(false)
            cell { item, _ ->
                // Use smaller IconButton in compact mode to allow shorter rows
                IconButton(
                    onClick = { onToggleMovementExpanded(item.id) },
                    modifier = Modifier.size(iconButtonSize),
                ) {
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
            cell { item, _ -> Text(item.name, modifier = Modifier.padding(cellPadding)) }

            // Editing configuration - table will manage when to show this
            editCell { person, tableData, onComplete ->
                var text by remember(person) { mutableStateOf(person.name) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it
                        onEvent(SampleUiEvent.UpdateName(it))
                    },
                    errorMessage = tableData.editState.nameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(
                            onDone = { onComplete() },
                        ),
                )
            }

            footer { tableData ->
                Text(
                    "Total: ${tableData.displayedPeople.size}",
                    modifier = Modifier.padding(cellPadding),
                    fontWeight = FontWeight.Bold,
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
            cell { item, _ ->
                Text(
                    item.age.toString(),
                    modifier = Modifier.padding(cellPadding),
                )
            }

            // Editing configuration
            editCell { person, tableData, onComplete ->
                var text by remember(person) { mutableStateOf(person.age.toString()) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it.filter { char -> char.isDigit() }
                        it.toIntOrNull()?.let { age ->
                            onEvent(SampleUiEvent.UpdateAge(age))
                        }
                    },
                    errorMessage = tableData.editState.ageError,
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = { onComplete() },
                        ),
                )
            }

            footer { tableData ->
                val avgAge =
                    if (tableData.displayedPeople.isNotEmpty()) {
                        tableData.displayedPeople.map { it.age }.average()
                    } else {
                        0.0
                    }
                val rounded = (avgAge * 10).toInt() / 10.0
                Text(
                    "Avg: $rounded",
                    modifier = Modifier.padding(cellPadding),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        column(PersonColumn.ACTIVE, { it.active }) {
            title { "Active" }
            autoWidth()
            sortable()
            filter(TableFilterType.BooleanTableFilter())
            cell { item, _ ->
                Text(
                    if (item.active) "Yes" else "No",
                    modifier = Modifier.padding(cellPadding),
                )
            }
        }
        column(PersonColumn.ID, valueOf = { it.id }) {
            title { "ID" }
            autoWidth()
            sortable()
            align(Alignment.CenterEnd)
            cell { item, _ ->
                Text(
                    item.id.toString(),
                    modifier = Modifier.padding(cellPadding),
                )
            }
        }
        column(PersonColumn.EMAIL, valueOf = { it.email }) {
            title { "Email" }
            autoWidth()
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item, _ -> Text(item.email, modifier = Modifier.padding(cellPadding)) }
        }
        column(PersonColumn.CITY, { it.city }) {
            title { "City" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item, _ -> Text(item.city, modifier = Modifier.padding(cellPadding)) }
        }
        column(PersonColumn.COUNTRY, { it.country }) {
            title { "Country" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item, _ -> Text(item.country, modifier = Modifier.padding(cellPadding)) }
        }
        column(PersonColumn.DEPARTMENT, { it.department }) {
            title { "Department" }
            autoWidth(500.dp)
            sortable()
            filter(TableFilterType.TextTableFilter())
            cell { item, _ ->
                Text(item.department, modifier = Modifier.padding(cellPadding))
            }
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
            cell { item, _ ->
                Text(item.position.displayName, modifier = Modifier.padding(cellPadding))
            }

            // Editing configuration with dropdown
            editCell { person, tableData, onComplete ->
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier =
                            Modifier.menuAnchor(
                                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            ),
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
            // Custom visual range filter with histogram - uses tableData internally
            filter(createSalaryRangeFilter())
            align(Alignment.CenterEnd)
            cell { item, _ ->
                Text(
                    "$${item.salary}",
                    modifier = Modifier.padding(cellPadding),
                )
            }

            // Editing configuration
            editCell { person, tableData, onComplete ->
                var text by remember(person) { mutableStateOf(person.salary.toString()) }

                TableCellTextFieldWithTooltipError(
                    value = text,
                    onValueChange = {
                        text = it.filter { char -> char.isDigit() }
                        it.toIntOrNull()?.let { salary ->
                            onEvent(SampleUiEvent.UpdateSalary(salary))
                        }
                    },
                    errorMessage = tableData.editState.salaryError,
                    singleLine = true,
                    prefix = { Text("$") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = { onComplete() },
                        ),
                )
            }

            footer { tableData ->
                val totalSalary = tableData.displayedPeople.sumOf { it.salary }
                Text(
                    "Total: $$totalSalary",
                    modifier = Modifier.padding(cellPadding),
                    fontWeight = FontWeight.Bold,
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
            cell { item, _ ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(cellPadding),
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
            cell { item, _ ->
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
                    modifier = Modifier.padding(cellPadding),
                )
            }
        }
        // Multiline text field to demonstrate dynamic row height
        column(PersonColumn.NOTES, { it.notes }) {
            title { "Notes" }
            // Let the row grow by content; optionally set bounds in dynamic mode
            // Use smaller min height in compact mode
            rowHeight(
                min = if (useCompactMode) 36.dp else 48.dp,
                max = 200.dp,
            )
            autoWidth(500.dp)
            filter(TableFilterType.TextTableFilter())
            cell { item, _ -> Text(item.notes, modifier = Modifier.padding(cellPadding)) }
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
            cell { item, _ ->
                Text(ageGroup(item), modifier = Modifier.padding(cellPadding))
            }
        }
    }

fun createMovementColumns(useCompactMode: Boolean = false): ImmutableList<ColumnSpec<PersonMovement, PersonMovementColumn, Person>> =
    tableColumns {
        val cellPadding = if (useCompactMode) CellPadding.compact else CellPadding.standard
        column(PersonMovementColumn.DATE, valueOf = { it.date }) {
            title { "Date" }
            autoWidth()
            cell { movement, _ ->
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
                    modifier = Modifier.padding(cellPadding),
                    fontFamily = FontFamily.Monospace,
                )
            }

            footer { person ->
                Text(
                    "Total: ${person.movements.size}",
                    modifier = Modifier.padding(cellPadding),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        column(PersonMovementColumn.FROM_POSITION, valueOf = { it.fromPosition }) {
            title { "From" }
            autoWidth()
            cell { movement, _ ->
                Text(
                    movement.fromPosition?.displayName ?: "-",
                    modifier = Modifier.padding(cellPadding),
                )
            }
        }

        column(PersonMovementColumn.TO_POSITION, valueOf = { it.toPosition }) {
            title { "To" }
            autoWidth()
            cell { movement, _ ->
                Text(
                    movement.toPosition.displayName,
                    modifier = Modifier.padding(cellPadding),
                )
            }
        }
    }
