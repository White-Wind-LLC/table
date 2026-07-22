package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.MeasureCellMinWidth
import ua.wwind.table.TableCellScopeImpl
import ua.wwind.table.TableItemScope
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.config.TableSettings
import ua.wwind.table.interaction.tableRowInteractions
import ua.wwind.table.state.PinnedColumnState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculatePinnedColumnState

/**
 * Composition local providing the current edit cell context (row index and column key).
 */
public val LocalEditCellContext: ProvidableCompositionLocal<EditCellContext?> =
    compositionLocalOf { null }

/**
 * Composition local providing a FocusRequester that should be used by edit cells
 * to automatically request focus when transitioning between editable cells.
 */
public val LocalEditCellFocusRequester: ProvidableCompositionLocal<FocusRequester?> =
    compositionLocalOf { null }

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
context(rowScope: TableItemScope)
internal fun <T : Any, C, E> TableRowItem(
    item: T?,
    index: Int,
    /** Whether [index] falls inside a row block; supplied by the caller from the same unit snapshot
     *  that produced [index], so it can never disagree with the rendered position. */
    isInRowBlock: Boolean,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    colors: ua.wwind.table.config.TableColors,
    customization: TableCustomization<T, C>,
    tableData: E,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    requestTableFocus: () -> Unit,
    horizontalState: ScrollState,
) {
    val dimensions = state.dimensions
    val isSelected = state.selection.selectedIndex == index
    val isDynamicRowHeight = state.settings.rowHeightMode == RowHeightMode.Dynamic
    val settings = state.settings

    // isInRowBlock is passed in from the caller (RowUnit), derived from the same unit snapshot as
    // [index], so it can never disagree with the rendered position.

    val defaultRowBackgroundColor =
        when {
            isSelected -> colors.rowSelectedContainerColor
            state.settings.stripedRows && (index % 2 != 0) -> colors.stripedRowContainerColor
            else -> colors.rowContainerColor
        }

    val rowStyle: TableRowStyle? =
        item?.let { nonNull ->
            val ctx =
                TableRowContext<T, C>(
                    item = nonNull,
                    index = index,
                    isSelected = isSelected,
                    isStriped = state.settings.stripedRows && (index % 2 != 0),
                    isInRowBlock = isInRowBlock,
                    isDeleted = false,
                )
            customization.resolveRowStyle(ctx)
        }

    val finalRowColor =
        rowStyle?.containerColor?.takeUnless { it == Unspecified } ?: defaultRowBackgroundColor
    val finalRowContentColor =
        rowStyle?.contentColor?.takeUnless { it == Unspecified }
            ?: contentColorFor(finalRowColor)

    val tonalElevation = rowStyle?.elevation?.takeUnless { it == Dp.Unspecified } ?: 0.dp

    Surface(
        color = finalRowColor,
        contentColor = finalRowContentColor,
        shape = rowStyle?.shape ?: RectangleShape,
        border = rowStyle?.border,
        tonalElevation = tonalElevation,
    ) {
        val minRowHeight: Dp? =
            if (isDynamicRowHeight) {
                visibleColumns.mapNotNull { it.minRowHeight }.maxOrNull()
            } else {
                null
            }
        val maxRowHeight: Dp? =
            if (isDynamicRowHeight) {
                visibleColumns.mapNotNull { it.maxRowHeight }.minOrNull()
            } else {
                null
            }

        var rowModifier = Modifier.width(state.tableWidth).then(rowStyle?.modifier ?: Modifier)
        if (isDynamicRowHeight) {
            rowModifier = rowModifier.height(IntrinsicSize.Min)
            if (minRowHeight != null || maxRowHeight != null) {
                rowModifier =
                    rowModifier.heightIn(
                        min = minRowHeight ?: Dp.Unspecified,
                        max = maxRowHeight ?: Dp.Unspecified,
                    )
            }
        }

        if (item != null) {
            Column {
                Column(modifier = Modifier.width(state.tableWidth)) {
                    RenderTableRowItem(
                        state = state,
                        index = index,
                        visibleColumns = visibleColumns,
                        dimensions = dimensions,
                        customization = customization,
                        item = item,
                        tableData = tableData,
                        isSelected = isSelected,
                        isInRowBlock = isInRowBlock,
                        settings = settings,
                        finalRowColor = finalRowColor,
                        isDynamicRowHeight = isDynamicRowHeight,
                        onRowClick = onRowClick,
                        onRowLongClick = onRowLongClick,
                        onContextMenu = onContextMenu,
                        requestTableFocus = requestTableFocus,
                        horizontalState = horizontalState,
                        modifier = rowModifier,
                    )
                }
                rowEmbedded?.invoke(index, item)
            }
        } else {
            Row(
                modifier =
                    if (isDynamicRowHeight) {
                        Modifier
                    } else {
                        Modifier.height(dimensions.rowHeight).width(state.tableWidth)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) { placeholderRow?.invoke() }
        }
    }
}

@Composable
context(rowScope: TableItemScope)
private fun <C, T : Any, E> RenderTableRowItem(
    state: TableState<C>,
    index: Int,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    dimensions: TableDimensions,
    customization: TableCustomization<T, C>,
    item: T,
    tableData: E,
    isSelected: Boolean,
    isInRowBlock: Boolean,
    settings: TableSettings,
    finalRowColor: Color,
    isDynamicRowHeight: Boolean,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    requestTableFocus: () -> Unit,
    horizontalState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.onGloballyPositioned { coordinates ->
                state.updateRowHeight(index, coordinates.size.height)
            },
    ) {
        visibleColumns.forEachIndexed { colIndex, spec ->
            val width = state.columns.resolveWidth(spec.key, spec)
            var cellTopLeft by remember(spec.key, index) { mutableStateOf(Offset.Zero) }
            val cellStyle: TableCellStyle =
                customization.resolveCellStyle(
                    TableCellContext(
                        row =
                            TableRowContext(
                                item = item,
                                index = index,
                                isSelected = isSelected,
                                isStriped =
                                    state.settings.stripedRows &&
                                        (index % 2 != 0),
                                isInRowBlock = isInRowBlock,
                                isDeleted = false,
                            ),
                        column = spec.key,
                    ),
                )

            val isCellSelected =
                state.selection.selectedCell?.let { it.rowIndex == index && it.column == spec.key } ==
                    true

            val pinnedState =
                calculatePinnedColumnState(
                    columnIndex = colIndex,
                    totalVisibleColumns = visibleColumns.size,
                    pinnedColumnsCount = settings.pinnedColumnsCount,
                    pinnedColumnsSide = settings.pinnedColumnsSide,
                    horizontalState = horizontalState,
                )

            val appearance =
                pinnedCellAppearance(
                    pinnedState = pinnedState,
                    cellStyle = cellStyle,
                    rowColor = finalRowColor,
                    dimensions = dimensions,
                    showVerticalDividers = state.settings.showVerticalDividers,
                )

            TableCell(
                width = width,
                height = if (isDynamicRowHeight) null else dimensions.rowHeight,
                dividerThickness = appearance.dividerThickness,
                cellStyle = appearance.cellStyle,
                alignment = spec.alignment,
                isSelected = isCellSelected,
                showLeftDivider = pinnedState.isFirstRightPinned,
                leftDividerThickness = dimensions.pinnedColumnDividerThickness,
                showRightDivider = appearance.showRightDivider,
                isPinned = pinnedState.isPinned,
                modifier =
                    Modifier
                        .zIndex(pinnedState.zIndex)
                        .graphicsLayer { this.translationX = pinnedState.translationX }
                        .onGloballyPositioned { coordinates ->
                            cellTopLeft = coordinates.positionInRoot()
                        }.then(
                            Modifier
                                .tableRowInteractions(
                                    item = item,
                                    onFocus = {
                                        if (canMoveFocusTo(state, settings, index)) {
                                            requestTableFocus()
                                            state.selection.selectCell(index, spec.key)
                                            state.selection.focusRow(index)
                                        }
                                    },
                                    useSelectAsPrimary =
                                        state.settings.selectionMode !=
                                            SelectionMode.None,
                                    onSelect = { state.selection.toggleRow(index) },
                                    onClick = { clicked ->
                                        onCellClick(state, settings, spec, item, index, clicked, onRowClick)
                                    },
                                    onLongClick = onRowLongClick,
                                    onContextMenu =
                                        onContextMenu?.let { handler ->
                                            { itemCtx, localPos ->
                                                handler(
                                                    itemCtx,
                                                    cellTopLeft + localPos,
                                                )
                                            }
                                        },
                                ),
                        ),
            ) {
                // Determine if we should show edit UI for this cell
                val isRowEditing = state.editing.rowIndex == index
                val shouldShowEditUI = isRowEditing && spec.editable && spec.editCell != null

                if (spec.resizable || spec.autoWidth) {
                    Box(Modifier.size(0.dp)) {
                        MeasureCellMinWidth(
                            item = item,
                            tableData = tableData,
                            measureKey = Pair(spec.key, index),
                            onMeasure = { measuredMinWidth ->
                                val adjusted = maxOf(measuredMinWidth, spec.minWidth)
                                state.columns.updateMaxContentWidth(spec.key, adjusted, source = "Row[$index]")
                            },
                            content = spec.cell,
                        )
                    }
                }

                // Render edit UI or normal cell
                if (shouldShowEditUI) {
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(state.editing.column, state.editing.rowIndex) {
                        val isCurrentEditingCell = state.editing.column == spec.key && state.editing.rowIndex == index
                        if (isCurrentEditingCell) {
                            focusRequester.requestFocus()
                        }
                    }

                    CompositionLocalProvider(
                        LocalEditCellContext provides EditCellContext(index, spec.key as Any),
                        LocalEditCellFocusRequester provides focusRequester,
                    ) {
                        spec.editCell.invoke(this, item, tableData) {
                            // onComplete callback - move to next editable cell
                            state.editing.completeCurrentCell(visibleColumns)
                        }
                    }
                } else {
                    val cellScope =
                        remember(rowScope) {
                            TableCellScopeImpl(rowScope)
                        }
                    context(cellScope) {
                        spec.cell(this@TableCell, item, tableData)
                    }
                }
            }
        }
    }
}

