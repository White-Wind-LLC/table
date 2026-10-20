package ua.wwind.table.component.filterchips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.ColumnSpec
import ua.wwind.table.DefaultFilterChip
import ua.wwind.table.FilterChipTexts
import ua.wwind.table.TableFilterChipNode
import ua.wwind.table.TableFilterChipPlacement
import ua.wwind.table.TableFilterChipStyle
import ua.wwind.table.filter.data.FilterPath
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Condition
import ua.wwind.table.filter.data.TableFilterNode.Group
import ua.wwind.table.filter.data.TableFilterNode.Not
import ua.wwind.table.filter.data.TableFilterNode.Operator
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
 * lines switch Any/All, toggle NOT and remove nodes. Calls [onDismiss] when it is closed and when an edit
 * leaves no group or not at [rootPath].
 */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> FilterGroupPopover(
    rootPath: FilterPath,
    state: TableState<C>,
    keyToSpec: Map<C, ColumnSpec<T, C, E>>,
    @Suppress("UnusedParameter") tableData: E,
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
    val tree = PopoverTree(style, texts, edit)
    val firstControl = remember { FocusRequester() }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 8.dp),
        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
        properties = PopupProperties(focusable = true, dismissOnBackPress = false),
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Escape && event.key != Key.Back) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyUp) onDismiss()
                        true
                    },
        ) {
            tree.Line(rootPath, node, Modifier.focusRequester(firstControl), isTop = true)
            // Keys reach the popover only through a focused control inside it.
            LaunchedEffect(Unit) { firstControl.requestFocus() }
        }
    }
}

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
    private val style: TableFilterChipStyle<C>,
    private val texts: FilterChipTexts<C>,
    private val edit: (transform: (Group<C>) -> Group<C>) -> Unit,
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
        val node =
            if (texts.isKnown(column)) {
                TableFilterChipNode.Condition(
                    column = column,
                    state = condition.state,
                    title = texts.title(column),
                    text = texts.leafText(column, condition.state),
                    isOpen = false,
                    placement = TableFilterChipPlacement.Tree,
                    strings = strings,
                    onOpen = {},
                    onChange = { new -> edit { it.replaceAt(path, Condition(column, new)) } },
                    onClear = remove,
                    content = null,
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
        val chip = style.chip
        if (chip != null) chip(node) else DefaultFilterChip(node)
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
