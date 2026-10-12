package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.sign
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState
import kotlin.math.min

/**
 * Most of the complexity is nothing-to-draw guards and optional slots; the rest computes how far the
 * sticky header is pushed up by the next group's first row. Extracting the push-up sum would leave
 * the guards behind and gain nothing, so `CyclomaticComplexMethod` is suppressed rather than fixed.
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun <T : Any, C, E> GroupStickyOverlay(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    tableData: E,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    customization: TableCustomization<T, C>,
    colors: TableColors,
    verticalState: LazyListState,
    horizontalState: ScrollState,
) {
    @Suppress("UNCHECKED_CAST")
    val state = currentTableState() as TableState<C>
    val groupKey = state.groupBy ?: return
    val spec = visibleColumns.firstOrNull { it.key == groupKey } ?: return

    var currentItem by remember { mutableStateOf<T?>(null) }
    var overlayOffsetPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val headerHeightPx =
        remember(state.effectiveDimensions.rowHeight, density) {
            with(density) { state.effectiveDimensions.rowHeight.roundToPx() }
        }
    val dividerThicknessPx =
        remember(state.effectiveDimensions.dividerThickness, density) {
            with(density) { state.effectiveDimensions.dividerThickness.roundToPx() }
        }
    // Read through updated state: a paged loader passes a new itemAt on every emission, and keying
    // the effect on it would restart the collection each time. Being state, these are also tracked
    // by the snapshotFlow below, so a new loader, spec or height recomputes the header.
    val currentItemAt by rememberUpdatedState(itemAt)
    val currentItemsCount by rememberUpdatedState(itemsCount)
    val currentSpec by rememberUpdatedState(spec)
    val currentOverlayHeightPx by rememberUpdatedState(headerHeightPx + dividerThicknessPx)
    val currentDividerThicknessPx by rememberUpdatedState(dividerThicknessPx)
    LaunchedEffect(verticalState, state.groupBy) {
        // Rows are read inside the flow so that snapshot-backed data (a SnapshotStateList behind an
        // unchanged loader, a row that finishes loading) re-emits too.
        snapshotFlow {
            val firstInfo = verticalState.layoutInfo.visibleItemsInfo.firstOrNull()
            val count = currentItemsCount
            // groupBy suppresses row blocks, so every lazy item before the footer is one row and
            // the row count bounds the index; the footer itself sits at index == count.
            if (firstInfo == null || firstInfo.index !in 0 until count) {
                return@snapshotFlow StickyHeader<T>(item = null, offsetPx = 0)
            }
            val index = firstInfo.index
            val item = currentItemAt(index)
            val currentValue = item?.let { currentSpec.valueOf(it) }
            val nextValue = if (index + 1 < count) currentItemAt(index + 1)?.let { currentSpec.valueOf(it) } else null
            val offsetPx =
                if (currentValue != nextValue) {
                    // Include row divider thickness to match the actual next row top
                    val bottomWithDivider = firstInfo.offset + firstInfo.size + currentDividerThicknessPx
                    min(0, bottomWithDivider - currentOverlayHeightPx)
                } else {
                    0
                }
            StickyHeader(item, offsetPx)
        }.collect { header ->
            currentItem = header.item
            overlayOffsetPx = header.offsetPx
        }
    }

    currentItem?.let { item ->
        val value = spec.valueOf(item)
        val viewportWidthDp = with(density) { horizontalState.viewportSize.toDp() }
        // A visual copy of a header that is already in the body; screen readers read that one.
        Box(
            modifier =
                Modifier.clearAndSetSemantics {}.graphicsLayer {
                    translationY = overlayOffsetPx.toFloat()
                    // Hold still horizontally by cancelling the scroll, which moves content left in LTR
                    // and right in RTL.
                    translationX = layoutDirection.sign(horizontalState.value.toFloat())
                },
        ) {
            Column {
                GroupHeaderCell(
                    value = value,
                    item = item,
                    tableData = tableData,
                    spec = spec,
                    width = viewportWidthDp,
                    height = state.effectiveDimensions.rowHeight,
                    colors = colors,
                    customization = customization,
                    containerColor = colors.stickyGroupContainerColor,
                )
                if (state.settings.showRowDividers) {
                    HorizontalDivider(
                        thickness = state.effectiveDimensions.dividerThickness,
                        color = currentTableColors().dividerColor,
                        modifier = Modifier.width(viewportWidthDp),
                    )
                }
            }
        }
    }
}

private data class StickyHeader<T>(
    val item: T?,
    val offsetPx: Int,
)
