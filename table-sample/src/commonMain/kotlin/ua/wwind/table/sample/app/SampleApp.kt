package ua.wwind.table.sample.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.FixedSide
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.sample.app.components.AppToolbar
import ua.wwind.table.sample.app.components.ConditionalFormattingDialog
import ua.wwind.table.sample.app.components.MainTable
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.column.createTableColumns
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.viewmodel.SampleUiEvent
import ua.wwind.table.sample.viewmodel.SampleViewModel
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
fun SampleApp(modifier: Modifier = Modifier) {
    var isDarkTheme by remember { mutableStateOf(false) }
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

    // Collect state from ViewModel
    val people by viewModel.people.collectAsState()
    val displayedPeople by viewModel.displayedPeople.collectAsState()

    // Create columns with callbacks
    val columns =
        remember(people) {
            createTableColumns(
                onToggleMovementExpanded = viewModel::toggleMovementExpanded,
                allPeople = people,
                onEvent = viewModel::onEvent,
            )
        }

    // Build customization based on rules + matching logic
    val customization =
        rememberCustomization<Person, PersonColumn, Map<PersonColumn, TableFilterState<*>>>(
            rules = viewModel.rules,
            key = viewModel.rules,
            matches = { person, ruleFilters ->
                viewModel.matchesPerson(person, ruleFilters)
            },
        )

    val state =
        rememberTableState(
            columns = PersonColumn.entries.toImmutableList(),
            settings = settings,
            dimensions = TableDimensions(defaultColumnWidth = 100.dp),
        )

    SampleTheme(darkTheme = isDarkTheme) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                AppToolbar(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = { isDarkTheme = it },
                    useStripedRows = useStripedRows,
                    onStripedRowsChange = { useStripedRows = it },
                    showFastFilters = showFastFilters,
                    onShowFastFiltersChange = { showFastFilters = it },
                    enableDragToScroll = enableDragToScroll,
                    onEnableDragToScrollChange = { enableDragToScroll = it },
                    fixedColumnsCount = fixedColumnsCount,
                    onFixedColumnsCountChange = { fixedColumnsCount = it },
                    fixedColumnsSide = fixedColumnsSide,
                    onFixedColumnsSideChange = { fixedColumnsSide = it },
                    enableEditing = enableEditing,
                    onEnableEditingChange = { enableEditing = it },
                    onConditionalFormattingClick = { viewModel.toggleFormatDialog(true) },
                )

                HorizontalDivider()

                MainTable(
                    displayedPeople = displayedPeople,
                    state = state,
                    editState = viewModel.editingRowState,
                    columns = columns,
                    customization = customization,
                    onFiltersChanged = viewModel::updateFilters,
                    onSortChanged = viewModel::updateSort,
                    onRowEditStart = { person, rowIndex ->
                        viewModel.onEvent(SampleUiEvent.StartEditing(rowIndex, person))
                    },
                    onRowEditComplete = { rowIndex ->
                        if (viewModel.validateEditedPerson()) {
                            viewModel.onEvent(SampleUiEvent.CompleteEditing)
                            true
                        } else {
                            false
                        }
                    },
                    onEditCancelled = { rowIndex ->
                        viewModel.onEvent(SampleUiEvent.CancelEditing)
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        ConditionalFormattingDialog(
            showDialog = viewModel.showFormatDialog,
            rules = viewModel.rules,
            onRulesChanged = viewModel::updateRules,
            buildFormatFilterData = viewModel::buildFormatFilterData,
            onDismissRequest = { viewModel.toggleFormatDialog(false) },
        )
    }
}
