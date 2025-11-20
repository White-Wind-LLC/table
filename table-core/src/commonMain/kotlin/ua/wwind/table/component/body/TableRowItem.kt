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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculateFixedColumnState

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
internal fun <T : Any, C> TableRowItem(
    item: T?,
    index: Int,
    visibleColumns: ImmutableList<ColumnSpec<T, C>>,
    state: TableState<C>,
    colors: ua.wwind.table.config.TableColors,
    customization: TableCustomization<T, C>,
    tableWidth: Dp,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    requestTableFocus: () -> Unit,
    horizontalState: ScrollState,
) {
    val dimensions = state.dimensions
    val isSelected = state.selectedIndex == index
    val isDynamicRowHeight = state.settings.rowHeightMode == RowHeightMode.Dynamic
    val settings = state.settings

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
                    isGroup = false,
                    isDeleted = false,
                )
            customization.resolveRowStyle(ctx)
        }

    val finalRowColor =
        rowStyle?.containerColor?.takeUnless { it == Unspecified } ?: defaultRowBackgroundColor
    val finalRowContentColor =
        rowStyle?.contentColor?.takeUnless { it == Unspecified } ?: contentColorFor(finalRowColor)

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

        var rowModifier = Modifier.width(tableWidth).then(rowStyle?.modifier ?: Modifier)
        if (isDynamicRowHeight) {
            rowModifier = rowModifier.height(IntrinsicSize.Min)
            if (minRowHeight != null || maxRowHeight != null) {
                rowModifier =
                    rowModifier.heightIn(min = minRowHeight ?: Dp.Unspecified, max = maxRowHeight ?: Dp.Unspecified)
            }
        }

        if (item != null) {
            Column(modifier = Modifier.width(tableWidth)) {
                RenderTableRowItem(
                    rowModifier = rowModifier,
                    state = state,
                    index = index,
                    visibleColumns = visibleColumns,
                    dimensions = dimensions,
                    customization = customization,
                    item = item,
                    isSelected = isSelected,
                    settings = settings,
                    horizontalState = horizontalState,
                    finalRowColor = finalRowColor,
                    isDynamicRowHeight = isDynamicRowHeight,
                    requestTableFocus = requestTableFocus,
                    onRowClick = onRowClick,
                    onRowLongClick = onRowLongClick,
                    onContextMenu = onContextMenu
                )

                rowEmbedded?.invoke(index, item)
            }
        } else {
            Row(
                modifier =
                    if (isDynamicRowHeight)
                        Modifier
                    else
                        Modifier
                            .height(dimensions.rowHeight)
                            .width(tableWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) { placeholderRow?.invoke() }
        }
    }
}

@Composable
private fun <C, T : Any> RenderTableRowItem(
    rowModifier: Modifier,
    state: TableState<C>,
    index: Int,
    visibleColumns: ImmutableList<ColumnSpec<T, C>>,
    dimensions: TableDimensions,
    customization: TableCustomization<T, C>,
    item: T,
    isSelected: Boolean,
    settings: TableSettings,
    horizontalState: ScrollState,
    finalRowColor: Color,
    isDynamicRowHeight: Boolean,
    requestTableFocus: () -> Unit,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?
) {
    Row(
        modifier =
            rowModifier.onGloballyPositioned { coordinates ->
                state.updateRowHeight(index, coordinates.size.height)
            },
    ) {
        visibleColumns.forEachIndexed { colIndex, spec ->
            val width = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
            var cellTopLeft by remember(spec.key, index) { mutableStateOf(Offset.Zero) }
            val cellStyle: TableCellStyle =
                customization.resolveCellStyle(
                    TableCellContext(
                        row =
                            TableRowContext(
                                item = item,
                                index = index,
                                isSelected = isSelected,
                                isStriped = state.settings.stripedRows && (index % 2 != 0),
                                isGroup = false,
                                isDeleted = false,
                            ),
                        column = spec.key,
                    ),
                )

            val isCellSelected =
                state.selectedCell?.let { it.rowIndex == index && it.column == spec.key } == true

            val fixedState =
                calculateFixedColumnState(
                    columnIndex = colIndex,
                    totalVisibleColumns = visibleColumns.size,
                    fixedColumnsCount = settings.fixedColumnsCount,
                    fixedColumnsSide = settings.fixedColumnsSide,
                    horizontalState = horizontalState,
                )

            // For fixed columns, always ensure there's a solid background
            // Use the row background color for fixed cells to prevent transparency
            val finalCellStyle =
                if (fixedState.isFixed) {
                    // Always use row background for fixed columns to ensure opacity
                    val backgroundToUse = if (cellStyle.background != Unspecified) {
                        cellStyle.background
                    } else {
                        finalRowColor
                    }
                    cellStyle.copy(background = backgroundToUse)
                } else {
                    cellStyle
                }
            val dividerThickness =
                if (fixedState.isLastLeftFixed) {
                    dimensions.fixedColumnDividerThickness
                } else {
                    dimensions.dividerThickness
                }

            TableCell(
                width = width,
                height = if (isDynamicRowHeight) null else dimensions.rowHeight,
                dividerThickness = dividerThickness,
                cellStyle = finalCellStyle,
                alignment = spec.alignment,
                isSelected = isCellSelected,
                showLeftDivider = fixedState.isFirstRightFixed,
                leftDividerThickness = dimensions.fixedColumnDividerThickness,
                showRightDivider = !fixedState.isLastBeforeRightFixed,
                isFixed = fixedState.isFixed,
                modifier =
                    Modifier
                        .zIndex(fixedState.zIndex)
                        .graphicsLayer {
                            this.translationX = fixedState.translationX
                        }.onGloballyPositioned { coordinates ->
                            cellTopLeft = coordinates.positionInRoot()
                        }.then(
                            Modifier.tableRowInteractions(
                                item = item,
                                onFocus = {
                                    requestTableFocus()
                                    state.selectCell(index, spec.key)
                                    state.focusRow(index)
                                },
                                useSelectAsPrimary = state.settings.selectionMode != SelectionMode.None,
                                onSelect = { state.toggleSelect(index) },
                                onClick = onRowClick,
                                onLongClick = onRowLongClick,
                                onContextMenu =
                                    onContextMenu?.let { handler ->
                                        { itemCtx, localPos -> handler(itemCtx, cellTopLeft + localPos) }
                                    },
                            ),
                        ),
            ) {
                if (spec.resizable || spec.autoWidth) {
                    Box(Modifier.size(0.dp)) {
                        MeasureCellMinWidth(
                            item = item,
                            measureKey = Pair(spec.key, index),
                            content = spec.cell,
                        ) { measuredMinWidth ->
                            val adjusted = maxOf(measuredMinWidth, spec.minWidth)
                            state.updateMaxContentWidth(spec.key, adjusted)
                        }
                    }
                }
                spec.cell.invoke(this, item)
            }
        }
    }
}
