package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.TableItemScope
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.state.TableState

/**
 * Renders one drag unit: either a single row (identical to the pre-groups layout) or a group block —
 * its rows stacked and divided exactly like standalone rows, over a
 * [TableColors.rowGroupContainerColor] background that also spans the vertical gap around them, so
 * the block reads as one card.
 *
 * Every row keeps its own divider, the last one included, and the block adds none: each rule sits
 * directly under the row it closes, inside the padded content, so the divider rhythm is the same
 * inside a block as anywhere else in the table. The tinted band is therefore pure margin around the
 * block, never crossed or trailed by a rule.
 *
 * A block **always opens with a top band**: the [rowGroupHeader] when there is one, otherwise a
 * `rowGroupSpacing` gap. It **closes with a bottom gap only when [nextIsGroup] is false** — a
 * following block opens with a band of its own, so drawing this one's bottom gap too would stack two
 * tinted regions between them and merge the pair into a single fat slab, erasing the very boundary
 * the band exists to draw. Suppressing the *bottom* gap rather than the next block's top band is
 * what keeps that band a header when one is configured: suppressing the top instead would leave the
 * second of two adjacent blocks nameless.
 *
 * The rule leaves exactly one band between any two units, and still gives a block at either end of
 * the list its own outer band.
 *
 * The header wraps its own content rather than being pinned to `rowGroupSpacing`, but is floored at
 * it: [nextIsGroup] drops a block's bottom gap on the promise that the next block opens with a band,
 * and a header free to measure zero could quietly break that promise — a slot that renders nothing
 * while its leader is still loading would butt two blocks together with no boundary at all. The
 * floor makes a header that draws nothing collapse to exactly the gap it replaced. The header is
 * also offset by the horizontal scroll so it stays in the viewport while the table scrolls sideways
 * — the same technique the `groupBy` header cell uses.
 *
 * The wrapper adds **no horizontal insets**: any horizontal padding here would shift group rows
 * relative to the header and break column alignment.
 */
@Composable
@Suppress("LongParameterList")
context(_: TableItemScope)
internal fun <T : Any, C, E> RowUnit(
    rows: IntRange,
    isGroup: Boolean,
    /** True when the following unit is a group, which opens with a band of its own. */
    nextIsGroup: Boolean,
    /** Optional content for the band above a group block; replaces the block's top gap. */
    rowGroupHeader: (@Composable (rows: IntRange) -> Unit)?,
    itemAt: (Int) -> T?,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableData: E,
    rowEmbedded: (@Composable (rowIndex: Int, item: T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    if (!isGroup) {
        TableBodyRow(
            index = rows.first,
            itemAt = itemAt,
            visibleColumns = visibleColumns,
            state = state,
            colors = colors,
            customization = customization,
            tableData = tableData,
            rowEmbedded = rowEmbedded,
            placeholderRow = placeholderRow,
            onRowClick = onRowClick,
            onRowLongClick = onRowLongClick,
            onContextMenu = onContextMenu,
            horizontalState = horizontalState,
            requestTableFocus = requestTableFocus,
        )
        return
    }

    Column(
        modifier =
            Modifier
                .width(state.tableWidth)
                .background(colors.rowGroupContainerColor)
                .padding(
                    top =
                        if (rowGroupHeader != null) {
                            0.dp
                        } else {
                            state.dimensions.rowGroupSpacing
                        },
                    bottom =
                        if (nextIsGroup) {
                            0.dp
                        } else {
                            state.dimensions.rowGroupSpacing
                        },
                ),
    ) {
        if (rowGroupHeader != null) {
            val viewportWidth = with(LocalDensity.current) { horizontalState.viewportSize.toDp() }
            Box(
                modifier =
                    Modifier
                        .heightIn(min = state.dimensions.rowGroupSpacing)
                        .graphicsLayer {
                            translationX = horizontalState.value.toFloat()
                        },
            ) {
                Box(modifier = Modifier.width(viewportWidth)) {
                    rowGroupHeader(rows)
                }
            }
        }
        rows.forEach { row ->
            TableBodyRow(
                index = row,
                itemAt = itemAt,
                visibleColumns = visibleColumns,
                state = state,
                colors = colors,
                customization = customization,
                tableData = tableData,
                rowEmbedded = rowEmbedded,
                placeholderRow = placeholderRow,
                onRowClick = onRowClick,
                onRowLongClick = onRowLongClick,
                onContextMenu = onContextMenu,
                horizontalState = horizontalState,
                requestTableFocus = requestTableFocus,
            )
        }
    }
}
