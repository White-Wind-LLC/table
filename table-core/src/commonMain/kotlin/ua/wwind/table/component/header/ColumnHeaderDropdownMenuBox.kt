package ua.wwind.table.component.header

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

@Composable
internal fun <T : Any, C> ColumnHeaderDropdownMenuBox(
    spec: ColumnSpec<T, C>,
    state: TableState<C>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var menuExpanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    var anchorHeight by remember { mutableStateOf(0.dp) }
    val strings = currentStrings()

    Box(
        modifier =
            modifier
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
        content()

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
