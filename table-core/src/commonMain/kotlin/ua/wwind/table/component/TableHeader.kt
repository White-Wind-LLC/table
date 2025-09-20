package ua.wwind.table.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.component.FilterPanel
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider
import kotlin.math.roundToInt

@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
internal fun <T : Any, C> TableHeader(
    columns: List<ColumnSpec<T, C>>,
    state: TableState<C>,
    tableWidth: Dp,
    headerColor: Color,
    headerContentColor: Color,
    dimensions: TableDimensions,
    strings: StringProvider,
    leadingColumnWidth: Dp? = null,
    icons: TableHeaderIcons =
        TableHeaderIcons(
            sortAsc = Icons.Rounded.ArrowUpward,
            sortDesc = Icons.Rounded.ArrowDownward,
            sortNeutral = Icons.AutoMirrored.Outlined.Sort,
            filterActive = Icons.Rounded.FilterAlt,
            filterInactive = Icons.Outlined.FilterAlt,
        ),
) {
    val lazyListState = remember { LazyListState() }
    var filterColumn by remember { mutableStateOf<C?>(null) }
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            // Map visible indices (what user sees) to indices in the full columnOrder
            val leadingOffset = if (leadingColumnWidth != null) 1 else 0
            val fromVisibleIndex = from.index - leadingOffset
            val toVisibleIndex = to.index - leadingOffset

            // Build current visible keys in the same order as rendered
            val visibleKeySet = columns.filter { it.visible }.map { it.key }.toSet()
            val fullOrder = state.columnOrder.toList()
            val visibleKeys = fullOrder.filter { visibleKeySet.contains(it) }

            if (fromVisibleIndex !in visibleKeys.indices) return@rememberReorderableLazyListState

            val fromKey = visibleKeys[fromVisibleIndex]
            val fromIndexInFull = fullOrder.indexOf(fromKey)
            if (fromIndexInFull == -1) return@rememberReorderableLazyListState

            // Simulate removal to compute target reference key correctly
            val fullAfterRemoval = fullOrder.toMutableList().apply { removeAt(fromIndexInFull) }
            val visibleAfterRemoval = fullAfterRemoval.filter { visibleKeySet.contains(it) }

            val targetIndexInFull =
                if (toVisibleIndex >= visibleAfterRemoval.size) {
                    // Drop after the last visible column: insert after the last visible in the full order
                    val lastVisibleKey = visibleAfterRemoval.lastOrNull()
                    if (lastVisibleKey == null) fullOrder.size else fullOrder.indexOf(lastVisibleKey) + 1
                } else {
                    // Insert before the target visible key
                    val targetBeforeKey = visibleAfterRemoval[toVisibleIndex]
                    fullOrder.indexOf(targetBeforeKey)
                }

            state.moveColumn(fromIndexInFull, targetIndexInFull)
        }

    Surface(color = headerColor, contentColor = headerContentColor) {
        Box {
            LazyRow(
                modifier = Modifier.width(tableWidth),
                state = lazyListState,
                userScrollEnabled = false,
            ) {
                if (leadingColumnWidth != null) {
                    item(key = "leading") {
                        Row {
                            Spacer(
                                modifier =
                                    Modifier
                                        .width(leadingColumnWidth)
                                        .height(dimensions.defaultRowHeight),
                            )
                            VerticalDivider(
                                modifier =
                                    Modifier
                                        .height(dimensions.defaultRowHeight),
                                thickness = dimensions.verticalDividerThickness,
                            )
                        }
                    }
                }
                val visible = state.columnOrder.mapNotNull { key -> columns.find { it.key == key && it.visible } }
                items(items = visible, key = { item -> item.key as Any }) { spec ->
                    ReorderableItem(reorderState, key = spec.key as Any) { isDragging ->
                        val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp).value
                        Surface(
                            color = headerColor,
                            contentColor = headerContentColor,
                            shadowElevation = elevation,
                            tonalElevation = elevation,
                            modifier = Modifier.draggableHandle(),
                        ) {
                            val width =
                                (state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth) +
                                    dimensions.verticalDividerThickness
                            val clickableModifier =
                                if (spec.sortable && spec.headerClickToSort) {
                                    Modifier.clickable { state.setSort(spec.key) }
                                } else {
                                    Modifier
                                }
                            Box(
                                modifier =
                                    clickableModifier
                                        .then(
                                            Modifier
                                                .width(width)
                                                .height(dimensions.defaultRowHeight),
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                val sortOrder = state.sort?.takeIf { it.column == spec.key }?.order
                                val isFilterActive = state.filters[spec.key]?.values?.isEmpty() == false

                                // Provide header cell info to slot content
                                val info =
                                    TableHeaderCellInfo(
                                        columnKey = spec.key,
                                        isSortable = spec.sortable,
                                        sortOrder = sortOrder,
                                        hasFilter = spec.filter != null,
                                        isFilterActive = isFilterActive,
                                        toggleSort = { state.setSort(spec.key) },
                                        sortIcon = {
                                            if (!spec.sortable) return@TableHeaderCellInfo
                                            val sortIcon =
                                                when (sortOrder) {
                                                    SortOrder.DESCENDING -> icons.sortDesc
                                                    SortOrder.ASCENDING -> icons.sortAsc
                                                    null -> icons.sortNeutral
                                                }
                                            Icon(
                                                imageVector = sortIcon,
                                                contentDescription = null,
                                                modifier =
                                                    Modifier
                                                        .then(
                                                            if (!spec.headerClickToSort) {
                                                                Modifier.clickable { state.setSort(spec.key) }
                                                            } else {
                                                                Modifier
                                                            },
                                                        ).size(24.dp),
                                            )
                                        },
                                        filterIcon = {
                                            if (spec.filter == null) return@TableHeaderCellInfo
                                            val active = state.filters[spec.key]?.values?.isEmpty() == false
                                            Box {
                                                IconButton(
                                                    onClick = { filterColumn = spec.key },
                                                    colors = IconButtonDefaults.filledIconButtonColors(),
                                                ) {
                                                    Icon(
                                                        imageVector = if (active) icons.filterActive else icons.filterInactive,
                                                        contentDescription = null,
                                                    )
                                                }
                                                if (filterColumn == spec.key) {
                                                    @Suppress("UNCHECKED_CAST")
                                                    FilterPanel(
                                                        type = spec.filter as? TableFilterType<Any?>,
                                                        state = state.filters[spec.key] as? TableFilterState<Any?>,
                                                        expanded = true,
                                                        onDismissRequest = { filterColumn = null },
                                                        strings = strings,
                                                        autoApplyFilters = state.settings.autoApplyFilters,
                                                        autoFilterDebounce = state.settings.autoFilterDebounce,
                                                        onChange = { newState ->
                                                            state.setFilter(spec.key, newState)
                                                        },
                                                    )
                                                }
                                            }
                                        },
                                    )
                                Row(
                                    modifier =
                                        if (spec.headerDecorations) {
                                            Modifier.padding(horizontal = 16.dp)
                                        } else {
                                            Modifier
                                        },
                                ) {
                                    @Suppress("UNCHECKED_CAST")
                                    CompositionLocalProvider(
                                        LocalTableHeaderCellInfo provides (info as TableHeaderCellInfo<Any?>),
                                        LocalTableHeaderIcons provides icons,
                                    ) {
                                        spec.header()
                                    }
                                    if (spec.headerDecorations) {
                                        info.sortIcon.invoke()
                                    }
                                }
                                if (spec.headerDecorations) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 8.dp),
                                    ) {
                                        info.filterIcon.invoke()
                                    }
                                }
                            }
                            VerticalDivider(
                                modifier =
                                    Modifier
                                        .height(dimensions.defaultRowHeight)
                                        .align(Alignment.CenterEnd),
                                thickness = dimensions.verticalDividerThickness,
                            )
                        }
                    }
                }
            }

            // Draggable resizers overlay
            Row(modifier = Modifier.width(tableWidth)) {
                val density = LocalDensity.current
                val visible = state.columnOrder.mapNotNull { key -> columns.find { it.key == key && it.visible } }
                if (leadingColumnWidth != null) {
                    Spacer(
                        modifier =
                            Modifier
                                .width(leadingColumnWidth + dimensions.verticalDividerThickness)
                                .height(dimensions.defaultRowHeight),
                    )
                }
                visible.forEachIndexed { index, spec ->
                    val currentWidth = state.columnWidths[spec.key] ?: spec.width ?: dimensions.defaultColumnWidth
                    val cellDividerPaddings = dimensions.verticalDividerPaddingHorizontal * (if (index > 0) 2 else 1)
                    Spacer(
                        modifier =
                            Modifier
                                .width(currentWidth - cellDividerPaddings)
                                .height(dimensions.defaultRowHeight),
                    )
                    var offsetX by remember(visible) { mutableStateOf(0f) }
                    Spacer(
                        modifier =
                            Modifier
                                .height(dimensions.defaultRowHeight)
                                .offset { IntOffset(offsetX.roundToInt(), 0) }
                                .then(
                                    if (spec.resizable) {
                                        Modifier
                                            .draggable(
                                                state =
                                                    rememberDraggableState { delta ->
                                                        offsetX += delta
                                                        val deltaDp = with(density) { delta.toDp() }
                                                        val newWidth = (currentWidth + deltaDp).coerceAtLeast(spec.minWidth)
                                                        state.resizeColumn(spec.key, ColumnWidthAction.Set(newWidth))
                                                    },
                                                onDragStopped = { offsetX = 0f },
                                                orientation = Orientation.Horizontal,
                                            ).pointerHoverIcon(PointerIcon.Hand)
                                    } else {
                                        Modifier
                                    },
                                ).padding(horizontal = dimensions.verticalDividerPaddingHorizontal)
                                .width(dimensions.verticalDividerThickness),
                    )
                }
            }
        }
    }
}
