package ua.wwind.table

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.component.TableMotionSpecs
import ua.wwind.table.component.filterchips.FilterChipItem
import ua.wwind.table.component.filterchips.FilterGroupPopover
import ua.wwind.table.component.filterchips.FilterGroupSummary
import ua.wwind.table.component.filterchips.rowItems
import ua.wwind.table.component.filterchips.summary
import ua.wwind.table.component.header.ColumnFilterPanel
import ua.wwind.table.component.rememberTableMotionSpecs
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.FilterPath
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.filter.data.nodeAt
import ua.wwind.table.filter.data.removeAt
import ua.wwind.table.filter.data.replaceAt
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.state.TableState
import ua.wwind.table.state.clearComplexFilters
import ua.wwind.table.state.updateFilterExpression
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Renders a compact header row with one chip per top-level node of the filter tree.
 * Clicking a condition's chip opens that column's filter panel under the chip; its × resets the filter.
 * A group's × removes the whole group.
 *
 * @param columns all column specs used by the table
 * @param state table state containing current filters
 * @param tableData table data passed to the filter panels (custom filters read it)
 * @param modifier layout modifier for the chips row
 * @param strings string provider for localized labels
 * @param includeClearAllChip whether to include a leading "Clear all" chip to reset all filters
 * @param filterChips how the chips are drawn
 */
@Composable
@Suppress("LongParameterList")
public fun <T : Any, C, E> TableActiveFilters(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    tableData: E,
    modifier: Modifier = Modifier,
    strings: StringProvider = DefaultStrings,
    includeClearAllChip: Boolean = true,
    filterChips: TableFilterChipStyle<C> = TableFilterChipDefaults.style(),
) {
    ActiveFilterChips(
        columns = columns,
        state = state,
        strings = strings,
        includeClearAllChip = includeClearAllChip,
        filterPanel = { spec, filter, onChange, onDismiss ->
            ColumnFilterPanel(spec, state, tableData, strings, onDismiss, filter, onChange)
        },
        groupPopover = { path, keyToSpec, onEdit, onDismiss ->
            FilterGroupPopover(path, state, keyToSpec, tableData, filterChips, strings, onEdit, onDismiss)
        },
        filterChips = filterChips,
        modifier = modifier,
    )
}

/**
 * Renders a compact header row with one chip per top-level node of the filter tree.
 * Without table data the chips cannot open filter panels; their × removes the filter. Groups cannot be opened,
 * so an unresolved condition inside a group goes away only with its group or with "Clear all".
 */
@Deprecated(
    "Pass tableData so that clicking a chip opens its filter panel. Removed in 4.0.",
    ReplaceWith("TableActiveFilters(columns, state, tableData, modifier, strings, includeClearAllChip)"),
)
@Composable
@Suppress("LongParameterList")
public fun <T : Any, C, E> TableActiveFilters(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    modifier: Modifier = Modifier,
    strings: StringProvider = DefaultStrings,
    includeClearAllChip: Boolean = true,
    filterChips: TableFilterChipStyle<C> = TableFilterChipDefaults.style(),
) {
    ActiveFilterChips(
        columns = columns,
        state = state,
        strings = strings,
        includeClearAllChip = includeClearAllChip,
        filterPanel = null,
        groupPopover = null,
        filterChips = filterChips,
        modifier = modifier,
    )
}

/** Renders a column's filter panel bound to one condition: its [filter] and [onChange]. */
internal typealias ActiveFilterPanel<T, C, E> = @Composable (
    spec: ColumnSpec<T, C, E>,
    filter: TableFilterState<*>,
    onChange: (TableFilterState<*>?) -> Unit,
    onDismiss: () -> Unit,
) -> Unit

/**
 * Renders the popover of the group or not at `rootPath`, a top-level path. It calls `onEdit` after each of
 * its own edits and `onDismiss` when it closes; `returnFocus` is false when it closed on a tap elsewhere.
 */
internal typealias ActiveGroupPopover<T, C, E> = @Composable (
    rootPath: FilterPath,
    keyToSpec: Map<C, ColumnSpec<T, C, E>>,
    onEdit: () -> Unit,
    onDismiss: (returnFocus: Boolean) -> Unit,
) -> Unit

