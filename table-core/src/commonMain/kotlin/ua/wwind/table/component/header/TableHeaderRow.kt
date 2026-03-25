package ua.wwind.table.component.header

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculatePinnedColumnState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C, E> TableHeaderRow(
    lazyListState: LazyListState,
    reorderState: ReorderableLazyListState,
    visibleColumns: ImmutableList<ColumnSpec<T, C, E>>,
    widthResolver: (C) -> Dp,
    style: TableHeaderStyle,
    state: TableState<C>,
    tableData: E,
    strings: StringProvider,
    filterColumn: C?,
    onFilterColumnChange: (C?) -> Unit,
    isResizing: Boolean,
    horizontalState: ScrollState,
) {
    val settings = state.settings
    val isMobilePlatform = remember { getPlatform().isMobile() }

    LazyRow(
        modifier = Modifier.width(state.tableWidth),
        state = lazyListState,
        userScrollEnabled = false,
    ) {
        items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
            val index = visibleColumns.indexOf(spec)

            val pinnedState =
                calculatePinnedColumnState(
                    columnIndex = index,
                    totalVisibleColumns = visibleColumns.size,
                    pinnedColumnsCount = settings.pinnedColumnsCount,
                    pinnedColumnsSide = settings.pinnedColumnsSide,
                    horizontalState = horizontalState,
                )

            ReorderableItem(
                state = reorderState,
                key = spec.key as Any,
                animateItemModifier = if (isResizing) Modifier else Modifier.animateItem(),
                enabled = !pinnedState.isPinned,
                modifier =
                    Modifier
                        .zIndex(pinnedState.zIndex)
                        .graphicsLayer {
                            this.translationX = pinnedState.translationX
                        },
            ) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp).value

                Surface(
                    color = style.headerColor,
                    contentColor = style.headerContentColor,
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    modifier =
                        if (isMobilePlatform) {
                            Modifier.draggableHandle(enabled = !pinnedState.isPinned)
                        } else {
                            Modifier
                        },
                ) {
                    val width = widthResolver(spec.key)
                    val headerHoverInteraction = remember(spec.key) { MutableInteractionSource() }
                    val isHeaderHovered =
                        if (isMobilePlatform) {
                            false
                        } else {
                            headerHoverInteraction.collectIsHoveredAsState().value
                        }
                    val showDragHandle = !isMobilePlatform && (isHeaderHovered || isDragging) && !pinnedState.isPinned

                    ColumnHeaderDropdownMenuBox(
                        spec = spec,
                        state = state,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .then(
                                        if (isMobilePlatform) {
                                            Modifier
                                        } else {
                                            Modifier.hoverable(interactionSource = headerHoverInteraction)
                                        },
                                    ).fillMaxSize(),
                        ) {
                            val dividerThickness =
                                if (pinnedState.isLastLeftPinned) {
                                    style.dimensions.pinnedColumnDividerThickness
                                } else {
                                    style.dimensions.dividerThickness
                                }

                            HeaderCell(
                                spec = spec,
                                state = state,
                                tableData = tableData,
                                strings = strings,
                                width = width,
                                dividerThickness = dividerThickness,
                                isFilterOpen = filterColumn == spec.key,
                                onOpenFilter = { onFilterColumnChange(spec.key) },
                                onDismissFilter = { onFilterColumnChange(null) },
                                onToggleSort = { state.setSort(spec.key) },
                                showLeftDivider = pinnedState.isFirstRightPinned,
                                leftDividerThickness = style.dimensions.pinnedColumnDividerThickness,
                                showRightDivider =
                                    !pinnedState.isLastBeforeRightPinned &&
                                        (state.settings.showVerticalDividers || pinnedState.isLastLeftPinned),
                            )

                            if (showDragHandle) {
                                Icon(
                                    imageVector = Icons.Filled.DragIndicator,
                                    contentDescription = "Drag column",
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 2.dp, top = 2.dp)
                                            .size(16.dp)
                                            .draggableHandle(enabled = true),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
