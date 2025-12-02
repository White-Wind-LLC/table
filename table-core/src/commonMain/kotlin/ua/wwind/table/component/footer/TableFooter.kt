package ua.wwind.table.component.footer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDimensions

@Composable
internal fun <T : Any, C, E> TableFooter(
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    footerColor: Color,
    footerContentColor: Color,
    dimensions: TableDimensions,
    horizontalState: ScrollState,
    tableWidth: Dp,
    pinnedColumnsCount: Int,
    pinnedColumnsSide: PinnedSide,
    pinned: Boolean = true,
) {
    Surface(color = footerColor, contentColor = footerContentColor) {
        Box(modifier = Modifier.width(tableWidth)) {
            TableFooterRow(
                visibleColumns = visibleColumns,
                widthResolver = widthResolver,
                footerColor = footerColor,
                footerContentColor = footerContentColor,
                dimensions = dimensions,
                horizontalState = horizontalState,
                pinnedColumnsCount = pinnedColumnsCount,
                pinnedColumnsSide = pinnedColumnsSide,
            )
        }
    }
}
