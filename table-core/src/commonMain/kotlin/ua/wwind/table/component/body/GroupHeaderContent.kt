package ua.wwind.table.component.body

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.DefaultTableCellScope
import ua.wwind.table.component.header.ColumnHeaderDropdownMenuBox
import ua.wwind.table.component.header.ColumnMenuContext
import ua.wwind.table.config.LocalTableTypography
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableGroupContext
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState

@Composable
internal fun <T : Any, C, E> GroupHeaderCell(
    value: Any?,
    item: T,
    tableData: E,
    spec: ColumnSpec<T, C, E>,
    width: Dp,
    height: Dp,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    /** The sticky overlay passes [TableColors.stickyGroupContainerColor]; inline headers keep the group color. */
    containerColor: Color = colors.groupContainerColor,
) {
    val state = currentTableState() as TableState<C>
    val style: TableCellStyle = customization.resolveGroupStyle(TableGroupContext(column = spec.key, value = value))
    val background: Color = if (style.background != Color.Unspecified) style.background else containerColor
    // A style background may be any color, so only the table's own container keeps groupContentColor.
    val contentColor: Color =
        when {
            style.contentColor != Color.Unspecified -> style.contentColor
            style.background != Color.Unspecified -> contentColorFor(background)
            else -> colors.groupContentColor
        }

    Surface(color = background, contentColor = contentColor) {
        ProvideTextStyle(value = LocalTableTypography.current.groupHeader.merge(style.textStyle)) {
            ColumnHeaderDropdownMenuBox(
                spec = spec,
                state = state,
                context = ColumnMenuContext.GroupHeader,
            ) { _ ->
                Box(
                    contentAlignment = state.settings.groupContentAlignment,
                    // One heading per group, read as a whole rather than cell content piece by piece.
                    modifier =
                        Modifier
                            .width(width)
                            .height(height)
                            .semantics(mergeDescendants = true) { heading() },
                ) {
                    spec.groupHeader?.invoke(this, value) ?: run {
                        context(DefaultTableCellScope) {
                            spec.cell(this@Box, item, tableData)
                        }
                    }
                }
            }
        }
    }
}
