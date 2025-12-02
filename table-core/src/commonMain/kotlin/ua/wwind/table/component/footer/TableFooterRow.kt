package ua.wwind.table.component.footer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.body.TableCell
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.state.calculatePinnedColumnState

@Composable
internal fun <T : Any, C, E> TableFooterRow(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    tableData: E,
    footerColor: Color,
    footerContentColor: Color,
    dimensions: TableDimensions,
    horizontalState: ScrollState,
    pinnedColumnsCount: Int,
    pinnedColumnsSide: PinnedSide,
) {
    Row(
        modifier = Modifier.height(dimensions.footerHeight),
    ) {
        visibleColumns.forEachIndexed { colIndex, spec ->
            val width = widthResolver(spec.key)
            val pinnedState =
                calculatePinnedColumnState(
                    columnIndex = colIndex,
                    totalVisibleColumns = visibleColumns.size,
                    pinnedColumnsCount = pinnedColumnsCount,
                    pinnedColumnsSide = pinnedColumnsSide,
                    horizontalState = horizontalState,
                )

            val dividerThickness =
                if (pinnedState.isLastLeftPinned) {
                    dimensions.pinnedColumnDividerThickness
                } else {
                    dimensions.dividerThickness
                }

            TableCell(
                width = width,
                height = dimensions.footerHeight,
                dividerThickness = dividerThickness,
                cellStyle =
                    TableCellStyle(
                        background = footerColor,
                        contentColor = footerContentColor,
                    ),
                alignment = spec.alignment,
                isSelected = false,
                showLeftDivider = pinnedState.isFirstRightPinned,
                leftDividerThickness = dimensions.pinnedColumnDividerThickness,
                showRightDivider = !pinnedState.isLastBeforeRightPinned,
                isFixed = pinnedState.isPinned,
                modifier =
                    Modifier
                        .zIndex(pinnedState.zIndex)
                        .graphicsLayer { this.translationX = pinnedState.translationX },
            ) {
                spec.footer?.invoke(this, tableData)
            }
        }
    }
}
