package ua.wwind.table.component.body

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.component.currentTableMotion
import ua.wwind.table.component.pinnedEdgeShadow
import ua.wwind.table.config.TableCellStyle
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.state.PinnedEdge
import ua.wwind.table.state.currentTableState

@Composable
internal fun TableCell(
    width: Dp,
    height: Dp?,
    dividerThickness: Dp,
    cellStyle: TableCellStyle,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    /** Whether digits use tabular (fixed-width) figures so numbers line up across rows. */
    tabularFigures: Boolean = false,
    isSelected: Boolean = false,
    /** Whether the table holds keyboard focus, which turns the selection border into a focus ring. */
    isTableFocused: Boolean = false,
    showStartDivider: Boolean = false,
    startDividerThickness: Dp = dividerThickness,
    showEndDivider: Boolean = true,
    isPinned: Boolean = false,
    /** The pinned-run edge this cell sits on; its end divider then takes the pinned color. */
    pinnedEdge: PinnedEdge = PinnedEdge.None,
    /** Whether content scrolls under [pinnedEdge], which fades in the edge shadow. */
    hasContentUnderEdge: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = currentTableColors()
    val decorationModifier = cellDecoration(isSelected, isTableFocused, pinnedEdge, hasContentUnderEdge)

    // Resolve the content colour once. When the style leaves it Unspecified we re-provide the
    // ambient LocalContentColor, which is a no-op, so the slot renders exactly as before.
    val resolvedContentColor =
        if (cellStyle.contentColor != Unspecified) cellStyle.contentColor else LocalContentColor.current

    // Emit the slot from a single position so detekt's ContentSlotReused no longer fires and the
    // slot's remembered state survives; the pinned/non-pinned branches wrap around this call.
    val cellContent: @Composable BoxScope.() -> Unit = {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            // merge(null) returns the ambient style unchanged; the cell style still wins over tnum.
            ProvideTextStyle(LocalTextStyle.current.withTabularFigures(tabularFigures).merge(cellStyle.textStyle)) {
                content()
            }
        }
    }

    // Both the style and the column default to CenterStart, so only a style that asks for something
    // else overrides the column's alignment.
    val resolvedAlignment = if (cellStyle.alignment != Alignment.CenterStart) cellStyle.alignment else alignment

    Row(modifier = modifier.then(decorationModifier.shadow)) {
        if (showStartDivider) {
            VerticalDivider(
                modifier = (if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
                thickness = startDividerThickness,
                color = colors.pinnedDividerColor,
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
                        .then(decorationModifier.selectionBorder),
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
                        .then(decorationModifier.selectionBorder),
                contentAlignment = resolvedAlignment,
            ) {
                cellContent()
            }
        }

        if (showEndDivider) {
            VerticalDivider(
                modifier = (if (height != null) Modifier.height(height) else Modifier.fillMaxHeight()),
                thickness = dividerThickness,
                color = if (pinnedEdge == PinnedEdge.End) colors.pinnedDividerColor else colors.dividerColor,
            )
        }
    }
}

/** Modifiers decorating a cell: the edge shadow goes on the whole cell, the border on its content box. */
private class CellDecoration(
    val shadow: Modifier,
    val selectionBorder: Modifier,
)

@Composable
private fun cellDecoration(
    isSelected: Boolean,
    isTableFocused: Boolean,
    pinnedEdge: PinnedEdge,
    hasContentUnderEdge: Boolean,
): CellDecoration {
    val dimensions = currentTableState().effectiveDimensions
    val selectionBorder =
        if (isSelected) {
            Modifier.border(
                dimensions.focusIndicatorWidth,
                // primary keeps the 3:1 contrast a focus indicator needs against the row containers.
                if (isTableFocused) currentTableColors().focusIndicatorColor else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(2.dp),
            )
        } else {
            Modifier
        }
    val shadowAlpha = animateFloatAsState(if (hasContentUnderEdge) 1f else 0f, currentTableMotion().settle())
    val shadow =
        Modifier.pinnedEdgeShadow(
            edge = pinnedEdge,
            width = dimensions.pinnedColumnShadowWidth,
            alpha = { shadowAlpha.value },
        )
    return CellDecoration(shadow = shadow, selectionBorder = selectionBorder)
}

private val TabularFigures = TextStyle(fontFeatureSettings = "tnum")

private fun TextStyle.withTabularFigures(enabled: Boolean): TextStyle = if (enabled) merge(TabularFigures) else this
