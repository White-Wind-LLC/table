package ua.wwind.table.component.body

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.config.TableCellStyle

@Composable
internal fun TableCell(
    width: Dp,
    height: Dp?,
    dividerThickness: Dp,
    cellStyle: TableCellStyle,
    alignment: Alignment,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backgroundModifier =
        if (cellStyle.background != Unspecified) Modifier.background(cellStyle.background) else Modifier
    val selectionBorderModifier =
        if (isSelected)
            Modifier.border(
                2.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(2.dp),
            )
        else
            Modifier

    Row(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .width(width)
                    .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
                    .then(backgroundModifier)
                    .then(selectionBorderModifier),
            contentAlignment = alignment,
        ) {
            if (cellStyle.contentColor != Unspecified) {
                CompositionLocalProvider(LocalContentColor provides cellStyle.contentColor) {
                    content()
                }
            } else {
                content()
            }
        }
        VerticalDivider(
            modifier = (if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
            thickness = dividerThickness
        )
    }
}
