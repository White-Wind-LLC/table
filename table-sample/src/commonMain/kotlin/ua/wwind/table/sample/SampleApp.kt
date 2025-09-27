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
import ua.wwind.table.ColumnSpec
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
import ua.wwind.table.lazy.LazyTable
import ua.wwind.table.lazy.rememberViewportState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.tableColumns

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

    var useStripedRows by remember { mutableStateOf(true) }
    var useLazyTable by remember { mutableStateOf(false) }
    var useBigData by remember { mutableStateOf(false) }

    val settings =
        remember(useStripedRows) {
            TableSettings(
                isDragEnabled = false,
                autoApplyFilters = true,
                autoFilterDebounce = 200,
                stripedRows = useStripedRows,
                showActiveFiltersHeader = true,
                selectionMode = SelectionMode.None,
                rowHeightMode = RowHeightMode.Dynamic,
            )
        }

    val sampleDimensions = remember { TableDimensions(defaultColumnWidth = 100.dp) }
    val state =
        rememberTableState(
            columns = PersonColumn.entries,
            settings = settings,
            dimensions = sampleDimensions,
        )

    // Build customization based on rules + matching logic
    val customization: TableCustomization<Person, PersonColumn> =
        rememberCustomization(
            rules = viewModel.rules,
            key = viewModel.rules, // recompose on rules change
            matches = { person, ruleFilters -> viewModel.matchesPerson(person, ruleFilters) },
        )

    // Apply active header filters to the dataset shown in the table
    val filteredPeople: List<Person> =
        viewModel.people.filter { person ->
            viewModel.matchesPerson(person, state.filters)
        }

    // Apply sorting based on current table state
    val sortedPeople: List<Person> =
        remember(filteredPeople, state.sort) {
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Stripped rows")
                        Switch(
                            checked = useStripedRows,
                            onCheckedChange = { useStripedRows = it },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("LazyTable (experimental)")
                        Switch(
                            checked = useLazyTable,
                            onCheckedChange = { useLazyTable = it },
                        )
                    }

                    if (useLazyTable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("1,000 x 1,000 data")
                            Switch(
                                checked = useBigData,
                                onCheckedChange = { useBigData = it },
                            )
                        }
                    }
                }
                HorizontalDivider()

                // The table
                if (!useLazyTable) {
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
                } else {
                    val viewportState = rememberViewportState(overscanPx = 300)
                    if (!useBigData) {
                        LazyTable(
                            itemsCount = sortedPeople.size,
                            itemAt = { index -> sortedPeople.getOrNull(index) },
                            columns = viewModel.columns,
                            rowHeight = 32.dp,
                            defaultColumnWidth = sampleDimensions.defaultColumnWidth,
                            state = viewportState,
                            stickyHeader = true,
                            stickyFirstColumn = false,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        val bigCols: List<ColumnSpec<Int, Int>> = remember {
                            tableColumns<Int, Int> {
                                for (c in 0 until 10000) {
                                    column(key = c, valueOf = { it }) {
                                        header(c.toString())
                                        cell { rowIndex ->
                                            Text(((rowIndex * 10000) + c).toString())
                                        }
                                        width(80.dp, 80.dp)
                                        resizable(false)
                                    }
                                }
                            }
                        }
                        LazyTable(
                            itemsCount = 1000,
                            itemAt = { index -> index },
                            columns = bigCols,
                            rowHeight = 28.dp,
                            defaultColumnWidth = 80.dp,
                            state = viewportState,
                            stickyHeader = true,
                            stickyFirstColumn = false,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
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

private fun headerTitle(col: PersonColumn): String =
    when (col) {
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

private fun cellText(p: Person, col: PersonColumn): String =
    when (col) {
        PersonColumn.NAME -> p.name
        PersonColumn.AGE -> p.age.toString()
        PersonColumn.ACTIVE -> if (p.active) "Yes" else "No"
        PersonColumn.ID -> p.id.toString()
        PersonColumn.EMAIL -> p.email
        PersonColumn.CITY -> p.city
        PersonColumn.COUNTRY -> p.country
        PersonColumn.DEPARTMENT -> p.department
        PersonColumn.SALARY -> p.salary.toString()
        PersonColumn.RATING -> p.rating.toString()
        PersonColumn.NOTES -> p.notes
        PersonColumn.AGE_GROUP -> when {
            p.age < 25 -> "<25"
            p.age < 35 -> "25-34"
            else -> "35+"
        }
    }