/**
 * The chips row, one chip per top-level node of [TableState.filterExpression]. [filterPanel] renders a
 * condition's filter panel under its chip, [groupPopover] a group's popover; null makes those chips not
 * clickable. [footer] is shown under the row while the row is shown.
 */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> ActiveFilterChips(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    strings: StringProvider,
    includeClearAllChip: Boolean,
    filterPanel: ActiveFilterPanel<T, C, E>?,
    groupPopover: ActiveGroupPopover<T, C, E>?,
    filterChips: TableFilterChipStyle<C>,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    val keyToSpec: Map<C, ColumnSpec<T, C, E>> = remember(columns) { columns.associateBy { it.key } }
    val row = remember { ChipRowState<C>() }
    val root = state.filterExpression
    val openConditionPath =
        validOpenPath(root, row.openCondition, onStale = { row.openCondition = null }) { opened, now ->
            now is TableFilterNode.Condition && now.column == (opened as TableFilterNode.Condition).column
        }
    val openGroupPath =
        validOpenPath(root, row.openGroup, onStale = { row.closeGroup(returnFocus = false) }) { opened, now ->
            now == opened
        }
    // The open chip stays while its panel is open, so editing the filter down to empty
    // does not take the panel away under the user.
    val chips = rowItems(root, isKnown = keyToSpec::containsKey, keepPath = openConditionPath ?: openGroupPath)
    if (chips.isEmpty()) return
    val texts =
        remember(keyToSpec, filterChips.unknownTitle, filterChips.unresolvedTitle, strings) {
            FilterChipTexts(keyToSpec, filterChips.unknownTitle, filterChips.unresolvedTitle, strings)
        }
    val listState: LazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Also shown outside a Table, so it reads the motion setting from the state, not the table's local.
    val motion = rememberTableMotionSpecs(state.settings.motion)
    // The arrows are auto-mirrored and scroll toward the start or the end; their labels name the physical side.
    val layoutDirection = LocalLayoutDirection.current
    val backwardLabel =
        if (layoutDirection.isPhysicallyLeft(towardStart = true)) {
            UiString.FilterChipsScrollLeft
        } else {
            UiString.FilterChipsScrollRight
        }
    val forwardLabel =
        if (layoutDirection.isPhysicallyLeft(towardStart = false)) {
            UiString.FilterChipsScrollLeft
        } else {
            UiString.FilterChipsScrollRight
        }
    // A removal shifts the paths after it, so nothing stays open; the focus goes to the chip now at that place,
    // else the one before it.
    val removeNode: (FilterPath) -> Unit = { path ->
        row.closeAll()
        state.updateFilterExpression { it.removeAt(path) }
        row.focusPath = chipAtRemoved(state.filterExpression, keyToSpec::containsKey, path)
    }
    FocusedChipInView(row, chips, listState)

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (includeClearAllChip) {
                InputChip(
                    selected = true,
                    onClick = {
                        row.closeAll()
                        clearAllFilters(state, keyToSpec, chips)
                    },
                    label = { Text(strings.get(UiString.FilterClearAll)) },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(TableIcons.Close),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.padding(vertical = 4.dp).padding(end = 4.dp),
                )
            }
            ChipsScrollButton(
                motion = motion,
                listState = listState,
                enabled = listState.canScrollBackward,
                icon = TableIcons.KeyboardArrowLeft,
                contentDescription = strings.get(backwardLabel),
                modifier = Modifier.padding(end = 4.dp),
                onClick = {
                    scope.launch(motion.scrollContext) {
                        val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(target)
                    }
                },
            )
            CompositionLocalProvider(LocalFilterChipColors provides filterChips.colors) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(chips, key = { it.path.single() }) { item ->
                        ReturnFocus(requested = item.path == row.focusPath, onReturn = { row.focusPath = null }) {
                            RowChip(
                                item = item,
                                row = row,
                                openConditionPath = openConditionPath,
                                openGroupPath = openGroupPath,
                                state = state,
                                keyToSpec = keyToSpec,
                                texts = texts,
                                filterChips = filterChips,
                                filterPanel = filterPanel,
                                groupPopover = groupPopover,
                                removeNode = removeNode,
                            )
                        }
                    }
                }
            }
            ChipsScrollButton(
                motion = motion,
                listState = listState,
                enabled = listState.canScrollForward,
                icon = TableIcons.KeyboardArrowRight,
                contentDescription = strings.get(forwardLabel),
                modifier = Modifier.padding(start = 4.dp),
                onClick = {
                    scope.launch(motion.scrollContext) {
                        val lastIndex = (chips.size - 1).coerceAtLeast(0)
                        val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(lastIndex)
                        listState.animateScrollToItem(target)
                    }
                },
            )
        }
        if (state.unresolvedFilters.isNotEmpty()) UnresolvedFiltersLine(strings)
        footer()
    }
}

