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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState
import kotlin.math.min

@Composable
@Suppress("LongParameterList")
@ExperimentalTableApi
internal fun <T : Any, C, E> GroupStickyOverlay(
    itemAt: (Int) -> T?,
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
    var overlayOffsetPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightPx =
        remember(state.dimensions.rowHeight, density) {
            with(density) { state.dimensions.rowHeight.roundToPx() }
        }
    val dividerThicknessPx =
        remember(state.dimensions.dividerThickness, density) {
            with(density) { state.dimensions.dividerThickness.roundToPx() }
        }
    val overlayHeightPx = headerHeightPx + dividerThicknessPx
    // Track first visible item layout to compute push-up effect precisely
    LaunchedEffect(verticalState, state.groupBy, itemAt) {
        snapshotFlow {
            val firstInfo = verticalState.layoutInfo.visibleItemsInfo.firstOrNull()
            // Pair of index and bottom-on-screen in px
            Pair(firstInfo?.index ?: -1, (firstInfo?.offset ?: 0) + (firstInfo?.size ?: 0))
        }.collectLatest { (index, bottomOnScreenPx) ->
            if (index < 0) return@collectLatest
            currentItem = itemAt(index)

            val currentValue = currentItem?.let { spec.valueOf(it) }
            val nextValue = itemAt(index + 1)?.let { spec.valueOf(it) }
            val isNextDifferent = currentValue != nextValue

            if (isNextDifferent) {
                // Include row divider thickness to match the actual next row top
                val bottomWithDivider = bottomOnScreenPx + dividerThicknessPx
                overlayOffsetPx = min(0, bottomWithDivider - overlayHeightPx)
            } else {
                overlayOffsetPx = 0
            }
        }
    }

    currentItem?.let { item ->
        val value = spec.valueOf(item)
        val viewportWidthDp = with(density) { horizontalState.viewportSize.toDp() }
        Box(
            modifier =
                Modifier.graphicsLayer {
                    translationY = overlayOffsetPx.toFloat()
                    // Pin horizontally within the viewport by negating current horizontal
                    // scroll
                    translationX = horizontalState.value.toFloat()
                },
        ) {
            Column {
                GroupHeaderCell(
                    value = value,
                    item = item,
                    spec = spec,
                    width = viewportWidthDp,
                    height = state.dimensions.rowHeight,
                    colors = colors,
                    customization = customization,
                )
                HorizontalDivider(
                    thickness = state.dimensions.dividerThickness,
                    modifier = Modifier.width(viewportWidthDp),
                )
            }
        }
    }
}
