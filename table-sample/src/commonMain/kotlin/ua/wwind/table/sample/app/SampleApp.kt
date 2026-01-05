package ua.wwind.table.sample.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.sample.app.components.AppToolbar
import ua.wwind.table.sample.app.components.ConditionalFormattingDialog
import ua.wwind.table.sample.app.components.MainTable
import ua.wwind.table.sample.app.components.SampleTableConfig
import ua.wwind.table.sample.app.components.SelectionActionBar
import ua.wwind.table.sample.app.components.SettingsSidebar
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

    var tableConfig by remember { mutableStateOf(SampleTableConfig()) }

    val settings =
        remember(tableConfig) {
            TableSettings(
                isDragEnabled = false,
                autoApplyFilters = true,
                showFastFilters = tableConfig.showFastFilters,
                autoFilterDebounce = 200,
                stripedRows = tableConfig.useStripedRows,
                showActiveFiltersHeader = true,
                selectionMode = SelectionMode.None,
                rowHeightMode = RowHeightMode.Dynamic,
                enableDragToScroll = tableConfig.enableDragToScroll,
                pinnedColumnsCount = tableConfig.pinnedColumnsCount,
                pinnedColumnsSide = tableConfig.pinnedColumnsSide,
                editingEnabled = tableConfig.enableEditing,
                showFooter = tableConfig.showFooter,
                footerPinned = tableConfig.footerPinned,
                enableTextSelection = true,
                showVerticalDividers = tableConfig.showVerticalDividers,
                showRowDividers = tableConfig.showRowDividers,
                showHeaderDivider = tableConfig.showHeaderDivider,
                showFastFiltersDivider = tableConfig.showFastFiltersDivider,
            )
        }

    // Collect state from ViewModel
    val tableData by viewModel.tableData.collectAsState()

    // Create columns with callbacks
    val columns =
        remember(tableConfig.useCompactMode) {
            createTableColumns(
                onToggleMovementExpanded = viewModel::toggleMovementExpanded,
                onEvent = viewModel::onEvent,
                useCompactMode = tableConfig.useCompactMode,
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
            dimensions =
                remember(tableConfig.useCompactMode) {
                    if (tableConfig.useCompactMode) {
                        TableDefaults.compactDimensions().copy(defaultColumnWidth = 100.dp)
                    } else {
                        TableDefaults.standardDimensions().copy(defaultColumnWidth = 200.dp)
                    }
                },
        )

    // Drawer state for settings sidebar
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Liquid glass effect state for SelectionActionBar
    val liquidState = rememberLiquidState()

    // Toggle visibility of SELECTION column by adjusting width based on selection mode
    val selectionColumnWidth =
        if (tableData.selectionModeEnabled) {
            if (tableConfig.useCompactMode) 36.dp else 48.dp
        } else {
            0.dp
        }
    LaunchedEffect(selectionColumnWidth) {
        state.setColumnWidths(mapOf(PersonColumn.SELECTION to selectionColumnWidth))
    }

    SampleTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        ModalDrawerSheet(
                            drawerShape =
                                MaterialTheme.shapes.large.copy(
                                    topStart = CornerSize(16.dp),
                                    bottomStart = CornerSize(16.dp),
                                    topEnd = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp),
                                ),
                        ) {
                            SettingsSidebar(
                                isDarkTheme = isDarkTheme,
                                onDarkThemeChange = { isDarkTheme = it },
                                config = tableConfig,
                                onConfigChange = { tableConfig = it },
                                enableSelectionMode = tableData.selectionModeEnabled,
                                onEnableSelectionModeChange = { viewModel.setSelectionMode(it) },
                                onConditionalFormattingClick = {
                                    viewModel.toggleFormatDialog(true)
                                    scope.launch { drawerState.close() }
                                },
                                onRecalculateAutoWidthsClick = {
                                    state.recalculateAutoWidths()
                                    scope.launch { drawerState.close() }
                                },
                                onClose = { scope.launch { drawerState.close() } },
                            )
                        }
                    }
                },
                gesturesEnabled = true,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.safeDrawing),
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                AppToolbar(
                                    onSettingsClick = { scope.launch { drawerState.open() } },
                                )

                                HorizontalDivider()

                                MainTable(
                                    state = state,
                                    tableData = tableData,
                                    columns = columns,
                                    customization = customization,
                                    onFiltersChanged = viewModel::updateFilters,
                                    onSortChanged = viewModel::updateSort,
                                    onRowEditStart = { person, rowIndex ->
                                        viewModel.onEvent(
                                            SampleUiEvent.StartEditing(rowIndex, person),
                                        )
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
                                    useCompactMode = tableConfig.useCompactMode,
                                    modifier = Modifier.padding(16.dp)
                                        .liquefiable(liquidState),
                                )
                            }

                            // Floating selection action bar at the bottom with Liquid Glass effect
                            SelectionActionBar(
                                selectedCount = tableData.selectedIds.size,
                                onDeleteClick = {
                                    viewModel.onEvent(SampleUiEvent.DeleteSelected)
                                },
                                onClearSelection = {
                                    viewModel.onEvent(SampleUiEvent.ClearSelection)
                                },
                                liquidState = liquidState,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                            )
                        }
                    }
                }
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
