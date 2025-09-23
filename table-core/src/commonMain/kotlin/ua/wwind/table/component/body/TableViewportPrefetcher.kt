package ua.wwind.table.component.body

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.TableColors
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.state.TableState

/**
 * Offscreen prefetcher that measures the next "page" of rows equal to the current viewport height.
 * It subcomposes rows sequentially starting from the first index after the last visible item and
 * accumulates their heights in pixels until the total meets/exceeds the viewport height. Measured
 * heights are stored into [TableState.rowHeightsPx] via [TableState.updateRowHeight].
 */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C> TableViewportPrefetcher(
    itemsCount: Int,
    itemAt: (Int) -> T?,
    visibleColumns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    colors: TableColors,
    customization: TableCustomization<T, C>,
    tableWidth: Dp,
    rowLeading: (@Composable (T) -> Unit)?,
    rowTrailing: (@Composable (T) -> Unit)?,
    placeholderRow: (@Composable () -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    onRowLongClick: ((T) -> Unit)?,
    onContextMenu: ((T, Offset) -> Unit)?,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    requestTableFocus: () -> Unit,
) {
    // Only meaningful for dynamic row heights
    if (state.settings.rowHeightMode != RowHeightMode.Dynamic) return
    if (itemsCount <= 0) return

    val density = LocalDensity.current

    // Viewport metrics updated via snapshotFlow to avoid reading layoutInfo in composition
    var viewportHeightPx by remember { mutableStateOf(0) }
    var startIndex by remember { mutableStateOf(0) }

    LaunchedEffect(verticalState, itemsCount) {
        snapshotFlow<Pair<Int, Int>> {
            val li = verticalState.layoutInfo
            val viewport = (li.viewportEndOffset - li.viewportStartOffset).coerceAtLeast(0)
            val lastVisible = li.visibleItemsInfo.maxByOrNull { it.index }?.index ?: -1
            Pair(viewport, (lastVisible + 1).coerceAtMost(itemsCount))
        }.distinctUntilChanged().collect { pair ->
            val (vp, start) = pair
            viewportHeightPx = vp
            startIndex = start
        }
    }

    // We perform subcomposition inside a size(0) layout; we then apply updates via a LaunchedEffect
    var pendingUpdates by remember { mutableStateOf(emptyList<Pair<Int, Int>>()) }

    SubcomposeLayout(Modifier.size(0.dp)) { _ ->
        pendingUpdates = emptyList()
        if (viewportHeightPx <= 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }

        val widthPx = with(density) { tableWidth.roundToPx() }
        val constraints = androidx.compose.ui.unit.Constraints.fixedWidth(widthPx)

        var total = 0
        val updates = ArrayList<Pair<Int, Int>>()
        var i = startIndex

        while (i < itemsCount && total < viewportHeightPx) {
            // Skip if already known
            val known = state.rowHeightsPx[i]
            val h = if (known != null) {
                known
            } else {
                val measurables = subcompose(slotId = i) {
                    TableRowItem(
                        item = itemAt(i),
                        index = i,
                        visibleColumns = visibleColumns,
                        state = state,
                        colors = colors,
                        customization = customization,
                        tableWidth = tableWidth,
                        rowLeading = rowLeading,
                        rowTrailing = rowTrailing,
                        placeholderRow = placeholderRow,
                        onRowClick = onRowClick,
                        onRowLongClick = onRowLongClick,
                        onContextMenu = onContextMenu,
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                        requestTableFocus = requestTableFocus,
                    )
                }
                val placeables = measurables.map { it.measure(constraints) }
                (placeables.maxOfOrNull { it.height } ?: 0).also { measured ->
                    updates += i to measured
                }
            }

            total += h
            i++
        }

        pendingUpdates = updates
        layout(0, 0) {}
    }

    LaunchedEffect(pendingUpdates) {
        if (pendingUpdates.isEmpty()) return@LaunchedEffect
        pendingUpdates.forEach { (index, heightPx) ->
            state.updateRowHeight(index, heightPx)
        }
    }
}
