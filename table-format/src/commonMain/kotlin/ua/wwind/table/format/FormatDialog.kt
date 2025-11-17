package ua.wwind.table.format

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.format.component.FormatDialogTabRow
import ua.wwind.table.format.component.RuleTab
import ua.wwind.table.format.component.TabData
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

private data class EditFormatRule<E : Enum<E>, FILTER>(
    val index: Int,
    val item: TableFormatRule<E, FILTER>,
    val isNew: Boolean = false,
)

public data class FormatDialogSettings(
    val copiedItemHighlightDuration: Long = 3000,
    val copiedItemHighlightColor: Color = Color.Unspecified,
)

@OptIn(FlowPreview::class)
@Composable
public fun <E : Enum<E>, FILTER> FormatDialog(
    showDialog: Boolean,
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChanged: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    entries: ImmutableList<E>,
    key: Any,
    strings: StringProvider,
    onDismissRequest: () -> Unit,
    settings: FormatDialogSettings = FormatDialogSettings(),
) {
    if (!showDialog) return
    val lazyListState = rememberLazyListState()
    var itemCopyIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(itemCopyIndex) {
        itemCopyIndex?.let { index ->
            lazyListState.animateScrollToItem(index)
            delay(settings.copiedItemHighlightDuration)
            itemCopyIndex = null
        }
    }
    var editItem by remember { mutableStateOf<EditFormatRule<E, FILTER>?>(null) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = spacedBy(16.dp, Alignment.End),
            ) {
                editItem?.let { (index, item, isNew) ->
                    if (!isNew) {
                        IconButton(
                            onClick = {
                                onRulesChanged(
                                    rules.toPersistentList().mutate { list ->
                                        list.removeAt(index)
                                    }
                                )
                                editItem = null
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        IconButton(
                            onClick = {
                                val id = rules.maxByOrNull { it.id }?.id?.inc() ?: 0L
                                val lastIndex = rules.lastIndex
                                val itemCopy = item.copy(id = id)
                                onRulesChanged(
                                    rules.toPersistentList().mutate { list ->
                                        list.add(itemCopy)
                                    }
                                )
                                editItem = null
                                itemCopyIndex = lastIndex.inc()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            editItem = null
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            onRulesChanged(
                                rules.toPersistentList().mutate { list ->
                                    if (index in list.indices) {
                                        list[index] = item
                                    } else {
                                        list.add(item)
                                    }
                                }
                            )
                            editItem = null
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = "Save",
                        )
                    }
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = SpaceBetween,
            ) {
                Text(
                    text = strings.get(UiString.FormatRules),
                )
                if (editItem == null) {
                    IconButton(
                        onClick = {
                            val id = rules.maxByOrNull { it.id }?.id?.inc() ?: 0L
                            editItem = EditFormatRule(rules.lastIndex + 1, getNewRule(id), true)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add",
                        )
                    }
                }
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
        text = {
            editItem?.let { (index, item) ->
                var currentTab by remember { mutableStateOf(RuleTab.DESIGN) }
                val data = remember {
                    RuleTab.entries.map { TabData(it, it.uiString) }.toImmutableList()
                }
                FormatDialogTabRow(
                    currentItem = currentTab,
                    onClick = { currentTab = it },
                    list = data,
                    createTab = { item, isSelected, onClick ->
                        Tab(
                            text = {
                                Text(
                                    text = strings.get(item.data),
                                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                                    maxLines = 1,
                                )
                            },
                            selected = isSelected,
                            onClick = onClick,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    when (currentTab) {
                        RuleTab.DESIGN -> {
                            FormatDialogDesignTab(
                                item = item,
                                onChange = { newItem -> editItem = EditFormatRule(index, newItem) },
                                strings = strings,
                            )
                        }

                        RuleTab.CONDITION -> {
                            FormatDialogConditionTab(
                                item = item,
                                getTitle = getTitle,
                                filters = filters,
                                onChange = { newItem -> editItem = EditFormatRule(index, newItem) },
                                strings = strings,
                            )
                        }

                        RuleTab.FIELD -> {
                            FormatDialogFieldTab(
                                item = item,
                                entries = entries,
                                getTitle = getTitle,
                                onChange = { newItem -> editItem = EditFormatRule(index, newItem) },
                            )
                        }
                    }
                }
            } ?: run {
                var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
                var rulesState by remember(key) { mutableStateOf(rules) }
                LaunchedEffect(key) {
                    snapshotFlow { rulesState }
                        .drop(1)
                        .debounce(1000)
                        .distinctUntilChanged()
                        .collect { onRulesChanged(it) }
                }
                val state =
                    rememberReorderableLazyListState(lazyListState) { from, to ->
                        rulesState = rulesState.toPersistentList().mutate { list ->
                            list.add(to.index, list.removeAt(from.index))
                        }
                    }
                Box {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(rulesState, key = { _, it -> it.id }) { index, item ->
                            ReorderableItem(state = state, key = item.id) { isDragging ->
                                val elevation =
                                    animateDpAsState(if (isDragging) 16.dp else 0.dp)
                                Surface(
                                    shadowElevation = elevation.value,
                                    tonalElevation = elevation.value,
                                    modifier = Modifier.Companion.draggableHandle(),
                                    onClick = {
                                        editItem = EditFormatRule(index, item)
                                    },
                                ) {
                                    ListItem(
                                        overlineContent =
                                            buildList {
                                                if (item.cellStyle.textStyle != null) add(strings.get(UiString.FormatLabelTypography))
                                                if (item.cellStyle.vertical != null) add(strings.get(UiString.FormatLabelVerticalAlignment))
                                                if (item.cellStyle.horizontal !=
                                                    null
                                                ) {
                                                    add(strings.get(UiString.FormatLabelHorizontalAlignment))
                                                }
                                                if (item.cellStyle.backgroundColor != null) add(strings.get(UiString.FormatBackgroundColor))
                                                if (item.cellStyle.contentColor != null) add(strings.get(UiString.FormatContentColor))
                                            }.takeIf { it.isNotEmpty() }?.let {
                                                { Text(it.joinToString(", "), maxLines = 1) }
                                            },
                                        headlineContent = {
                                            val style =
                                                item.cellStyle.textStyle?.toTextStyle() ?: LocalTextStyle.current
                                            val color = item.cellStyle.contentColor?.toColor() ?: Color.Unspecified
                                            Text(
                                                buildRuleTitle(
                                                    rule = item,
                                                    getFieldTitle = getTitle,
                                                    filtersProvider = filters,
                                                    strings = strings,
                                                ),
                                                maxLines = 1,
                                                style = style,
                                                color = color,
                                            )
                                        },
                                        supportingContent =
                                            item.columns
                                                .map {
                                                    getTitle(it)
                                                }.takeIf { it.isNotEmpty() }
                                                ?.let {
                                                    { Text(it.joinToString(", "), maxLines = 1) }
                                                },
                                        leadingContent = {
                                            Checkbox(
                                                checked = item.enabled,
                                                onCheckedChange = { enabled ->
                                                    val itemIndex =
                                                        rulesState.indexOfFirst { it == item }
                                                    if (itemIndex != -1) {
                                                        rulesState = rulesState.toPersistentList().mutate { list ->
                                                            list[itemIndex] =
                                                                list[itemIndex].copy(
                                                                    enabled = enabled,
                                                                )
                                                        }
                                                        onRulesChanged(rulesState)
                                                    }
                                                },
                                                enabled = !item.base,
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                imageVector = Icons.Rounded.DragIndicator,
                                                contentDescription = null,
                                                modifier =
                                                    Modifier
                                                        .pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onPress = {
                                                                    draggedItemIndex = rulesState.indexOf(item)
                                                                    awaitRelease()
                                                                    draggedItemIndex = null
                                                                },
                                                            )
                                                        },
                                            )
                                        },
                                        colors =
                                            when {
                                                index == itemCopyIndex -> {
                                                    ListItemDefaults.colors(
                                                        containerColor =
                                                            if (settings.copiedItemHighlightColor.isUnspecified) {
                                                                MaterialTheme.colorScheme.tertiaryContainer
                                                            } else {
                                                                settings.copiedItemHighlightColor
                                                            },
                                                    )
                                                }

                                                item.cellStyle.backgroundColor != null -> {
                                                    val backgroundColor = item.cellStyle.backgroundColor.toColor()
                                                    val contrastColor = backgroundColor.contrastColor()
                                                    ListItemDefaults.colors(
                                                        containerColor = backgroundColor,
                                                        overlineColor = contrastColor,
                                                        supportingColor = contrastColor,
                                                        trailingIconColor = contrastColor,
                                                        leadingIconColor = contrastColor,
                                                    )
                                                }

                                                else -> {
                                                    ListItemDefaults.colors()
                                                }
                                            },
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
    )
}

private fun Color.contrastColor(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > 0.5) Color.Black else Color.White
}

@Composable
private fun <E : Enum<E>, FILTER> buildRuleTitle(
    rule: TableFormatRule<E, FILTER>,
    getFieldTitle: @Composable (E) -> String,
    filtersProvider: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    strings: StringProvider,
): String {
    val parts = mutableListOf<String>()
    val filterItems = filtersProvider(rule) { }
    for (filterData in filterItems) {
        val built = buildFilterHeaderTitle(filterData = filterData, strings = strings)
        if (built != null) {
            val fieldTitle = getFieldTitle(filterData.field)
            parts += "$fieldTitle $built"
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = " • ") ?: strings.get(UiString.FormatAlwaysApply)
}