/** Says that the filter is not applied while it has unresolved conditions. */
@Composable
private fun UnresolvedFiltersLine(strings: StringProvider) {
    val color = MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            painter = painterResource(TableIcons.Warning),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = strings.get(UiString.FilterUnresolvedBlocked),
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Which chip's panel or popover is open, and which chip gets the focus back. */
private class ChipRowState<C> {
    var openCondition by mutableStateOf<OpenNode<C>?>(null)
    var openGroup by mutableStateOf<OpenNode<C>?>(null)
    var focusPath by mutableStateOf<FilterPath?>(null)

    fun closeAll() {
        openCondition = null
        openGroup = null
    }

    /**
     * Closes the popover. Only a popover closed from the keyboard or by an edit gives the focus back to its
     * chip ([returnFocus]); after a tap elsewhere the focus stays with what was tapped.
     */
    fun closeGroup(returnFocus: Boolean) {
        if (returnFocus) focusPath = openGroup?.path
        openGroup = null
    }

    /** Follows an edit made in the popover, so that it does not read as the app replacing the group. */
    fun groupEdited(root: TableFilterNode.Group<C>) {
        val open = openGroup ?: return
        openGroup = root.nodeAt(open.path)?.let { open.copy(node = it) }
    }
}

/** An open panel or popover: the top-level [path] it belongs to and the [node] it was opened on. */
private data class OpenNode<C>(
    val path: FilterPath,
    val node: TableFilterNode<C>,
)

/**
 * [open]'s path while [isSame] says the node now at it is still the one opened; else null, and [onStale]
 * closes it, as when a tree set by the app moved or replaced the node.
 */
@Composable
private fun <C> validOpenPath(
    root: TableFilterNode.Group<C>,
    open: OpenNode<C>?,
    onStale: () -> Unit,
    isSame: (opened: TableFilterNode<C>, now: TableFilterNode<C>?) -> Boolean,
): FilterPath? {
    val path = open?.takeIf { isSame(it.node, root.nodeAt(it.path)) }?.path
    val currentOnStale by rememberUpdatedState(onStale)
    if (open != null && path == null) LaunchedEffect(open) { currentOnStale() }
    return path
}

/**
 * Scrolls the chip that is to get the focus into view, so that it composes and takes it. A chip that has not
 * taken it two frames later, e.g. one the app removed meanwhile, no longer gets it.
 */
@Composable
private fun <C> FocusedChipInView(
    row: ChipRowState<C>,
    chips: List<FilterChipItem<C>>,
    listState: LazyListState,
) {
    val currentChips by rememberUpdatedState(chips)
    val target = row.focusPath
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        val index = currentChips.indexOfFirst { it.path == target }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.scrollToItem(index)
        }
        repeat(2) { withFrameNanos {} }
        if (row.focusPath == target) row.focusPath = null
    }
}

