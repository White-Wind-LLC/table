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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.compose.ui.graphics.RectangleShape
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
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    /** Whether the table holds keyboard focus, which turns the selection border into a focus ring. */
    isTableFocused: Boolean = false,
    showLeftDivider: Boolean = false,
    leftDividerThickness: Dp = dividerThickness,
    showRightDivider: Boolean = true,
    isPinned: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val selectionBorderModifier =
        if (isSelected) {
            Modifier.border(
                2.dp,
                // primary keeps the 3:1 contrast a focus indicator needs against the row containers.
                if (isTableFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(2.dp),
            )
        } else {
            Modifier
        }

    // Resolve the content colour once. When the style leaves it Unspecified we re-provide the
    // ambient LocalContentColor, which is a no-op, so the slot renders exactly as before.
    val resolvedContentColor =
        if (cellStyle.contentColor != Unspecified) cellStyle.contentColor else LocalContentColor.current

    // Emit the slot from a single position so detekt's ContentSlotReused no longer fires and the
    // slot's remembered state survives; the pinned/non-pinned branches wrap around this call.
    val cellContent: @Composable BoxScope.() -> Unit = {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            // merge(null) returns the ambient style unchanged.
            ProvideTextStyle(LocalTextStyle.current.merge(cellStyle.textStyle)) { content() }
        }
    }

    // Both the style and the column default to CenterStart, so only a style that asks for something
    // else overrides the column's alignment.
    val resolvedAlignment = if (cellStyle.alignment != Alignment.CenterStart) cellStyle.alignment else alignment

    Row(modifier = modifier) {
        if (showLeftDivider) {
            VerticalDivider(
                modifier = (if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
                thickness = leftDividerThickness,
            )
        }

        // Use Surface for fixed cells to ensure solid background
        if (isPinned) {
            // For fixed cells, always use Surface even if background is Unspecified
            // This ensures proper opacity and prevents see-through effect
            val backgroundColor =
                if (cellStyle.background != Unspecified) {
                    // Ensure full opacity for fixed cells
                    cellStyle.background.copy(alpha = 1f)
                } else {
                    MaterialTheme.colorScheme.surface
                }

            Surface(
                color = backgroundColor,
                shape = RectangleShape,
                shadowElevation = 0.dp,
                modifier =
                    Modifier
                        .width(width)
                        .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
                        .then(cellStyle.modifier)
                        .then(selectionBorderModifier),
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = resolvedAlignment,
                ) {
                    cellContent()
                }
            }
        } else {
            val backgroundModifier =
                if (cellStyle.background != Unspecified) {
                    Modifier.background(cellStyle.background)
                } else {
                    Modifier
                }

            Box(
                modifier =
                    Modifier
                        .width(width)
                        .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
                        .then(backgroundModifier)
                        .then(cellStyle.modifier)
                        .then(selectionBorderModifier),
                contentAlignment = resolvedAlignment,
            ) {
                cellContent()
            }
        }

        if (showRightDivider) {
            VerticalDivider(
                modifier = (if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
                thickness = dividerThickness,
            )
        }
    }
}