/**
 * Whether focus may move to a cell of row [index].
 *
 * An edit in flight on ANOTHER row has to be completed first, and completing it can fail: an invalid
 * row stays in edit mode rather than losing what the user typed to a stray click elsewhere.
 */
private fun <C> canMoveFocusTo(
    state: TableState<C>,
    settings: TableSettings,
    index: Int,
): Boolean =
    !settings.editingEnabled ||
        state.editing.rowIndex == null ||
        state.editing.rowIndex == index ||
        state.editing.tryComplete()

/**
 * Decides what a click on a cell does: start editing it, or fall through to the row's own action.
 *
 * Editing wins only when the table, the column and the row all allow it — [ColumnSpec.canStartEdit]
 * gets the final say, and a refusal is a plain row click rather than nothing at all.
 */
@Suppress("LongParameterList")
private fun <T : Any, C, E> onCellClick(
    state: TableState<C>,
    settings: TableSettings,
    spec: ColumnSpec<T, C, E>,
    item: T,
    index: Int,
    clicked: T,
    onRowClick: ((T) -> Unit)?,
) {
    if (!canMoveFocusTo(state, settings, index)) return
    val canEdit =
        settings.editingEnabled &&
            spec.editable &&
            (spec.canStartEdit?.invoke(item, index) ?: true)
    if (canEdit) {
        state.editing.start(item, index, spec.key)
    } else {
        onRowClick?.invoke(clicked)
    }
}

