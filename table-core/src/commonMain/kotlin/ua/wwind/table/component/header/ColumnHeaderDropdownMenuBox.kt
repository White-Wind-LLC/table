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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.ColumnMenuBuilder
import ua.wwind.table.component.ColumnMenuItem
import ua.wwind.table.component.ColumnMenuSection
import ua.wwind.table.component.LocalColumnMenuBuilder
import ua.wwind.table.config.isInteractionLockByRowReorderEnabled
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile
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
    val instance = remember { Any() }
    var anchorHeight by remember { mutableStateOf(0.dp) }
    val sections = columnMenuSections(spec, state, context, onOpenFilter)
    val hasMenu = sections.isNotEmpty()
    val request = state.columnMenuRequest
    val expanded = hasMenu && request.isFor(spec.key, context, instance)
    val isMobile = remember { getPlatform().isMobile() }

    // A pointer press anywhere on a column header, its sort, filter and menu buttons included, moves the
    // header Tab stop there, so the keyboard picks up where the mouse was.
    val focusHeader by rememberUpdatedState {
        if (context == ColumnMenuContext.Header && !isMobile) state.focusHeaderFromPointer(spec.key)
    }
    val openAt by rememberUpdatedState { position: Offset? ->
        if (hasMenu) {
            val offset = position?.let { with(density) { DpOffset(it.x.toDp(), it.y.toDp() - anchorHeight) } }
            state.columnMenuRequest =
                ColumnMenuRequest(spec.key, context, offset, fromKeyboard = false, anchor = instance)
        }
    }
    val onTap by rememberUpdatedState {
        if (spec.sortable && spec.headerClickToSort && !state.settings.isInteractionLockByRowReorderEnabled) {
            state.setSort(spec.key)
        }
    }

    Box(
        modifier =
            modifier
                .onGloballyPositioned { anchorHeight = with(density) { it.size.height.toDp() } }
                .columnMenuGestures(
                    state = state,
                    openAt = { openAt(it) },
                    onPress = { focusHeader() },
                    onTap = { onTap() },
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
            modifier = Modifier.closeOnEscape { state.closeColumnMenu() },
        ) {
            ColumnMenuItems(sections, focusFirstItem = request?.fromKeyboard == true) { item ->
                state.closeColumnMenu()
                item.onClick()
            }
        }
    }
}

/**
 * The non-empty sections of [spec]'s menu. The table's [ColumnMenuBuilder] shapes the column-header
 * menu only; a group-header menu shows its defaults unchanged.
 */
@Composable
private fun <C> columnMenuSections(
    spec: ColumnSpec<*, C, *>,
    state: TableState<C>,
    context: ColumnMenuContext,
    onOpenFilter: () -> Unit,
): List<ColumnMenuSection> {
    // Derived so a resize drag, which rewrites the width on every frame, recomposes the menu only when
    // one of these flips.
    val hasWidthOverride by remember(state, spec.key) { derivedStateOf { spec.key in state.columns.widths } }
    val canAutoFit by remember(state, spec.key) { derivedStateOf { spec.key in state.columns.contentMaxWidths } }
    val defaults =
        columnMenuModel(spec, state, context, onOpenFilter, hasWidthOverride, canAutoFit).resolve(currentStrings())
    val sections =
        when (context) {
            ColumnMenuContext.Header -> {
                @Suppress("UNCHECKED_CAST")
                (LocalColumnMenuBuilder.current as ColumnMenuBuilder<C>).build(spec.key, defaults)
            }

            ColumnMenuContext.GroupHeader -> {
                defaults
            }
        }
    return sections.filter { it.items.isNotEmpty() }
}

/**
 * The menu rows, divided by section. A menu opened from the keyboard focuses its first enabled item,
 * so the arrows, Enter and Esc work inside it straight away.
 */
@Composable
private fun ColumnMenuItems(
    sections: List<ColumnMenuSection>,
    focusFirstItem: Boolean,
    onItemClick: (ColumnMenuItem) -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    val firstEnabled = sections.firstNotNullOfOrNull { section -> section.items.firstOrNull { it.enabled } }
    sections.forEachIndexed { index, section ->
        if (index > 0) HorizontalDivider()
        section.items.forEach { item ->
            ColumnMenuItemRow(
                item = item,
                onClick = { onItemClick(item) },
                modifier = if (item === firstEnabled) Modifier.focusRequester(firstItemFocus) else Modifier,
            )
        }
    }
    if (focusFirstItem && firstEnabled != null) {
        LaunchedEffect(Unit) { firstItemFocus.requestFocus() }
    }
}

/**
 * Esc closes the menu while focus is inside it. A desktop window already maps Esc to a popup
 * dismissal through back navigation; handling it here as well keeps the behaviour independent of
 * the host (and of test scenes, which have no back-navigation input).
 */
private fun Modifier.closeOnEscape(onClose: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            onClose()
            true
        } else {
            false
        }
    }

/** Test tag of the trailing check on a checked menu item. */
internal const val CHECKED_ITEM_TAG: String = "column-menu-checked"

/** Test tag of a menu item's leading icon. */
internal const val LEADING_ICON_TAG: String = "column-menu-leading-icon"

@Composable
private fun ColumnMenuItemRow(
    item: ColumnMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        leadingIcon =
            item.icon?.let { icon ->
                { Icon(icon, contentDescription = null, modifier = Modifier.testTag(LEADING_ICON_TAG)) }
            },
        trailingIcon =
            if (item.checked) {
                {
                    Icon(
                        painterResource(TableIcons.Check),
                        contentDescription = null,
                        modifier = Modifier.testTag(CHECKED_ITEM_TAG),
                    )
                }
            } else {
                null
            },
        modifier = if (reason != null) modifier.semantics { stateDescription = reason } else modifier,
    )
}

private fun <C> ColumnMenuRequest<C>?.isFor(
    column: C,
    context: ColumnMenuContext,
    instance: Any,
): Boolean = this != null && this.column == column && this.context == context && (anchor == null || anchor === instance)

/**
 * Every press runs [onPress] first, including one a child button consumes, but not one that starts
 * on a handle marked with [headerHandlePress]. Right-click and long-press then open the menu; a
 * primary tap runs [onTap].
 */
private fun Modifier.columnMenuGestures(
    state: TableState<*>,
    openAt: (Offset?) -> Unit,
    onPress: () -> Unit,
    onTap: () -> Unit,
): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    // A press that started on a drag or resize handle drags; it does not count as one on the header.
                    if (event.changes.none { it.id == state.handlePressPointer }) onPress()
                    if (event.buttons.isSecondaryPressed) openAt(event.changes.firstOrNull()?.position)
                }
            }
        }
    }.pointerInput(state) {
        detectTapGestures(onTap = { onTap() }, onLongPress = { offset -> openAt(offset) })
    }

/**
 * Records a press that starts on this column handle (drag or resize) in
 * [TableState.handlePressPointer], so the header's press listener, which sees the press after it in
 * the main pass, leaves header focus alone.
 */
internal fun Modifier.headerHandlePress(state: TableState<*>): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) {
                    val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                    if (down != null) state.handlePressPointer = down.id
                }
            }
        }
    }

/**
 * Marks the header and exposes the enabled [items] as accessibility custom actions. The header's text
 * merges in, so the heading is announced by its title; its buttons keep nodes of their own.
 */
private fun Modifier.columnMenuActions(items: List<ColumnMenuItem>): Modifier =
    semantics(mergeDescendants = true) {
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
