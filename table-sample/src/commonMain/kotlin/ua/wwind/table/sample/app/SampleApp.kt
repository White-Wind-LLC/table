package ua.wwind.table.sample.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import ua.wwind.table.TableRowGroups
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.rememberCustomization
import ua.wwind.table.rowGroupsOf
import ua.wwind.table.sample.app.components.AppToolbar
import ua.wwind.table.sample.app.components.ConditionalFormattingDialog
import ua.wwind.table.sample.app.components.GroupRenameDialog
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
                rowReorderEnabled = tableConfig.enableRowReorder,
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
        remember(
            tableConfig.useCompactMode,
            tableConfig.enableRowReorder,
            tableConfig.enableRowGroups,
        ) {
            createTableColumns(
                onToggleMovementExpanded = viewModel::toggleMovementExpanded,
                onEvent = viewModel::onEvent,
                useCompactMode = tableConfig.useCompactMode,
                enableRowReorder = tableConfig.enableRowReorder,
                enableRowGroups = tableConfig.enableRowGroups,
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

    // The SELECTION column carries the checkbox in selection mode and the drag handle outside it,
    // so it must stay open for either one; collapsing it on selection mode alone took the handle
    // with it. Width is the only visibility control the column has.
    val selectionColumnWidth =
        if (tableData.selectionModeEnabled || tableConfig.enableRowReorder) {
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
                                    onRowMove = { from, to ->
                                        val displayedPeople = tableData.displayedPeople
                                        val source = displayedPeople.getOrNull(from) ?: return@MainTable
                                        val targetIndex =
                                            if (to >= displayedPeople.size) {
                                                displayedPeople.lastIndex
                                            } else {
                                                to
                                            }
                                        val target = displayedPeople.getOrNull(targetIndex) ?: return@MainTable
                                        if (source.id != target.id) {
                                            viewModel.onEvent(
                                                SampleUiEvent.RowMove(
                                                    fromPersonId = source.id,
                                                    toPersonId = target.id,
                                                ),
                                            )
                                        }
                                    },
                                    // Built only while groups are on: a non-null onMove takes
                                    // precedence over onRowMove inside the table, which would turn
                                    // the plain single-row swap demo into a remove-and-insert move.
                                    rowGroups =
                                        remember(
                                            tableData.displayedPeople,
                                            tableConfig.enableRowGroups,
                                        ) {
                                            if (!tableConfig.enableRowGroups) {
                                                null
                                            } else {
                                                TableRowGroups(
                                                    ranges =
                                                        tableData.displayedPeople.rowGroupsOf {
                                                            it.groupId
                                                        },
                                                    onMove = { from, to ->
                                                        val people = tableData.displayedPeople
                                                        val movedIds =
                                                            from.mapNotNull { people.getOrNull(it)?.id }
                                                        val targetIds =
                                                            to.mapNotNull { people.getOrNull(it)?.id }
                                                        if (movedIds.isNotEmpty() && targetIds.isNotEmpty()) {
                                                            viewModel.onEvent(
                                                                SampleUiEvent.RowsMove(
                                                                    fromPersonIds = movedIds,
                                                                    toPersonIds = targetIds,
                                                                ),
                                                            )
                                                        }
                                                    },
                                                    // Names the block in the band above it — where the
                                                    // group chip used to sit inside the Name cell.
                                                    // The id IS the name, so tapping it edits the id.
                                                    header = { rows ->
                                                        val groupId =
                                                            tableData.displayedPeople
                                                                .getOrNull(rows.first)
                                                                ?.groupId
                                                        if (groupId != null) {
                                                            Row(
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                    Arrangement.spacedBy(6.dp),
                                                                // clickable before padding: the
                                                                // padding is inside the touch target,
                                                                // not dead space around it.
                                                                modifier =
                                                                    Modifier
                                                                        .clickable {
                                                                            viewModel
                                                                                .setRenamingGroup(groupId)
                                                                        }.padding(
                                                                            horizontal = 12.dp,
                                                                            vertical = 6.dp,
                                                                        ),
                                                            ) {
                                                                Text(
                                                                    text = groupId,
                                                                    style =
                                                                        MaterialTheme.typography
                                                                            .labelLarge,
                                                                    color =
                                                                        MaterialTheme.colorScheme
                                                                            .onSurface,
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription =
                                                                        "Rename group $groupId",
                                                                    tint =
                                                                        MaterialTheme.colorScheme
                                                                            .onSurfaceVariant,
                                                                    modifier = Modifier.size(16.dp),
                                                                )
                                                            }
                                                        }
                                                    },
                                                )
                                            }
                                        },
                                    onMovementRowMove = { person, from, to ->
                                        viewModel.onEvent(
                                            SampleUiEvent.MovementRowMove(
                                                personId = person.id,
                                                fromIndex = from,
                                                toIndex = to,
                                            ),
                                        )
                                    },
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
                                    enableRowReorder = tableConfig.enableRowReorder,
                                    modifier =
                                        Modifier
                                            .padding(16.dp)
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
                                // Gated on the groups toggle: with groups off `rowGroups` is null,
                                // so grouping would change nothing visible and look broken.
                                canGroup =
                                    tableConfig.enableRowGroups &&
                                        tableData.selectedIds.size >= 2,
                                onGroupClick = {
                                    viewModel.onEvent(SampleUiEvent.GroupSelected)
                                },
                                canUngroup =
                                    tableConfig.enableRowGroups &&
                                        tableData.displayedPeople.any {
                                            it.id in tableData.selectedIds && it.groupId != null
                                        },
                                onUngroupClick = {
                                    viewModel.onEvent(SampleUiEvent.UngroupSelected)
                                },
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

        // The master list, NOT `tableData.displayedPeople`: that one is filtered, so a name held by
        // a hidden row would look free and the rename would silently bail out in the ViewModel.
        // Group identity lives in the unfiltered list, which is exactly what RenameGroup rewrites.
        val allPeople by viewModel.people.collectAsState()
        val renamingGroupId = viewModel.renamingGroupId
        GroupRenameDialog(
            groupId = renamingGroupId,
            // Its own name is excluded, or the dialog would call the group's current id taken.
            takenGroupIds =
                remember(allPeople, renamingGroupId) {
                    allPeople.mapNotNull { it.groupId }.toSet() - setOfNotNull(renamingGroupId)
                },
            onRename = { newGroupId ->
                val groupId = renamingGroupId ?: return@GroupRenameDialog
                viewModel.onEvent(
                    SampleUiEvent.RenameGroup(groupId = groupId, newGroupId = newGroupId),
                )
            },
            onDismissRequest = { viewModel.setRenamingGroup(null) },
        )
    }
}
