package ua.wwind.table

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
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
import ua.wwind.table.component.filterchips.isGroupLikeAt
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
        groupPopover = { path, keyToSpec, onDismiss ->
            FilterGroupPopover(path, state, keyToSpec, tableData, filterChips, strings, onDismiss)
        },
        filterChips = filterChips,
        modifier = modifier,
    )
}

/**
 * Renders a compact header row with one chip per top-level node of the filter tree.
 * Without table data the chips cannot open filter panels; their × removes the filter.
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

/** Renders the popover of the group or not at `rootPath`, a top-level path. */
internal typealias ActiveGroupPopover<T, C, E> = @Composable (
    rootPath: FilterPath,
    keyToSpec: Map<C, ColumnSpec<T, C, E>>,
    onDismiss: () -> Unit,
) -> Unit

/**
 * The chips row, one chip per top-level node of [TableState.filterExpression]. [filterPanel] renders a
 * condition's filter panel under its chip, [groupPopover] a group's popover; null makes those clicks do
 * nothing. [footer] is shown under the row while the row is shown.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
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
    var openPath by remember { mutableStateOf<FilterPath?>(null) }
    var openGroupPath by remember { mutableStateOf<FilterPath?>(null) }
    // The chip that gets the focus back when its popover closes.
    var focusPath by remember { mutableStateOf<FilterPath?>(null) }
    val closeGroup = {
        focusPath = openGroupPath
        openGroupPath = null
    }
    val openGroup = validOpenGroup(state, openGroupPath, onStale = closeGroup)
    // The open chip stays while its panel is open, so editing the filter down to empty
    // does not take the panel away under the user.
    val items =
        rowItems(state.filterExpression, isKnown = keyToSpec::containsKey, keepPath = openPath ?: openGroup)
    if (items.isEmpty()) return
    val texts = FilterChipTexts(keyToSpec, filterChips.unknownTitle, strings)
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
    // A removal shifts the paths after it, so nothing stays open.
    val removeNode: (FilterPath) -> Unit = { path ->
        openPath = null
        openGroupPath = null
        state.updateFilterExpression { it.removeAt(path) }
    }

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (includeClearAllChip) {
                InputChip(
                    selected = true,
                    onClick = {
                        openPath = null
                        openGroupPath = null
                        clearAllFilters(state, keyToSpec, items)
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
                    items.forEach { item ->
                        item {
                            ReturnFocus(requested = item.path == focusPath, onReturn = { focusPath = null }) {
                                when (item) {
                                    is FilterChipItem.Condition -> {
                                        val isOpen = item.path == openPath
                                        val spec = keyToSpec.getValue(item.column)
                                        ConditionChip(
                                            spec = spec,
                                            item = item,
                                            isOpen = isOpen,
                                            texts = texts,
                                            filterChips = filterChips,
                                            onOpen = { if (filterPanel != null) openPath = item.path },
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
                                                if (isOpen) openPath = null
                                                state.updateFilterExpression {
                                                    it.replaceAt(item.path, resetCondition(item.column, spec))
                                                }
                                            },
                                        ) { onChange ->
                                            filterPanel?.invoke(spec, item.state, onChange) { openPath = null }
                                        }
                                    }

                                    is FilterChipItem.Group -> {
                                        GroupChip(
                                            item = item,
                                            node = state.filterExpression.children[item.path.single()],
                                            isOpen = item.path == openGroup,
                                            texts = texts,
                                            filterChips = filterChips,
                                            onOpen = {
                                                openPath = null
                                                openGroupPath = item.path
                                            },
                                            onClear = { removeNode(item.path) },
                                            popover =
                                                groupPopover?.let { popover ->
                                                    { popover(item.path, keyToSpec, closeGroup) }
                                                },
                                        )
                                    }

                                    is FilterChipItem.Unknown -> {
                                        FilterChipSlot(
                                            TableFilterChipNode.Unknown(
                                                column = item.column,
                                                state = item.state,
                                                title = texts.title(item.column),
                                                placement = TableFilterChipPlacement.Row,
                                                strings = strings,
                                                onClear = { removeNode(item.path) },
                                            ),
                                            filterChips,
                                        )
                                    }
                                }
                            }
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
                        val lastIndex = (items.size - 1).coerceAtLeast(0)
                        val target = (listState.firstVisibleItemIndex + 1).coerceAtMost(lastIndex)
                        listState.animateScrollToItem(target)
                    }
                },
            )
        }
        footer()
    }
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

/** [node] drawn by [TableFilterChipStyle.chip] or [DefaultFilterChip], with the row's spacing. */
@Composable
private fun <C> FilterChipSlot(
    node: TableFilterChipNode<C>,
    filterChips: TableFilterChipStyle<C>,
) {
    Box(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        val chip = filterChips.chip
        if (chip != null) chip(node) else DefaultFilterChip(node)
    }
}