/** One chip of the row: a condition, a group or an unknown key. */
@Composable
@Suppress("LongParameterList")
private fun <T : Any, C, E> RowChip(
    item: FilterChipItem<C>,
    row: ChipRowState<C>,
    openConditionPath: FilterPath?,
    openGroupPath: FilterPath?,
    state: TableState<C>,
    keyToSpec: Map<C, ColumnSpec<T, C, E>>,
    texts: FilterChipTexts<C>,
    filterChips: TableFilterChipStyle<C>,
    filterPanel: ActiveFilterPanel<T, C, E>?,
    groupPopover: ActiveGroupPopover<T, C, E>?,
    removeNode: (FilterPath) -> Unit,
) {
    when (item) {
        is FilterChipItem.Condition -> {
            val isOpen = item.path == openConditionPath
            val spec = keyToSpec.getValue(item.column)
            ConditionChip(
                spec = spec,
                item = item,
                isOpen = isOpen,
                canOpen = filterPanel != null && spec.filter != null,
                texts = texts,
                filterChips = filterChips,
                onOpen = {
                    row.openCondition = OpenNode(item.path, TableFilterNode.Condition(item.column, item.state))
                },
                // A null state removes the condition, as the header's panel does.
                onChange = { newState ->
                    if (newState == null) {
                        removeNode(item.path)
                    } else {
                        val condition = TableFilterNode.Condition(item.column, newState)
                        state.updateFilterExpression { it.replaceAt(item.path, condition) }
                    }
                },
                onClear = {
                    if (isOpen) row.openCondition = null
                    state.updateFilterExpression { it.replaceAt(item.path, resetCondition(item.column, spec)) }
                },
            ) { onChange ->
                filterPanel?.invoke(spec, item.state, onChange) { row.openCondition = null }
            }
        }

        is FilterChipItem.Group -> {
            val node = state.filterExpression.children[item.path.single()]
            GroupChip(
                item = item,
                node = node,
                isOpen = item.path == openGroupPath,
                texts = texts,
                filterChips = filterChips,
                onOpen = {
                    row.openCondition = null
                    row.openGroup = OpenNode(item.path, node)
                },
                onClear = { removeNode(item.path) },
                popover =
                    groupPopover?.let { popover ->
                        {
                            popover(item.path, keyToSpec, { row.groupEdited(state.filterExpression) }, row::closeGroup)
                        }
                    },
            )
        }

        is FilterChipItem.Unresolved -> {
            FilterChipSlot(
                TableFilterChipNode.Unresolved(
                    key = item.node.key,
                    payload = item.node.payload,
                    title = texts.unresolvedTitle(item.node),
                    placement = TableFilterChipPlacement.Row,
                    strings = texts.strings,
                    onClear = { removeNode(item.path) },
                ),
                spec = null,
                filterChips = filterChips,
            )
        }

        is FilterChipItem.Unknown -> {
            FilterChipSlot(
                TableFilterChipNode.Unknown(
                    column = item.column,
                    state = item.state,
                    title = texts.title(item.column),
                    placement = TableFilterChipPlacement.Row,
                    strings = texts.strings,
                    onClear = { removeNode(item.path) },
                ),
                spec = null,
                filterChips = filterChips,
            )
        }
    }
}

/** The chip now at the top-level [removed] path, else the last chip before it; null when the row is empty. */
private fun <C> chipAtRemoved(
    root: TableFilterNode.Group<C>,
    isKnown: (C) -> Boolean,
    removed: FilterPath,
): FilterPath? {
    val items = rowItems(root, isKnown, keepPath = null)
    return (items.firstOrNull { it.path.single() >= removed.single() } ?: items.lastOrNull())?.path
}

/** An arrow that scrolls the chips row; shown only while the row overflows. */
@Composable
private fun RowScope.ChipsScrollButton(
    motion: TableMotionSpecs,
    listState: LazyListState,
    enabled: Boolean,
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = listState.canScrollBackward || listState.canScrollForward,
        enter = motion.enter { fadeIn(motion.fade()) + expandHorizontally(motion.move(IntSize.VisibilityThreshold)) },
        exit = motion.exit { fadeOut(motion.fade()) + shrinkHorizontally(motion.move(IntSize.VisibilityThreshold)) },
    ) {
        IconButton(enabled = enabled, onClick = onClick, modifier = modifier) {
            Icon(painter = painterResource(icon), contentDescription = contentDescription)
        }
    }
}

/** [node] drawn by [ResolvedFilterChip], with the row's spacing. */
@Composable
private fun <C> FilterChipSlot(
    node: TableFilterChipNode<C>,
    spec: ColumnSpec<*, C, *>?,
    filterChips: TableFilterChipStyle<C>,
) {
    Box(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) { ResolvedFilterChip(node, spec, filterChips) }
}

