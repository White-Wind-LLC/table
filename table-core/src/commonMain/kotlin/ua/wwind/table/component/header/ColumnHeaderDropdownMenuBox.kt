package ua.wwind.table.component.header

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.ColumnMenuBuilder
import ua.wwind.table.component.ColumnMenuItem
import ua.wwind.table.component.LocalColumnMenuBuilder
import ua.wwind.table.config.isInteractionLockByRowReorderEnabled
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.state.ColumnMenuRequest
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.currentStrings

/**
 * Hosts the column menu for [spec]: opens it on right-click or long-press, renders the sections the
 * table's [ColumnMenuBuilder] returns, and, in a column header, exposes the enabled items as
 * accessibility custom actions. [content] receives an `openMenu` callback, or null when the column
 * has no menu.
 */
@Composable
internal fun <T : Any, C, E> ColumnHeaderDropdownMenuBox(
    spec: ColumnSpec<T, C, E>,
    state: TableState<C>,
    context: ColumnMenuContext,
    modifier: Modifier = Modifier,
    onOpenFilter: () -> Unit = {},
    content: @Composable (openMenu: (() -> Unit)?) -> Unit,
) {
    val density = LocalDensity.current
    var anchorHeight by remember { mutableStateOf(0.dp) }
    val strings = currentStrings()

    @Suppress("UNCHECKED_CAST")
    val builder = LocalColumnMenuBuilder.current as ColumnMenuBuilder<C>
    val sections =
        builder
            .build(spec.key, columnMenuModel(spec, state, context, onOpenFilter).resolve(strings))
            .filter { it.items.isNotEmpty() }
    val hasMenu = sections.isNotEmpty()
    val request = state.columnMenuRequest
    val expanded = hasMenu && request.isFor(spec.key, context)

    val openAt by rememberUpdatedState { position: Offset? ->
        if (hasMenu) {
            val offset = position?.let { with(density) { DpOffset(it.x.toDp(), it.y.toDp() - anchorHeight) } }
            state.columnMenuRequest = ColumnMenuRequest(spec.key, context, offset, fromKeyboard = false)
        }
    }
    val sortOnTap by rememberUpdatedState(
        spec.sortable && spec.headerClickToSort && !state.settings.isInteractionLockByRowReorderEnabled,
    )

    Box(
        modifier =
            modifier
                .onGloballyPositioned { anchorHeight = with(density) { it.size.height.toDp() } }
                .columnMenuGestures(
                    state = state,
                    openAt = { openAt(it) },
                    onTap = { if (sortOnTap) state.setSort(spec.key) },
                ).then(
                    if (context == ColumnMenuContext.Header) {
                        Modifier.columnMenuActions(sections.flatMap { it.items })
                    } else {
                        Modifier
                    },
                ),
    ) {
        content(if (hasMenu) ({ openAt(null) }) else null)

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { state.closeColumnMenu() },
            offset = request?.offset ?: DpOffset.Zero,
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) HorizontalDivider()
                section.items.forEach { item ->
                    ColumnMenuItemRow(item) {
                        state.closeColumnMenu()
                        item.onClick()
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnMenuItemRow(
    item: ColumnMenuItem,
    onClick: () -> Unit,
) {
    val reason = item.disabledReason.takeIf { !item.enabled }
    DropdownMenuItem(
        text = {
            Column {
                Text(item.label)
                if (reason != null) Text(reason, style = MaterialTheme.typography.bodySmall)
            }
        },
        onClick = onClick,
        enabled = item.enabled,
        leadingIcon = item.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = if (item.checked) ({ Icon(TableIcons.Check, contentDescription = null) }) else null,
        modifier = if (reason != null) Modifier.semantics { stateDescription = reason } else Modifier,
    )
}

private fun <C> ColumnMenuRequest<C>?.isFor(
    column: C,
    context: ColumnMenuContext,
): Boolean = this != null && this.column == column && this.context == context

/** Right-click and long-press open the menu; a primary tap runs [onTap]. */
private fun Modifier.columnMenuGestures(
    state: TableState<*>,
    openAt: (Offset?) -> Unit,
    onTap: () -> Unit,
): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    openAt(event.changes.firstOrNull()?.position)
                }
            }
        }
    }.pointerInput(state) {
        detectTapGestures(onTap = { onTap() }, onLongPress = { offset -> openAt(offset) })
    }

/** Marks the header and exposes the enabled [items] as accessibility custom actions. */
private fun Modifier.columnMenuActions(items: List<ColumnMenuItem>): Modifier =
    semantics {
        heading()
        val actions = items.filter { it.enabled }
        if (actions.isNotEmpty()) {
            customActions =
                actions.map { item ->
                    CustomAccessibilityAction(item.label) {
                        item.onClick()
                        true
                    }
                }
        }
    }