/** How a cell renders, given where it sits relative to the pinned run. */
private class PinnedCellAppearance(
    val cellStyle: TableCellStyle,
    val dividerThickness: Dp,
    val showRightDivider: Boolean,
)

/**
 * Resolves a cell's background and dividers against its pinned position.
 *
 * A pinned cell scrolls over its neighbours, so it must be opaque — it falls back to the row colour
 * whenever the style leaves the background unspecified. The divider between the pinned run and the
 * rest is thicker than an ordinary one and is drawn even when vertical dividers are off, since it is
 * the only thing marking the seam; the last cell before a right-pinned run leaves it to that run.
 */
private fun pinnedCellAppearance(
    pinnedState: PinnedColumnState,
    cellStyle: TableCellStyle,
    rowColor: Color,
    dimensions: TableDimensions,
    showVerticalDividers: Boolean,
): PinnedCellAppearance =
    PinnedCellAppearance(
        cellStyle =
            if (pinnedState.isPinned) {
                cellStyle.copy(
                    background = cellStyle.background.takeUnless { it == Unspecified } ?: rowColor,
                )
            } else {
                cellStyle
            },
        dividerThickness =
            if (pinnedState.isLastLeftPinned) {
                dimensions.pinnedColumnDividerThickness
            } else {
                dimensions.dividerThickness
            },
        showRightDivider =
            !pinnedState.isLastBeforeRightPinned &&
                (showVerticalDividers || pinnedState.isLastLeftPinned),
    )