/**
 * Draws [node] in the row or in a popover: a condition by its column [spec]'s deprecated
 * [ColumnSpec.activeFilterChip] when it has one; else [TableFilterChipStyle.chip], else [DefaultFilterChip].
 */
@Composable
internal fun <C> ResolvedFilterChip(
    node: TableFilterChipNode<C>,
    spec: ColumnSpec<*, C, *>?,
    style: TableFilterChipStyle<C>,
) {
    @Suppress("DEPRECATION")
    val legacyChip = spec?.activeFilterChip
    val chip = style.chip
    when {
        node is TableFilterChipNode.Condition && legacyChip != null -> legacyChip(node.toChipContext())
        chip != null -> chip(node)
        else -> DefaultFilterChip(node)
    }
}

/**
 * A top-level condition's chip: [onOpen] opens the panel when [canOpen], [onChange] sets the condition,
 * [onClear] resets it. While [isOpen], [panel] is anchored under the chip, and the chip stays with only the
 * column title if the condition no longer describes a value. A column without a filter type describes no
 * value either; its chip always shows the title alone.
 */
@Composable
@Suppress("LongParameterList")
private fun <T : Any, C, E> ConditionChip(
    spec: ColumnSpec<T, C, E>,
    item: FilterChipItem.Condition<C>,
    isOpen: Boolean,
    canOpen: Boolean,
    texts: FilterChipTexts<C>,
    filterChips: TableFilterChipStyle<C>,
    onOpen: () -> Unit,
    onChange: (TableFilterState<*>?) -> Unit,
    onClear: () -> Unit,
    panel: @Composable (onChange: (TableFilterState<*>?) -> Unit) -> Unit,
) {
    val text = texts.leafText(item.column, item.state)
    if (text == null && !isOpen && spec.filter != null) return
    val title = texts.title(item.column)
    // The panel is anchored outside the slot, so a custom chip gets it by calling onOpen.
    Box {
        FilterChipSlot(
            TableFilterChipNode.Condition(
                column = item.column,
                state = item.state,
                title = title,
                text = text,
                isOpen = isOpen,
                canOpen = canOpen,
                placement = TableFilterChipPlacement.Row,
                strings = texts.strings,
                onOpen = onOpen,
                onChange = onChange,
                onClear = onClear,
                content = spec.filterChipContent,
            ),
            spec,
            filterChips,
        )
        if (isOpen) panel(onChange)
    }
}

/** A top-level group's chip; while [isOpen], [popover] is anchored under it. */
@Composable
@Suppress("LongParameterList")
private fun <C> GroupChip(
    item: FilterChipItem.Group<C>,
    node: TableFilterNode<C>,
    isOpen: Boolean,
    texts: FilterChipTexts<C>,
    filterChips: TableFilterChipStyle<C>,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    popover: (@Composable () -> Unit)?,
) {
    Box {
        FilterChipSlot(
            TableFilterChipNode.Group(
                isGroup = item.summary.isGroup,
                operator = item.summary.operator,
                negated = item.summary.negated,
                itemCount = item.summary.activeCount,
                columnTitles = texts.titles(item.summary),
                sharedColumn = item.summary.sharedColumn,
                text = texts.groupText(node),
                description = texts.describe(node),
                hasUnresolved = item.summary.unresolved.isNotEmpty(),
                isOpen = isOpen,
                canOpen = popover != null,
                onOpen = onOpen,
                placement = TableFilterChipPlacement.Row,
                strings = texts.strings,
                onClear = onClear,
            ),
            spec = null,
            filterChips = filterChips,
        )
        if (isOpen && popover != null) popover()
    }
}

/** Moves the focus into [content] when [requested] turns true, after calling [onReturn]. */
@Composable
internal fun ReturnFocus(
    requested: Boolean,
    onReturn: () -> Unit,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnReturn by rememberUpdatedState(onReturn)
    LaunchedEffect(requested) {
        if (requested) {
            currentOnReturn()
            focusRequester.requestFocus()
        }
    }
    Box(Modifier.focusRequester(focusRequester)) { content() }
}