/**
 * A top-level condition's chip: [onOpen] opens the panel, [onChange] sets the condition, [onClear] resets
 * it. While [isOpen], [panel] is anchored under the chip, and the chip stays with only the column title if
 * the condition no longer describes a value.
 */
@Composable
@Suppress("LongParameterList")
private fun <T : Any, C, E> ConditionChip(
    spec: ColumnSpec<T, C, E>,
    item: FilterChipItem.Condition<C>,
    isOpen: Boolean,
    texts: FilterChipTexts<C>,
    filterChips: TableFilterChipStyle<C>,
    onOpen: () -> Unit,
    onChange: (TableFilterState<*>?) -> Unit,
    onClear: () -> Unit,
    panel: @Composable (onChange: (TableFilterState<*>?) -> Unit) -> Unit,
) {
    val text = texts.leafText(item.column, item.state)
    if (text == null && !isOpen) return
    val title = texts.title(item.column)
    // The panel is anchored outside the slot, so a custom chip gets it by calling onOpen.
    Box {
        val legacyChip = spec.activeFilterChip
        if (legacyChip != null) {
            val ctx =
                TableFilterChipContext(
                    column = item.column,
                    state = item.state,
                    title = title,
                    text = text,
                    isOpen = isOpen,
                    strings = texts.strings,
                    onOpen = onOpen,
                    onChange = onChange,
                    onClear = onClear,
                )
            Box(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) { legacyChip(ctx) }
        } else {
            FilterChipSlot(
                TableFilterChipNode.Condition(
                    column = item.column,
                    state = item.state,
                    title = title,
                    text = text,
                    isOpen = isOpen,
                    placement = TableFilterChipPlacement.Row,
                    strings = texts.strings,
                    onOpen = onOpen,
                    onChange = onChange,
                    onClear = onClear,
                    content = null,
                ),
                filterChips,
            )
        }
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
                operator = item.summary.operator,
                negated = item.summary.negated,
                itemCount = item.summary.activeCount,
                columnTitles = item.summary.columns.map { texts.title(it) },
                sharedColumn = item.summary.sharedColumn,
                text = texts.groupText(node),
                description = texts.describe(node),
                isOpen = isOpen,
                onOpen = if (popover != null) onOpen else ({}),
                placement = TableFilterChipPlacement.Row,
                strings = texts.strings,
                onClear = onClear,
            ),
            filterChips,
        )
        if (isOpen && popover != null) popover()
    }
}

/**
 * [openGroupPath] while a group or not is there; else null, and [onStale] closes it, as when a tree set by
 * the app leaves no group at the open path.
 */
@Composable
private fun <C> validOpenGroup(
    state: TableState<C>,
    openGroupPath: FilterPath?,
    onStale: () -> Unit,
): FilterPath? {
    val open = openGroupPath?.takeIf { state.filterExpression.isGroupLikeAt(it) }
    val currentOnStale by rememberUpdatedState(onStale)
    if (openGroupPath != null && open == null) LaunchedEffect(openGroupPath) { currentOnStale() }
    return open
}

/** Moves the focus into [content] when [requested] turns true, after calling [onReturn]. */
@Composable
private fun ReturnFocus(
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
    val strings: StringProvider,
) {
    fun isKnown(column: C): Boolean = column in keyToSpec

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
        return summary.columns.map { title(it) }.joinToString(", ")
    }

    /** [node] read as a sentence; inactive children are left out. */
    @Composable
    fun describe(node: TableFilterNode<C>): String =
        when (node) {
            is TableFilterNode.Condition -> {
                val text = leafText(node.column, node.state)
                if (text != null) "${title(node.column)} $text" else title(node.column)
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
                        strings.filterGroupDescription(inner.operator, negated, parts)
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
 * Resets the shown top-level conditions as before the filter tree, removes conditions on unknown columns
 * and every group or not.
 */
private fun <C> clearAllFilters(
    state: TableState<C>,
    keyToSpec: Map<C, ColumnSpec<*, C, *>>,
    shown: List<FilterChipItem<C>>,
) {
    val reset = shown.filterIsInstance<FilterChipItem.Condition<C>>().mapTo(HashSet()) { it.path.single() }
    state.updateFilterExpression { root ->
        root.copy(
            children =
                root.children.mapIndexedNotNull { index, node ->
                    when {
                        node !is TableFilterNode.Condition -> node
                        node.column !in keyToSpec -> null
                        index in reset -> resetCondition(node.column, keyToSpec[node.column])
                        else -> node
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
