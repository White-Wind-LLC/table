package ua.wwind.table.component.body

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.MeasureCellMinWidth
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.interaction.tableRowInteractions
import ua.wwind.table.state.TableState

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
internal fun <T : Any, C> TableRowItem(
    item: T?,
    index: Int,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    colors: ua.wwind.table.config.TableColors,
    customization: TableCustomization<T, C>,
    tableWidth: Dp,
    rowLeading: (@Composable (T) -> Unit)?,
    rowTrailing: (@Composable (T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    requestTableFocus: () -> Unit,
) {
    val dimensions = state.dimensions
    val isSelected = state.selectedIndex == index
    val isDynamicRowHeight = state.settings.rowHeightMode == RowHeightMode.Dynamic

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

        Row(
            modifier =
                rowModifier.onGloballyPositioned { coordinates ->
                    state.updateRowHeight(index, coordinates.size.height)
                },
        ) {
            item?.let { itItem ->
                if (rowLeading != null) {
                    RowLeadingSection(
                        cellWidth = dimensions.rowHeight,
                        height = if (isDynamicRowHeight) null else dimensions.rowHeight,
                        dividerThickness = dimensions.dividerThickness,
                        rowLeading = rowLeading,
                        item = itItem,
                    )
                }

                visibleColumns.forEach { spec ->
                    val width = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
                    var cellTopLeft by remember(spec.key, index) { mutableStateOf(Offset.Zero) }
                    val cellStyle: TableCellStyle =
                        customization.resolveCellStyle(
                            TableCellContext(
                                row =
                                    TableRowContext(
                                        item = itItem,
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

                    TableCell(
                        width = width,
                        height = if (isDynamicRowHeight) null else dimensions.rowHeight,
                        dividerThickness = dimensions.dividerThickness,
                        cellStyle = cellStyle,
                        alignment = spec.alignment,
                        isSelected = isCellSelected,
                        modifier =
                            Modifier
                                .onGloballyPositioned { coordinates ->
                                    cellTopLeft = coordinates.positionInRoot()
                                }.then(
                                    Modifier.tableRowInteractions(
                                        item = itItem,
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
                                    item = itItem,
                                    measureKey = Pair(spec.key, index),
                                    content = spec.cell,
                                ) { measuredMinWidth ->
                                    val adjusted = maxOf(measuredMinWidth, spec.minWidth)
                                    state.updateMaxContentWidth(spec.key, adjusted)
                                }
                            }
                        }
                        spec.cell.invoke(this, itItem)
                    }
                }

                rowTrailing?.invoke(itItem)
            } ?: run {
                Row(
                    modifier = (if (isDynamicRowHeight) Modifier else Modifier.height(dimensions.rowHeight)),
                    verticalAlignment = Alignment.CenterVertically,
                ) { placeholderRow?.invoke() }
            }
        }
    }
}
