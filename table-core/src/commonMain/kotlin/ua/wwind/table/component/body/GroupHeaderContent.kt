package ua.wwind.table.component.body

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableGroupContext
import ua.wwind.table.state.currentTableState

@Composable
internal fun <T : Any, C> GroupHeaderCell(
    value: Any?,
    item: T,
    spec: ColumnSpec<T, C>,
    width: Dp,
    height: Dp,
    colors: TableColors,
    customization: TableCustomization<T, C>,
) {
    val state = currentTableState()
    val style: TableCellStyle = customization.resolveGroupStyle(TableGroupContext(column = spec.key, value = value))
    val background: Color = if (style.background != Color.Unspecified) style.background else colors.groupContainerColor
    val contentColor: Color =
        if (style.contentColor != Color.Unspecified) style.contentColor else contentColorFor(background)

    Surface(color = background, contentColor = contentColor) {
        ProvideTextStyle(value = style.textStyle ?: LocalTextStyle.current) {
            Box(
                contentAlignment = state.settings.groupContentAlignment,
                modifier =
                    Modifier
                        .width(width)
                        .height(height),
            ) {
                spec.groupHeader?.invoke(this, value) ?: run {
                    spec.cell.invoke(this, item)
                }
            }
        }
    }
}