/** Titles, texts and accessibility sentences of filter chips. */
internal class FilterChipTexts<C>(
    private val keyToSpec: Map<C, ColumnSpec<*, C, *>>,
    private val unknownTitle: ((C) -> String?)?,
    private val unresolvedTitle: ((key: String, payload: Any?) -> String?)?,
    val strings: StringProvider,
) {
    /** An unresolved condition's title: the app's title for it, else its key. */
    fun unresolvedTitle(node: TableFilterNode.Unresolved): String =
        unresolvedTitle?.invoke(node.key, node.payload) ?: node.key

    /** The titles of [summary]'s columns, then of its unresolved leaves. */
    @Composable
    fun titles(summary: FilterGroupSummary<C>): List<String> =
        summary.columns.map { title(it) } + summary.unresolved.map { unresolvedTitle(it) }

    /** The column's plain-text title; for an unknown column the app's title for it, else the key. */
    @Composable
    fun title(column: C): String {
        val spec = keyToSpec[column]
        val title = if (spec != null) spec.title?.invoke() else unknownTitle?.invoke(column)
        return title ?: column.toString()
    }

    /** The filter described for a chip, e.g. `Contains "x"`; null when it describes no value. */
    @Composable
    fun leafText(
        column: C,
        state: TableFilterState<*>,
    ): String? = keyToSpec[column]?.filter?.let { buildFilterChipTextUnsafe(it, state, strings) }

    /**
     * A group chip's text: `"Status: Active or Pending"` for an OR on one column, else the distinct
     * titles of the columns under [node].
     */
    @Composable
    fun groupText(node: TableFilterNode<C>): String {
        val summary = node.summary()
        val shared = summary.sharedColumn
        if (shared != null && node is TableFilterNode.Group) {
            val leafTexts =
                node.children.map { child ->
                    val condition = child as TableFilterNode.Condition
                    leafText(condition.column, condition.state)
                }
            if (leafTexts.all { it != null }) {
                return "${title(shared)}: " + leafTexts.joinToString(" ${strings.get(UiString.FilterOr)} ")
            }
        }
        return columnTitles(node)
    }

    /** [node] read as a sentence; inactive children are left out. */
    @Composable
    fun describe(node: TableFilterNode<C>): String =
        when (node) {
            is TableFilterNode.Condition -> {
                val text = leafText(node.column, node.state)
                if (text != null) strings.filterConditionDescription(title(node.column), text) else title(node.column)
            }

            is TableFilterNode.Unresolved -> {
                strings.unresolvedFilterDescription(unresolvedTitle(node))
            }

            is TableFilterNode.Group, is TableFilterNode.Not -> {
                var inner: TableFilterNode<C> = node
                var negated = false
                while (inner is TableFilterNode.Not) {
                    negated = !negated
                    inner = inner.child
                }
                when {
                    inner is TableFilterNode.Group -> {
                        val parts = inner.children.filter { it.isActive() }.map { describe(it) }
                        // A group kept open with nothing active in it yet reads as the columns it is on.
                        if (parts.isEmpty()) {
                            columnTitles(inner)
                        } else {
                            strings.filterGroupDescription(inner.operator, negated, parts)
                        }
                    }

                    negated -> {
                        strings.filterGroupDescription(TableFilterNode.Operator.AND, true, listOf(describe(inner)))
                    }

                    else -> {
                        describe(inner)
                    }
                }
            }
        }

    @Composable
    private fun columnTitles(node: TableFilterNode<C>): String = titles(node.summary()).joinToString(", ")
}

/** [column]'s condition reset to its first constraint with no value. */
private fun <C> resetCondition(
    column: C,
    spec: ColumnSpec<*, C, *>?,
): TableFilterNode.Condition<C> {
    val defaultConstraint = spec?.filter?.constraints?.firstOrNull() ?: FilterConstraint.EQUALS
    return TableFilterNode.Condition(column, TableFilterState<Any?>(defaultConstraint, null))
}

