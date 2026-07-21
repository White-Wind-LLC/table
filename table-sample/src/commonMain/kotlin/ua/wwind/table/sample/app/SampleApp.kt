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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
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
import ua.wwind.table.RowBlocks
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.draggableHandle
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.rememberCustomization
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

/**
 * The demo shell. Its complexity is compact-mode dimension picks and optional slots — which
 * sidebar, dialog and table features the toggles currently enable — so `CyclomaticComplexMethod`
 * is suppressed rather than fixed.
 */
@Suppress("CyclomaticComplexMethod")
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
    val sortWithinBlocks by viewModel.sortWithinBlocks.collectAsState()

    // Create columns with callbacks
    val columns =
        remember(
            tableConfig.useCompactMode,
            tableConfig.enableRowReorder,
            tableConfig.hiddenColumns,
        ) {
            createTableColumns(
                onToggleMovementExpanded = viewModel::toggleMovementExpanded,
                onEvent = viewModel::onEvent,
                useCompactMode = tableConfig.useCompactMode,
                enableRowReorder = tableConfig.enableRowReorder,
                hiddenColumns = tableConfig.hiddenColumns,
            )
        }

    // Mirrors the row-handle column in createTableColumns.
    val handleColumnWidth = if (tableConfig.useCompactMode) 36.dp else 48.dp

    // Declared by identity, so nothing here depends on the displayed list: the table derives block
    // extents itself from the snapshot it renders. One instance per toggle flip — RowBlocks
    // compares by identity, and a fresh instance each recomposition would defeat skipping.
    val rowBlocks =
        remember(tableConfig.enableRowBlocks, handleColumnWidth) {
            if (!tableConfig.enableRowBlocks) {
                null
            } else {
                RowBlocks<Person>(
                    blockOf = { it.groupId },
                    // Exactly one event per completed gesture; the lift to the master list is one
                    // applyRowBlockMove call in the ViewModel.
                    onCommit = { move -> viewModel.onEvent(SampleUiEvent.BlockMove(move)) },
                    // Reordering a row within its block: one applyRowReorderWithinBlock call.
                    onRowReorderWithinBlock = { move ->
                        viewModel.onEvent(SampleUiEvent.RowWithinBlockMove(move))
                    },
                    // The band above the block: a drag handle that moves the whole block, the group
                    // name (tap to rename), and an edit affordance.
                    blockHeader = { blockId, _ ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            // Same width as the row-handle column, so every drag handle in the table
                            // lines up in one vertical run.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.width(handleColumnWidth),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Drag group $blockId",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp).draggableHandle(),
                                )
                            }
                            Text(
                                text = blockId.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                // clickable inside the label so the handle drag and the rename tap
                                // do not fight over the same node.
                                modifier =
                                    Modifier.clickable {
                                        viewModel.setRenamingGroup(blockId.toString())
                                    },
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename group $blockId",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
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
                                sortWithinBlocks = sortWithinBlocks,
                                onSortWithinBlocksChange = { viewModel.setSortWithinBlocks(it) },
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
                                    // Non-null blocks take over the drag inside the table, so the
                                    // plain single-row swap demo (onRowMove) still applies only
                                    // while blocks are off.
                                    rowBlocks = rowBlocks,
                                    onMovementBlockMove = { person, move ->
                                        viewModel.onEvent(
                                            SampleUiEvent.MovementBlockMove(
                                                personId = person.id,
                                                move = move,
                                            ),
                                        )
                                    },
                                    onMovementRowWithinBlockMove = { person, move ->
                                        viewModel.onEvent(
                                            SampleUiEvent.MovementRowWithinBlockMove(
                                                personId = person.id,
                                                move = move,
                                            ),
                                        )
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
                                // Gated on the blocks toggle: with blocks off `rowBlocks` is null,
                                // so grouping would change nothing visible and look broken.
                                canGroup =
                                    tableConfig.enableRowBlocks &&
                                        tableData.selectedIds.size >= 2,
                                onGroupClick = {
                                    viewModel.onEvent(SampleUiEvent.GroupSelected)
                                },
                                canUngroup =
                                    tableConfig.enableRowBlocks &&
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
