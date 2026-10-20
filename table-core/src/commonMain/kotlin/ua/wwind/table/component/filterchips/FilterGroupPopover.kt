package ua.wwind.table.component.filterchips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.ColumnSpec
import ua.wwind.table.FilterChipTexts
import ua.wwind.table.ResolvedFilterChip
import ua.wwind.table.ReturnFocus
import ua.wwind.table.TableFilterChipNode
import ua.wwind.table.TableFilterChipPlacement
import ua.wwind.table.TableFilterChipStyle
import ua.wwind.table.filter.component.main.FilterPanelContent
import ua.wwind.table.filter.data.FilterPath
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.nodeAt
import ua.wwind.table.filter.data.removeAt
import ua.wwind.table.filter.data.replaceAt
import ua.wwind.table.filter.data.setOperatorAt
import ua.wwind.table.filter.data.toggleNotAt
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.state.TableState
import ua.wwind.table.state.updateFilterExpression
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

private val TreeIndent = 12.dp
private val TreeBorderWidth = 2.dp

/**
 * The popover of the group or not at [rootPath], anchored under its chip: the node as an indented tree whose
 * lines switch Any/All, toggle NOT and remove nodes, and whose condition chips drill into the condition's
 * filter panel. Calls [onDismiss] when it is closed and when an edit leaves no group or not at [rootPath].
 */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> FilterGroupPopover(
    rootPath: FilterPath,
    state: TableState<C>,
    keyToSpec: Map<C, ColumnSpec<T, C, E>>,
    tableData: E,
    style: TableFilterChipStyle<C>,
    strings: StringProvider,
    onDismiss: () -> Unit,
) {
    val node = state.filterExpression.nodeAt(rootPath)
    if (node !is Group && node !is Not) return
    val texts = FilterChipTexts(keyToSpec, style.unknownTitle, strings)
    val edit: (transform: (Group<C>) -> Group<C>) -> Unit = { transform ->
        val before = state.filterExpression
        state.updateFilterExpression(transform)
        val after = state.filterExpression
        if (!after.isGroupLikeAt(rootPath) || after.siblingCount(rootPath) < before.siblingCount(rootPath)) {
            onDismiss()
        }
    }
    val drill = remember { PopoverDrill() }
    val drilled =
        drill.path?.let { path ->
            (state.filterExpression.nodeAt(path) as? Condition)?.takeIf { texts.isKnown(it.column) }
        }
    if (drill.path != null && drilled == null) LaunchedEffect(drill.path) { drill.path = null }
    val back = { drill.leave(state.filterExpression) { path -> edit { it.removeAt(path) } } }
    val dismiss = {
        drill.leave(state.filterExpression) { path -> state.updateFilterExpression { it.removeAt(path) } }
        onDismiss()
    }
    val tree = PopoverTree(keyToSpec, style, texts, edit, drill)
    DropdownMenu(
        expanded = true,
        onDismissRequest = dismiss,
        offset = DpOffset(0.dp, 8.dp),
        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
        properties = PopupProperties(focusable = true, dismissOnBackPress = false),
    ) {
        Column(
            Modifier.onPreviewKeyEvent { event ->
                if (event.key != Key.Escape && event.key != Key.Back) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyUp) {
                    if (drilled != null) back() else onDismiss()
                }
                true
            },
        ) {
            val path = drill.path
            if (path != null && drilled != null) {
                DrillIn(
                    title = texts.title(drilled.column),
                    trail = drillTrail(state.filterExpression, rootPath, path, strings),
                    strings = strings,
                    onBack = back,
                ) {
                    ConditionPanel(drilled, keyToSpec.getValue(drilled.column), state, tableData, strings, back) {
                        edit { root -> root.replaceAt(path, it) }
                    }
                }
            } else {
                TreeView(tree, rootPath, node, focusFirst = drill.focusLeaf == null)
            }
        }
    }
}

/** Which condition's panel the popover shows instead of the tree, and which leaf chip gets the focus back. */
private class PopoverDrill {
    var path by mutableStateOf<FilterPath?>(null)
    var focusLeaf by mutableStateOf<FilterPath?>(null)
}