/**
 * Resets the [shown] top-level conditions as before the filter tree and removes the shown ones on unknown
 * columns; top-level conditions that are not shown stay as they are. Removes every group or not.
 */
private fun <C> clearAllFilters(
    state: TableState<C>,
    keyToSpec: Map<C, ColumnSpec<*, C, *>>,
    shown: List<FilterChipItem<C>>,
) {
    val shownPaths = shown.mapTo(HashSet()) { it.path.single() }
    state.updateFilterExpression { root ->
        root.copy(
            children =
                root.children.mapIndexedNotNull { index, node ->
                    when {
                        node !is TableFilterNode.Condition || index !in shownPaths -> node
                        node.column !in keyToSpec -> null
                        else -> resetCondition(node.column, keyToSpec[node.column])
                    }
                },
        )
    }
    state.clearComplexFilters()
}

@Composable
@Suppress("UNCHECKED_CAST", "CyclomaticComplexMethod", "ReturnCount")
internal fun buildFilterChipTextUnsafe(
    filterType: TableFilterType<*>,
    state: TableFilterState<*>,
    strings: StringProvider,
): String? {
    val constraint = state.constraint ?: return null
    if (constraint.isNullCheck()) {
        return strings.get(constraint.toUiString())
    }
    return when (filterType) {
        is TableFilterType.TextTableFilter -> {
            val s = state as? TableFilterState<String> ?: return null
            val value = s.values?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            "${strings.get(constraint.toUiString())} \"$value\""
        }

        is TableFilterType.NumberTableFilter<*> -> {
            val s = state as? TableFilterState<Number> ?: return null
            val list = s.values ?: emptyList()
            if (constraint == FilterConstraint.BETWEEN && list.size >= 2) {
                val from = list[0]
                val to = list[1]
                "${strings.get(constraint.toUiString())} ${strings.formatNumber(from)} – ${strings.formatNumber(to)}"
            } else {
                val single = list.firstOrNull() ?: return null
                "${strings.get(constraint.toUiString())} ${strings.formatNumber(single)}"
            }
        }

        is TableFilterType.BooleanTableFilter -> {
            val s = state as? TableFilterState<Boolean> ?: return null
            val value = s.values?.firstOrNull() ?: return null
            val valueTitle =
                if (value) strings.get(UiString.BooleanTrueTitle) else strings.get(UiString.BooleanFalseTitle)
            "${strings.get(FilterConstraint.EQUALS.toUiString())} $valueTitle"
        }

        is TableFilterType.DateTableFilter -> {
            val s = state as? TableFilterState<LocalDate> ?: return null
            val list = s.values ?: emptyList()
            if (constraint == FilterConstraint.BETWEEN && list.size >= 2) {
                "${strings.get(
                    constraint.toUiString(),
                )} ${strings.formatDate(list[0])} – ${strings.formatDate(list[1])}"
            } else {
                val value = list.firstOrNull() ?: return null
                "${strings.get(constraint.toUiString())} ${strings.formatDate(value)}"
            }
        }

        is TableFilterType.EnumTableFilter<*> -> {
            val list = state.values ?: return null
            when (constraint) {
                FilterConstraint.EQUALS -> {
                    val single = (list.singleOrNull() as? Enum<*>) ?: return null
                    val title = (filterType.getTitle as @Composable (Enum<*>) -> String).invoke(single)
                    "${strings.get(constraint.toUiString())} $title"
                }

                FilterConstraint.IN, FilterConstraint.NOT_IN -> {
                    if (list.isEmpty()) return null
                    val titles =
                        list
                            .mapNotNull { it as? Enum<*> }
                            .map { (filterType.getTitle as @Composable (Enum<*>) -> String).invoke(it) }
                    val joined = titles.joinToString(", ")
                    "${strings.get(constraint.toUiString())} $joined"
                }

                else -> {
                    null
                }
            }
        }

        is TableFilterType.CustomTableFilter<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val customFilter = filterType as TableFilterType.CustomTableFilter<Any?, Any?>
            val s = state as? TableFilterState<Any?>
            customFilter.stateProvider.buildChipText(s)
        }

        TableFilterType.DisabledTableFilter -> {
            null
        }
    }
}
