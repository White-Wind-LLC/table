package ua.wwind.table.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.FixedSide
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
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
    val viewModel: SampleViewModel = viewModel { SampleViewModel() }

    var useStripedRows by remember { mutableStateOf(true) }

    var showFastFilters by remember { mutableStateOf(true) }

    var enableDragToScroll by remember { mutableStateOf(true) }

    var fixedColumnsCount by remember { mutableStateOf(0) }
    var fixedColumnsSide by remember { mutableStateOf(FixedSide.Left) }

    var enableEditing by remember { mutableStateOf(false) }

    val settings =
        remember(
            useStripedRows,
            showFastFilters,
            enableDragToScroll,
            fixedColumnsCount,
            fixedColumnsSide,
            enableEditing,
        ) {
            TableSettings(
                isDragEnabled = false,
                autoApplyFilters = true,
                showFastFilters = showFastFilters,
                autoFilterDebounce = 200,
                stripedRows = useStripedRows,
                showActiveFiltersHeader = true,
                selectionMode = SelectionMode.None,
                rowHeightMode = RowHeightMode.Dynamic,
                enableDragToScroll = enableDragToScroll,
                fixedColumnsCount = fixedColumnsCount,
                fixedColumnsSide = fixedColumnsSide,
                editingEnabled = enableEditing,
            )
        }

    val state =
        rememberTableState(
            columns = PersonColumn.entries.toImmutableList(),
            settings = settings,
            dimensions = TableDimensions(defaultColumnWidth = 100.dp),
        )

    val columns =
        remember {
            createTableColumns(
                onToggleMovementExpanded = viewModel::toggleMovementExpanded,
                onEvent = viewModel::onEvent,
            )
        }

    // Observe filters and sort state changes and update ViewModel
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }
            .collect { filters ->
                viewModel.updateFilters(filters)
            }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.sort }
            .collect { sort ->
                viewModel.updateSort(sort)
            }
    }

    // Build customization based on rules + matching logic
    val customization: TableCustomization<Person, PersonColumn> =
        rememberCustomization(
            rules = viewModel.rules,
            key = viewModel.rules, // recompose on rules change
            matches = { person, ruleFilters -> viewModel.matchesPerson(person, ruleFilters) },
        )

    // Get filtered and sorted people from ViewModel StateFlow
    val displayedPeople by viewModel.displayedPeople.collectAsState()

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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState()),
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
                        Text("Fast filters")
                        Switch(
                            checked = showFastFilters,
                            onCheckedChange = { showFastFilters = it },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Drag to scroll")
                        Switch(
                            checked = enableDragToScroll,
                            onCheckedChange = { enableDragToScroll = it },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Fixed cols:")
                        OutlinedButton(onClick = { if (fixedColumnsCount > 0) fixedColumnsCount-- }) {
                            Text("-")
                        }
                        Text("$fixedColumnsCount")
                        OutlinedButton(onClick = { fixedColumnsCount++ }) {
                            Text("+")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Fixed Side: $fixedColumnsSide")
                        Switch(
                            checked = fixedColumnsSide == FixedSide.Right,
                            onCheckedChange = { fixedColumnsSide = if (it) FixedSide.Right else FixedSide.Left },
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Cell editing")
                        Switch(
                            checked = enableEditing,
                            onCheckedChange = { enableEditing = it },
                        )
                    }
                }
                HorizontalDivider()

                // The table
                EditableTable(
                    itemsCount = displayedPeople.size,
                    itemAt = { index -> displayedPeople.getOrNull(index) },
                    state = state,
                    editState = viewModel.editingRowState,
                    columns = columns,
                    customization = customization,
                    strings = DefaultStrings,
                    rowKey = { item, index -> index },
                    rowEmbedded = { _, person ->
                        val visible = person.expandedMovement
                        AnimatedVisibility(
                            visible = visible,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column {
                                HorizontalDivider(
                                    thickness = state.dimensions.dividerThickness,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                PersonMovementsSection(person = person)
                            }
                        }
                    },
                    onRowEditStart = { person, rowIndex ->
                        // Start editing - initializes editing state in ViewModel
                        viewModel.onEvent(SampleUiEvent.StartEditing(rowIndex, person))
                    },
                    onRowEditComplete = { rowIndex ->
                        // Validate using ViewModel method
                        if (viewModel.validateEditedPerson()) {
                            // Validation passed - complete editing
                            viewModel.onEvent(SampleUiEvent.CompleteEditing)
                            true // Allow exit from edit mode
                        } else {
                            // Validation failed - stay in edit mode with errors shown
                            false // Block exit from edit mode
                        }
                    },
                    onEditCancelled = { rowIndex ->
                        // Cancel editing - discards changes
                        viewModel.onEvent(SampleUiEvent.CancelEditing)
                    },
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
                    PersonColumn.POSITION -> "Position"
                    PersonColumn.SALARY -> "Salary"
                    PersonColumn.RATING -> "Rating"
                    PersonColumn.HIRE_DATE -> "Hire Date"
                    PersonColumn.NOTES -> "Notes"
                    PersonColumn.AGE_GROUP -> "Age group"
                    PersonColumn.EXPAND -> "Movements"
                }
            },
            filters = viewModel::buildFormatFilterData,
            entries = PersonColumn.entries.toImmutableList(),
            key = Unit,
            strings = DefaultStrings,
            onDismissRequest = { viewModel.toggleFormatDialog(false) },
            settings = FormatDialogSettings(),
        )
    }
}

@OptIn(ExperimentalTableApi::class)
@Composable
private fun PersonMovementsSection(person: Person) {
    val columns = remember { createMovementColumns() }
    val movementSettings =
        remember {
            TableSettings(
                isDragEnabled = false,
                autoApplyFilters = false,
                showFastFilters = false,
                autoFilterDebounce = 0,
                stripedRows = false,
                showActiveFiltersHeader = false,
                selectionMode = SelectionMode.None,
                rowHeightMode = RowHeightMode.Dynamic,
                enableDragToScroll = false,
            )
        }
    val movementState =
        rememberTableState(columns = PersonMovementColumn.entries.toImmutableList(), settings = movementSettings)

    Column(
        modifier =
            Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "HR movements",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Table(
            itemsCount = person.movements.size,
            itemAt = { index -> person.movements.getOrNull(index) },
            state = movementState,
            columns = columns,
            strings = DefaultStrings,
            rowKey = { item, index -> item?.date ?: index },
            modifier = Modifier.padding(top = 8.dp),
            embedded = true,
        )
    }
}
