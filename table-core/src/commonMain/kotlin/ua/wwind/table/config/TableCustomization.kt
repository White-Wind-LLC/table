package ua.wwind.table.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Centralized customization entry-point for table row and cell appearance/behavior.
 * Implementors can compute styles based on row/cell context (item, selection, grouping, etc.).
 */
public interface TableCustomization<T : Any, C> {
    @Composable
    public fun resolveRowStyle(ctx: TableRowContext<T, C>): TableRowStyle = TableRowStyle()

    @Composable
    public fun resolveCellStyle(ctx: TableCellContext<T, C>): TableCellStyle = TableCellStyle()
}

/** Default no-op customization. */
public class DefaultTableCustomization<T : Any, C> : TableCustomization<T, C>

@Immutable
public data class TableRowContext<T : Any, C>(
    val item: T,
    val index: Int,
    val isSelected: Boolean,
    val isStriped: Boolean,
    val isGroup: Boolean,
    val isDeleted: Boolean,
)

@Immutable
public data class TableCellContext<T : Any, C>(
    val row: TableRowContext<T, C>,
    val column: C,
)

@Immutable
public data class TableRowStyle(
    val modifier: Modifier = Modifier,
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val elevation: Dp = Dp.Unspecified,
    val shape: Shape? = RectangleShape,
    val border: BorderStroke? = null,
)

@Immutable
public data class TableCellStyle(
    val modifier: Modifier = Modifier,
    val background: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val textStyle: TextStyle? = null,
    val horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    val verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
)
