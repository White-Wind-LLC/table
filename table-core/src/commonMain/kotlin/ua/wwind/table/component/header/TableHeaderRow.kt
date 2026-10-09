package ua.wwind.table.component.header

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.currentTableMotion
import ua.wwind.table.component.pinnedEdgeShadow
import ua.wwind.table.component.tableAnimateItem
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.platform.ColumnGrabPointerIcon
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile
import ua.wwind.table.state.TableState
import ua.wwind.table.state.calculatePinnedColumnState
import ua.wwind.table.state.currentTableState
import ua.wwind.table.state.hasContentUnder
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

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
    onOpenFilterFromMenu: (C) -> Unit,
    isResizing: Boolean,
    horizontalState: ScrollState,
) {
    val settings = state.settings
    val motion = currentTableMotion()
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
                    pinnedColumnsCount = state.columns.pinnedCount,
                    pinnedColumnsSide = settings.pinnedColumnsSide,
                    horizontalState = horizontalState,
                    layoutDirection = LocalLayoutDirection.current,
                )

            val shadowAlpha =
                animateFloatAsState(if (pinnedState.edge.hasContentUnder(horizontalState)) 1f else 0f, motion.settle())
            ReorderableItem(
                state = reorderState,
                key = spec.key as Any,
                animateItemModifier = if (isResizing) Modifier else tableAnimateItem(motion),
                enabled = !pinnedState.isPinned,
                modifier =
                    Modifier
                        .zIndex(pinnedState.zIndex)
                        .graphicsLayer {
                            this.translationX = pinnedState.translationX
                        }
                        // Outside the Surface, which clips to its shape and would hide the shadow.
                        .pinnedEdgeShadow(
                            edge = pinnedState.edge,
                            width = style.dimensions.pinnedColumnShadowWidth,
                            alpha = { shadowAlpha.value },
                        ),
            ) { isDragging ->
                val elevation =
                    animateDpAsState(
                        if (isDragging) style.dimensions.dragElevation else 0.dp,
                        motion.settle(Dp.VisibilityThreshold),
                    ).value

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
                        context = ColumnMenuContext.Header,
                        onOpenFilter = { onOpenFilterFromMenu(spec.key) },
                    ) { openMenu ->
                        Box(
                            modifier =
                                Modifier
                                    .then(
                                        if (isMobilePlatform) {
                                            Modifier
                                        } else {
                                            Modifier.hoverable(interactionSource = headerHoverInteraction)
                                        },
                                    ).fillMaxSize()
                                    .headerFocusRing(state.showsHeaderFocusRing(spec.key)),
                        ) {
                            val dividerThickness =
                                if (pinnedState.isLastStartPinned) {
                                    style.dimensions.pinnedColumnDividerThickness
                                } else {
                                    style.dimensions.dividerThickness
                                }

                            HeaderCell(
                                spec = spec,
                                state = state,
                                tableData = tableData,
                                strings = strings,
                                width = widthResolver(spec.key),
                                dividerThickness = dividerThickness,
                                isFilterOpen = filterColumn == spec.key,
                                onOpenFilter = { onFilterColumnChange(spec.key) },
                                onDismissFilter = { onFilterColumnChange(null) },
                                onToggleSort = { state.setSort(spec.key) },
                                showStartDivider = pinnedState.isFirstEndPinned,
                                startDividerThickness = style.dimensions.pinnedColumnDividerThickness,
                                showEndDivider =
                                    !pinnedState.isLastBeforeEndPinned &&
                                        (state.settings.showVerticalDividers || pinnedState.isLastStartPinned),
                                pinnedEdge = pinnedState.edge,
                                onOpenMenu = openMenu.takeIf { state.settings.showColumnMenuButton },
                            )

                            if (showDragHandle) {
                                // The glyph stays small in the top-left corner; the target around it is larger.
                                // Its own semantics node, so its label stays out of the heading's label.
                                Box(
                                    contentAlignment = Alignment.TopStart,
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .size(style.dimensions.columnDragHandleSize)
                                            .semantics(mergeDescendants = true) {}
                                            .pointerHoverIcon(ColumnGrabPointerIcon)
                                            .headerHandlePress(state)
                                            .draggableHandle(enabled = true),
                                ) {
                                    Icon(
                                        painter = painterResource(TableIcons.DragIndicator),
                                        contentDescription = strings.get(UiString.HeaderDragColumn),
                                        modifier =
                                            Modifier
                                                .padding(start = 2.dp, top = 2.dp)
                                                .size(style.dimensions.dragHandleIconSize),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The keyboard focus ring of a header cell: the same border a focused body cell shows. */
@Composable
private fun Modifier.headerFocusRing(focused: Boolean): Modifier =
    if (focused) {
        border(
            currentTableState().effectiveDimensions.focusIndicatorWidth,
            currentTableColors().focusIndicatorColor,
            RoundedCornerShape(2.dp),
        )
    } else {
        this
    }
