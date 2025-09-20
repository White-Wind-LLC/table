package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import ua.wwind.table.config.TableCellContext
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.TableRowContext
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.format.data.TableCellStyleConfig
import ua.wwind.table.format.data.TableFormatHorizontalAlignment
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.data.TableFormatTextStyle
import ua.wwind.table.format.data.TableFormatVerticalAlignment

@Composable
public fun <T : Any, C, FILTER> rememberCustomization(
    rules: List<TableFormatRule<C, FILTER>>,
    key: Any? = null,
    matches: (item: T, filter: FILTER) -> Boolean,
    baseRowStyle: (@Composable (TableRowContext<T, C>) -> TableRowStyle)? = null,
    baseCellStyle: (@Composable (TableCellContext<T, C>) -> TableCellStyle)? = null,
): TableCustomization<T, C> {
    return remember(key, rules) {
        object : TableCustomization<T, C> {
            @Composable
            override fun resolveRowStyle(ctx: TableRowContext<T, C>): TableRowStyle {
                val base = baseRowStyle?.invoke(ctx) ?: TableRowStyle()
                // Merge all row-wide rules (columns empty) into a single cell-style accumulator
                var rowWideCellStyle = TableCellStyle()
                rules.forEach { rule ->
                    if (rule.enabled && rule.columns.isEmpty() && matches(ctx.item, rule.filter)) {
                        rowWideCellStyle = rowWideCellStyle.merge(config = rule.cellStyle)
                    }
                }
                val container = rowWideCellStyle.background.takeUnless { it == Color.Unspecified }
                val content = rowWideCellStyle.contentColor.takeUnless { it == Color.Unspecified }
                return base.copy(
                    containerColor = container ?: base.containerColor,
                    contentColor = content ?: base.contentColor,
                )
            }

            @Composable
            override fun resolveCellStyle(ctx: TableCellContext<T, C>): TableCellStyle {
                var style = baseCellStyle?.invoke(ctx) ?: TableCellStyle()
                rules.forEach { rule ->
                    if (rule.enabled &&
                        matches(ctx.row.item, rule.filter) &&
                        (rule.columns.isEmpty() || rule.columns.contains(ctx.column))
                    ) {
                        style = style.merge(config = rule.cellStyle)
                    }
                }
                return style
            }
        }
    }
}

@Composable
private fun TableCellStyle.merge(config: TableCellStyleConfig): TableCellStyle {
    val style = config.textStyle?.toTextStyle()
    return this.copy(
        background = config.backgroundColor?.toColor() ?: this.background,
        contentColor = config.contentColor?.toColor() ?: this.contentColor,
        textStyle = style ?: this.textStyle,
        horizontalArrangement = config.horizontal?.toArrangement() ?: this.horizontalArrangement,
        verticalAlignment = config.vertical?.toAlignment() ?: this.verticalAlignment,
    )
}

internal fun Int.toColor(): Color = Color(this)

private fun TableFormatHorizontalAlignment.toArrangement() =
    when (this) {
        TableFormatHorizontalAlignment.START -> Arrangement.Start
        TableFormatHorizontalAlignment.CENTER -> Arrangement.Center
        TableFormatHorizontalAlignment.END -> Arrangement.End
    }

private fun TableFormatVerticalAlignment.toAlignment() =
    when (this) {
        TableFormatVerticalAlignment.TOP -> Alignment.Top
        TableFormatVerticalAlignment.CENTER -> Alignment.CenterVertically
        TableFormatVerticalAlignment.BOTTOM -> Alignment.Bottom
    }

@Composable
internal fun TableFormatTextStyle.toTextStyle(base: TextStyle = MaterialTheme.typography.bodyMedium) =
    when (this) {
        TableFormatTextStyle.NORMAL -> base
        TableFormatTextStyle.BOLD -> base.copy(fontWeight = FontWeight.Bold)
        TableFormatTextStyle.ITALIC -> base.copy(fontStyle = FontStyle.Italic)
        TableFormatTextStyle.UNDERLINE -> base.copy(textDecoration = TextDecoration.Underline)
        TableFormatTextStyle.STRIKETHROUGH -> base.copy(textDecoration = TextDecoration.LineThrough)
    }