/** Shows the tree again; the condition, kept while its panel was shown, goes to [remove] if it is empty. */
private fun <C> PopoverDrill.leave(
    root: Group<C>,
    remove: (FilterPath) -> Unit,
) {
    val leaf = path ?: return
    path = null
    if ((root.nodeAt(leaf) as? Condition)?.state?.isActive() == false) remove(leaf) else focusLeaf = leaf
}

@Composable
private fun <C> TreeView(
    tree: PopoverTree<C>,
    rootPath: FilterPath,
    node: TableFilterNode<C>,
    focusFirst: Boolean,
) {
    val firstControl = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        tree.Line(rootPath, node, Modifier.focusRequester(firstControl), isTop = true)
        // Keys reach the popover only through a focused control inside it.
        LaunchedEffect(Unit) { if (focusFirst) firstControl.requestFocus() }
    }
}

/** A condition's panel under a header with a back button, the column [title] and the [trail] above it. */
@Composable
private fun DrillIn(
    title: String,
    trail: String,
    strings: StringProvider,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val backButton = remember { FocusRequester() }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.focusRequester(backButton)) {
            Icon(
                painter = painterResource(TableIcons.KeyboardArrowLeft),
                contentDescription = strings.get(UiString.FilterBack),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = trail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
    content()
    // Keys reach the popover only through a focused control inside it.
    LaunchedEffect(Unit) { backButton.requestFocus() }
}

/** [condition]'s filter panel; clearing it sets an empty condition, so the leaf keeps its place. */
@Composable
@Suppress("LongParameterList")
private fun <C, E> ConditionPanel(
    condition: Condition<C>,
    spec: ColumnSpec<*, C, E>,
    state: TableState<C>,
    tableData: E,
    strings: StringProvider,
    onClose: () -> Unit,
    onChange: (Condition<C>) -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    FilterPanelContent(
        type = spec.filter as? TableFilterType<Any?>,
        state = condition.state as TableFilterState<Any?>,
        tableData = tableData,
        onClose = onClose,
        strings = strings,
        autoApplyFilters = state.settings.autoApplyFilters,
        autoFilterDebounce = state.settings.autoFilterDebounce,
        onChange = { new -> onChange(Condition(condition.column, new ?: TableFilterState<Any?>(null, null))) },
    )
}

/** The operator words of the groups from [rootPath] down to the leaf at [leafPath], e.g. `Any › All`. */
@Composable
private fun <C> drillTrail(
    root: Group<C>,
    rootPath: FilterPath,
    leafPath: FilterPath,
    strings: StringProvider,
): String =
    (rootPath.size until leafPath.size)
        .mapNotNull { depth -> (root.nodeAt(leafPath.take(depth)) as? Group)?.operator }
        .map { operator ->
            strings.get(if (operator == Operator.OR) UiString.FilterOperatorAny else UiString.FilterOperatorAll)
        }.joinToString(" › ")

/** Whether a group or a not is at [path]. */
internal fun <C> Group<C>.isGroupLikeAt(path: FilterPath): Boolean = nodeAt(path).let { it is Group || it is Not }

/** Children of [path]'s parent; drops when an edit removed the node at [path]. */
private fun <C> Group<C>.siblingCount(path: FilterPath): Int =
    when (val parent = nodeAt(path.dropLast(1))) {
        is Group -> parent.children.size
        is Not -> 1
        is Condition, null -> 0
    }

private class PopoverTree<C>(
    private val keyToSpec: Map<C, ColumnSpec<*, C, *>>,
    private val style: TableFilterChipStyle<C>,
    private val texts: FilterChipTexts<C>,
    private val edit: (transform: (Group<C>) -> Group<C>) -> Unit,
    private val drill: PopoverDrill,
) {
    private val strings get() = texts.strings

    /** The line of [node] at [path]; [isTop] lines have no ×, the row chip has it. */
    @Composable
    fun Line(
        path: FilterPath,
        node: TableFilterNode<C>,
        modifier: Modifier = Modifier,
        isTop: Boolean = false,
    ) {
        Column(modifier) { LineContent(path, node, isTop) }
    }

    @Composable
    private fun LineContent(
        path: FilterPath,
        node: TableFilterNode<C>,
        isTop: Boolean,
    ) {
        when (node) {
            is Condition -> {
                Leaf(path, node)
            }

            is Group -> {
                GroupLine(path, path, node, negated = false, isTop)
            }

            is Not -> {
                when (val child = node.child) {
                    is Group -> {
                        GroupLine(path, path + 0, child, negated = true, isTop)
                    }

                    is Condition -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NotToggle(path, negated = true)
                            Leaf(path + 0, child)
                        }
                    }

                    is Not -> {
                        NotToggle(path, negated = true)
                        Children { Line(path + 0, child) }
                    }
                }
            }
        }
    }

    /** A group's line at [linePath] (the not when [negated]) with its children under it. */
    @Composable
    private fun GroupLine(
        linePath: FilterPath,
        groupPath: FilterPath,
        group: Group<C>,
        negated: Boolean,
        isTop: Boolean,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OperatorSwitch(group.operator) { op -> edit { it.setOperatorAt(groupPath, op) } }
                NotToggle(linePath, negated)
                if (!isTop) {
                    val columns = group.summary().columns.map { texts.title(it) }
                    IconButton(onClick = { edit { it.removeAt(linePath) } }) {
                        Icon(
                            painter = painterResource(TableIcons.Close),
                            contentDescription = strings.removeFilterGroupLabel(columns.joinToString()),
                        )
                    }
                }
            }
            Children {
                group.children.forEachIndexed { index, child -> Line(groupPath + index, child) }
            }
        }
    }

    @Composable
    private fun OperatorSwitch(
        operator: Operator,
        onSelect: (Operator) -> Unit,
    ) {
        val options = listOf(Operator.OR to UiString.FilterOperatorAny, Operator.AND to UiString.FilterOperatorAll)
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = operator == option,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.semantics { role = Role.RadioButton },
                    label = { Text(strings.get(label)) },
                )
            }
        }
    }

    @Composable
    private fun NotToggle(
        path: FilterPath,
        negated: Boolean,
    ) {
        FilterChip(
            selected = negated,
            onClick = { edit { it.toggleNotAt(path) } },
            label = { Text(strings.get(UiString.FilterNot)) },
        )
    }

    @Composable
    private fun Leaf(
        path: FilterPath,
        condition: Condition<C>,
    ) {
        val column = condition.column
        val remove = { edit { it.removeAt(path) } }
        val spec = keyToSpec[column]
        val node =
            if (spec != null) {
                TableFilterChipNode.Condition(
                    column = column,
                    state = condition.state,
                    title = texts.title(column),
                    text = texts.leafText(column, condition.state),
                    isOpen = false,
                    placement = TableFilterChipPlacement.Tree,
                    strings = strings,
                    onOpen = { drill.path = path },
                    onChange = { new -> edit { it.replaceAt(path, Condition(column, new)) } },
                    onClear = remove,
                    content = spec.filterChipContent,
                )
            } else {
                TableFilterChipNode.Unknown(
                    column = column,
                    state = condition.state,
                    title = texts.title(column),
                    placement = TableFilterChipPlacement.Tree,
                    strings = strings,
                    onClear = remove,
                )
            }
        ReturnFocus(requested = path == drill.focusLeaf, onReturn = { drill.focusLeaf = null }) {
            ResolvedFilterChip(node, spec, style)
        }
    }

    /** Lines one level deeper: indented, with a border along their start. */
    @Composable
    private fun Children(content: @Composable () -> Unit) {
        val border = MaterialTheme.colorScheme.outlineVariant
        Column(Modifier.treeBorder(border).padding(start = TreeIndent)) { content() }
    }
}

private fun Modifier.treeBorder(color: Color): Modifier =
    drawBehind {
        val width = TreeBorderWidth.toPx()
        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width / 2 else width / 2
        drawLine(color, Offset(x, 0f), Offset(x, size.height), width)
    }
