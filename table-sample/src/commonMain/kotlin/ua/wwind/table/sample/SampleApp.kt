package ua.wwind.table.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatDialog
import ua.wwind.table.format.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun SampleApp(modifier: Modifier = Modifier) {
    var isDarkTheme by remember { mutableStateOf(false) }
    // Create or remember ViewModel
    val viewModel = remember { SampleViewModel() }

    // Handle cleanup when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clear()
        }
    }

    val settings = remember {
        TableSettings(
            isDragEnabled = false,
            autoApplyFilters = true,
            autoFilterDebounce = 200,
            stripedRows = true,
            showActiveFiltersHeader = true,
            selectionMode = SelectionMode.None,
            rowHeightMode = RowHeightMode.Dynamic,
        )
    }

    val state =
        rememberTableState(
            columns = PersonColumn.entries,
            settings = settings,
            dimensions = TableDimensions(defaultColumnWidth = 100.dp),
        )

    // Build customization based on rules + matching logic
    val customization: TableCustomization<Person, PersonColumn> =
        rememberCustomization(
            rules = viewModel.rules,
            key = viewModel.rules, // recompose on rules change
            matches = { person, ruleFilters -> viewModel.matchesPerson(person, ruleFilters) },
        )

    // Apply active header filters to the dataset shown in the table
    val filteredPeople: List<Person> = viewModel.people.filter { person ->
        viewModel.matchesPerson(person, state.filters)
    }

    // Apply sorting based on current table state
    val sortedPeople: List<Person> = remember(filteredPeople, state.sort) {
        val sortState = state.sort
        if (sortState == null) {
            filteredPeople
        } else {
            val base =
                when (sortState.column) {
                    PersonColumn.NAME -> filteredPeople.sortedBy { it.name.lowercase() }
                    PersonColumn.AGE -> filteredPeople.sortedBy { it.age }
                    PersonColumn.ACTIVE -> filteredPeople.sortedBy { it.active }
                    PersonColumn.ID -> filteredPeople.sortedBy { it.id }
                    PersonColumn.EMAIL -> filteredPeople.sortedBy { it.email.lowercase() }
                    PersonColumn.CITY -> filteredPeople.sortedBy { it.city.lowercase() }
                    PersonColumn.COUNTRY -> filteredPeople.sortedBy { it.country.lowercase() }
                    PersonColumn.DEPARTMENT -> filteredPeople.sortedBy { it.department.lowercase() }
                    PersonColumn.SALARY -> filteredPeople.sortedBy { it.salary }
                    PersonColumn.RATING -> filteredPeople.sortedBy { it.rating }
                    PersonColumn.NOTES -> filteredPeople.sortedBy { it.notes.lowercase() }
                    PersonColumn.AGE_GROUP ->
                        filteredPeople.sortedBy {
                            when {
                                it.age < 25 -> 0
                                it.age < 35 -> 1
                                else -> 2
                            }
                        }
                }
            if (sortState.order == SortOrder.DESCENDING) base.asReversed() else base
        }
    }

    SampleTheme(darkTheme = isDarkTheme) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                // Toolbar with theme toggle and conditional formatting button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { viewModel.toggleFormatDialog(true) }) {
                        Text("Conditional formatting")
                    }

                    // Spacer is not necessary; we use Arrangement.spacedBy
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Dark theme")
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it },
                        )
                    }
                }
                HorizontalDivider()

                // The table
                Table(
                    itemsCount = sortedPeople.size,
                    itemAt = { index -> sortedPeople.getOrNull(index) },
                    state = state,
                    columns = viewModel.columns,
                    customization = customization,
                    strings = DefaultStrings,
                    rowKey = { item, _ -> item?.id ?: 0 },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Formatting dialog
        FormatDialog(
            showDialog = viewModel.showFormatDialog,
            rules = viewModel.rules,
            onRulesChanged = { viewModel.updateRules(it) },
            getNewRule = { id ->
                TableFormatRule.new<PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
                    id,
                    emptyMap(),
                )
            },
            getTitle = { field ->
                when (field) {
                    PersonColumn.NAME -> "Name"
                    PersonColumn.AGE -> "Age"
                    PersonColumn.ACTIVE -> "Active"
                    PersonColumn.ID -> "ID"
                    PersonColumn.EMAIL -> "Email"
                    PersonColumn.CITY -> "City"
                    PersonColumn.COUNTRY -> "Country"
                    PersonColumn.DEPARTMENT -> "Department"
                    PersonColumn.SALARY -> "Salary"
                    PersonColumn.RATING -> "Rating"
                    PersonColumn.NOTES -> "Notes"
                    PersonColumn.AGE_GROUP -> "Age group"
                }
            },
            filters = viewModel::buildFormatFilterData,
            entries = PersonColumn.entries,
            key = Unit,
            strings = DefaultStrings,
            onDismissRequest = { viewModel.toggleFormatDialog(false) },
            settings = FormatDialogSettings(),
        )
    }
}
