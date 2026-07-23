package ua.wwind.table.format

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.format.component.FormatDialogTabRow
import ua.wwind.table.format.component.RuleTab
import ua.wwind.table.format.component.TabData
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.time.Duration.Companion.milliseconds

// Rules are edited in place; wait for the edits to settle before reporting them upstream.
private const val RULES_CHANGE_DEBOUNCE_MS = 1_000L

// Above this perceived brightness a background takes black text, below it white.
private const val LUMINANCE_THRESHOLD = 0.5

@Suppress("CyclomaticComplexMethod", "LongParameterList", "LongMethod")
@OptIn(FlowPreview::class)
@Composable
internal fun <E : Enum<E>, FILTER> FormatDialogBody(
    state: FormatDialogState<E, FILTER>,
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    entries: ImmutableList<E>,
    key: Any,
    strings: StringProvider,
    settings: FormatDialogSettings,
    scrollbarRenderer: VerticalScrollbarRenderer?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        state.editItem?.let { (index, item) ->
            var currentTab by remember { mutableStateOf(RuleTab.DESIGN) }
            val data =
                remember {
                    RuleTab.entries.map { TabData(it, it.uiString) }.toImmutableList()
                }
            FormatDialogTabRow(
                currentItem = currentTab,
                onClick = { currentTab = it },
                list = data,
                createTab = { tabItem, isSelected, onClick ->
                    Tab(
                        text = {
                            Text(
                                text = strings.get(tabItem.data),
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
                            onChange = { newItem -> state.editItem = EditFormatRule(index, newItem) },
                            strings = strings,
                            scrollbarRenderer = scrollbarRenderer,
                        )
                    }

                    RuleTab.CONDITION -> {
                        FormatDialogConditionTab(
                            item = item,
                            getTitle = getTitle,
                            filters = filters,
                            onChange = { newItem -> state.editItem = EditFormatRule(index, newItem) },
                            strings = strings,
                            scrollbarRenderer = scrollbarRenderer,
                        )
                    }

                    RuleTab.FIELD -> {
                        FormatDialogFieldTab(
                            item = item,
                            entries = entries,
                            getTitle = getTitle,
                            onChange = { newItem -> state.editItem = EditFormatRule(index, newItem) },
                            scrollbarRenderer = scrollbarRenderer,
                        )
                    }
                }
            }
        } ?: run {
            var rulesState by remember(key) { mutableStateOf(rules) }
            val currentOnRulesChange = rememberUpdatedState(onRulesChange)
            LaunchedEffect(key) {
                snapshotFlow { rulesState }
                    .drop(1)
                    .debounce(RULES_CHANGE_DEBOUNCE_MS.milliseconds)
                    .distinctUntilChanged()
                    .collect { currentOnRulesChange.value(it) }
            }
            val reorderableState =
                rememberReorderableLazyListState(state.lazyListState) { from, to ->
                    rulesState =
                        rulesState.toPersistentList().mutate { list ->
                            list.add(to.index, list.removeAt(from.index))
                        }
                }
            Box {
                LazyColumn(state = state.lazyListState, modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(rulesState, key = { _, item -> item.id }) { index, item ->
                        ReorderableItem(state = reorderableState, key = item.id) { isDragging ->
                            val elevation =
                                animateDpAsState(if (isDragging) 16.dp else 0.dp)
                            Surface(
                                shadowElevation = elevation.value,
                                tonalElevation = elevation.value,
                                modifier = Modifier.draggableHandle(),
                                onClick = {
                                    state.editItem = EditFormatRule(index, item)
                                },
                            ) {
                                ListItem(
                                    overlineContent =
                                        buildList {
                                            if (item.cellStyle.textStyle !=
                                                null
                                            ) {
                                                add(strings.get(UiString.FormatLabelTypography))
                                            }
                                            if (item.cellStyle.vertical !=
                                                null
                                            ) {
                                                add(strings.get(UiString.FormatLabelVerticalAlignment))
                                            }
                                            if (item.cellStyle.horizontal !=
                                                null
                                            ) {
                                                add(strings.get(UiString.FormatLabelHorizontalAlignment))
                                            }
                                            if (item.cellStyle.backgroundColor !=
                                                null
                                            ) {
                                                add(strings.get(UiString.FormatBackgroundColor))
                                            }
                                            if (item.cellStyle.contentColor !=
                                                null
                                            ) {
                                                add(strings.get(UiString.FormatContentColor))
                                            }
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
                                                    rulesState =
                                                        rulesState.toPersistentList().mutate { list ->
                                                            list[itemIndex] =
                                                                list[itemIndex].copy(
                                                                    enabled = enabled,
                                                                )
                                                        }
                                                    onRulesChange(rulesState)
                                                }
                                            },
                                            enabled = !item.base,
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Rounded.DragIndicator,
                                            contentDescription = null,
                                        )
                                    },
                                    colors =
                                        when {
                                            index == state.itemCopyIndex -> {
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
                scrollbarRenderer?.Render(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight(),
                    state = VerticalScrollbarState.LazyList(state.lazyListState),
                )
            }
        }
    }
}

private fun Color.contrastColor(): Color {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return if (luminance > LUMINANCE_THRESHOLD) Color.Black else Color.White
}

@Composable
private fun <E : Enum<E>, FILTER> buildRuleTitle(
    rule: TableFormatRule<E, FILTER>,
    getFieldTitle: @Composable (E) -> String,
    filtersProvider: (
        TableFormatRule<E, FILTER>,
        onApply: (TableFormatRule<E, FILTER>) -> Unit,
    ) -> List<FormatFilterData<E>>,
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
    return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = " • ")
        ?: strings.get(UiString.FormatAlwaysApply)
}
