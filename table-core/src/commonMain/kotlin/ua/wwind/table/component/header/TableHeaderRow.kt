package ua.wwind.table.component.header

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun <T : Any, C> TableHeaderRow(
    tableWidth: Dp,
    leadingColumnWidth: Dp?,
    lazyListState: LazyListState,
    reorderState: ReorderableLazyListState,
    visibleColumns: List<ColumnSpec<T, C>>,
    widthResolver: (C) -> Dp,
    style: TableHeaderStyle,
    state: TableState<C>,
    strings: StringProvider,
    filterColumn: C?,
    onFilterColumnChange: (C?) -> Unit,
    isResizing: Boolean,
) {
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
                                .fillMaxHeight(),
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = style.dimensions.dividerThickness,
                    )
                }
            }
        }

        items(items = visibleColumns, key = { item -> item.key as Any }) { spec ->
            ReorderableItem(
                state = reorderState,
                key = spec.key as Any,
                animateItemModifier = if (isResizing) Modifier else Modifier.animateItem(),
            ) { isDragging ->
                val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp).value
                Surface(
                    color = style.headerColor,
                    contentColor = style.headerContentColor,
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    modifier = Modifier.draggableHandle(),
                ) {
                    val width = widthResolver(spec.key) + style.dimensions.dividerThickness
                    val clickableModifier =
                        if (spec.sortable && spec.headerClickToSort) {
                            Modifier.clickable {}
                        } else {
                            Modifier
                        }
                    val density = LocalDensity.current
                    var menuExpanded by remember { mutableStateOf(false) }
                    var menuOffset by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
                    var anchorHeight by remember { mutableStateOf(0.dp) }
                    Box(
                        modifier =
                            clickableModifier
                                // Measure anchor height to correct vertical positioning of the menu
                                .onGloballyPositioned { coordinates ->
                                    anchorHeight = with(density) { coordinates.size.height.toDp() }
                                }
                                // Handle right mouse button click to open context menu
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (
                                                event.type == PointerEventType.Press &&
                                                event.buttons.isSecondaryPressed
                                            ) {
                                                val pos = event.changes.firstOrNull()?.position
                                                if (pos != null) {
                                                    menuOffset =
                                                        with(density) {
                                                            DpOffset(
                                                                pos.x.toDp(),
                                                                pos.y.toDp() - anchorHeight,
                                                            )
                                                        }
                                                }
                                                menuExpanded = true
                                            }
                                        }
                                    }
                                }
                                // Handle primary tap and long-press gestures
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if (spec.sortable && spec.headerClickToSort) {
                                                state.setSort(spec.key)
                                            }
                                        },
                                        onLongPress = { offset ->
                                            menuOffset =
                                                with(density) {
                                                    DpOffset(
                                                        offset.x.toDp(),
                                                        offset.y.toDp() - anchorHeight,
                                                    )
                                                }
                                            menuExpanded = true
                                        },
                                    )
                                },
                    ) {
                        HeaderCell(
                            spec = spec,
                            state = state,
                            strings = strings,
                            width = width,
                            dimensions = style.dimensions,
                            isFilterOpen = filterColumn == spec.key,
                            onOpenFilter = { onFilterColumnChange(spec.key) },
                            onDismissFilter = { onFilterColumnChange(null) },
                            onToggleSort = { state.setSort(spec.key) },
                        )

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = menuOffset,
                        ) {
                            if (state.groupBy == spec.key) {
                                DropdownMenuItem(
                                    text = { Text(strings.get(UiString.Ungroup)) },
                                    onClick = {
                                        state.setGrouping(null)
                                        menuExpanded = false
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(strings.get(UiString.GroupBy)) },
                                    onClick = {
                                        state.setGrouping(spec.key)
                                        if (spec.sortable && state.sort?.column != spec.key) {
                                            state.setSort(spec.key)
                                        }
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
