package ua.wwind.table.format

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.painterResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import ua.wwind.table.config.isReduced
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.format.component.FormatDialogTabRow
import ua.wwind.table.format.component.MIN_TEXT_CONTRAST
import ua.wwind.table.format.component.RuleTab
import ua.wwind.table.format.component.TabData
import ua.wwind.table.format.component.contrastRatio
import ua.wwind.table.format.component.readableContentColor
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.FormatDialogSettings
import ua.wwind.table.format.data.TableCellStyleConfig
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.format.scrollbar.VerticalScrollbarState
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString
import kotlin.time.Duration.Companion.milliseconds

// Rules are edited in place; wait for the edits to settle before reporting them upstream.
private const val RULES_CHANGE_DEBOUNCE_MS = 1_000L

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
        state.editItem?.let { edit ->
            val item = edit.item
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
                            onChange = { newItem -> state.editItem = edit.copy(item = newItem) },
                            strings = strings,
                            scrollbarRenderer = scrollbarRenderer,
                        )
                    }

                    RuleTab.CONDITION -> {
                        FormatDialogConditionTab(
                            item = item,
                            getTitle = getTitle,
                            filters = filters,
                            onChange = { newItem -> state.editItem = edit.copy(item = newItem) },
                            strings = strings,
                            scrollbarRenderer = scrollbarRenderer,
                            motion = settings.motion,
                        )
                    }

                    RuleTab.FIELD -> {
                        FormatDialogFieldTab(
                            item = item,
                            entries = entries,
                            getTitle = getTitle,
                            onChange = { newItem -> state.editItem = edit.copy(item = newItem) },
                            scrollbarRenderer = scrollbarRenderer,
                        )
                    }
                }
            }
        } ?: run {
            var rulesState by remember(key) { mutableStateOf(rules) }
            val reportedRules = remember(key) { mutableStateOf(rules) }
            val currentOnRulesChange = rememberUpdatedState(onRulesChange)
            LaunchedEffect(key) {
                snapshotFlow { rulesState }
                    .drop(1)
                    .debounce(RULES_CHANGE_DEBOUNCE_MS.milliseconds)
                    .distinctUntilChanged()
                    .collect {
                        reportedRules.value = it
                        currentOnRulesChange.value(it)
                    }
            }
            // Opening a rule or closing the dialog disposes the list inside the debounce window; report
            // the order it still holds instead of dropping it.
            DisposableEffect(key) {
                onDispose {
                    if (rulesState != reportedRules.value) currentOnRulesChange.value(rulesState)
                }
            }

            fun move(
                from: Int,
                to: Int,
            ) {
                rulesState = rulesState.toPersistentList().mutate { list -> list.add(to, list.removeAt(from)) }
            }
            val reorderableState =
                rememberReorderableLazyListState(state.lazyListState) { from, to -> move(from.index, to.index) }
            Box {
                if (rulesState.isEmpty()) {
                    Text(
                        text = strings.get(UiString.FormatRulesEmpty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
                    )
                }
                val reducedMotion = settings.motion.isReduced()
                LazyColumn(state = state.lazyListState, modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(rulesState, key = { _, item -> item.id }) { index, item ->
                        ReorderableItem(
                            state = reorderableState,
                            key = item.id,
                            animateItemModifier = if (reducedMotion) Modifier else Modifier.animateItem(),
                        ) { isDragging ->
                            FormatRuleRow(
                                item = item,
                                isDragging = isDragging,
                                highlighted = index == state.itemCopyIndex,
                                settings = settings,
                                canMoveUp = index > 0,
                                canMoveDown = index < rulesState.lastIndex,
                                onMove = { delta -> move(index, index + delta) },
                                onOpen = { state.editItem = EditFormatRule(index, item) },
                                onEnabledChange = { enabled ->
                                    val itemIndex = rulesState.indexOfFirst { it == item }
                                    if (itemIndex != -1) {
                                        rulesState =
                                            rulesState.toPersistentList().mutate { list ->
                                                list[itemIndex] = list[itemIndex].copy(enabled = enabled)
                                            }
                                        reportedRules.value = rulesState
                                        onRulesChange(rulesState)
                                    }
                                },
                                getTitle = getTitle,
                                filters = filters,
                                strings = strings,
                            )
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

/**
 * One rule in the list: a preview of its style, an enabled checkbox and a reorder handle. Only the handle drags;
 * activating it opens Move up / Move down, which screen readers also get as custom actions on the row.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
private fun <E : Enum<E>, FILTER> ReorderableCollectionItemScope.FormatRuleRow(
    item: TableFormatRule<E, FILTER>,
    isDragging: Boolean,
    highlighted: Boolean,
    settings: FormatDialogSettings,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    onOpen: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    strings: StringProvider,
) {
    val elevation =
        animateDpAsState(
            if (isDragging) 16.dp else 0.dp,
            if (settings.motion.isReduced()) snap() else spring(visibilityThreshold = Dp.VisibilityThreshold),
        )
    val moveUp = strings.get(UiString.FormatRuleMoveUp)
    val moveDown = strings.get(UiString.FormatRuleMoveDown)
    val colors = rulePreviewColors(item.cellStyle, highlighted, settings)
    Surface(
        shadowElevation = elevation.value,
        tonalElevation = elevation.value,
        onClick = onOpen,
    ) {
        ListItem(
            // ListItem merges its own semantics, so screen readers focus it, not the Surface around it.
            modifier =
                Modifier.semantics {
                    customActions =
                        buildList {
                            if (canMoveUp) add(CustomAccessibilityAction(moveUp) { onMove(-1).let { true } })
                            if (canMoveDown) add(CustomAccessibilityAction(moveDown) { onMove(1).let { true } })
                        }
                },
            overlineContent =
                item.cellStyle.styleLabels(strings).takeIf { it.isNotEmpty() }?.let {
                    { Text(it.joinToString(", "), maxLines = 1) }
                },
            headlineContent = {
                Text(
                    buildRuleTitle(rule = item, getFieldTitle = getTitle, filtersProvider = filters, strings = strings),
                    maxLines = 1,
                    style = item.cellStyle.textStyle?.toTextStyle() ?: LocalTextStyle.current,
                    color = colors.headline,
                )
            },
            supportingContent =
                item.columns
                    .map { getTitle(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { { Text(it.joinToString(", "), maxLines = 1) } },
            leadingContent = {
                Checkbox(checked = item.enabled, onCheckedChange = onEnabledChange, enabled = !item.base)
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (colors.lowContrast) {
                        Icon(
                            painter = painterResource(TableIcons.ErrorOutline),
                            contentDescription = strings.get(UiString.FormatColorLowContrast),
                            tint = colors.warning,
                        )
                    }
                    RuleReorderHandle(canMoveUp, canMoveDown, onMove, strings)
                }
            },
            colors = colors.listItem,
        )
    }
}

/** Drags the rule; a click, Enter or Space opens Move up / Move down instead. */
@Composable
private fun ReorderableCollectionItemScope.RuleReorderHandle(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    strings: StringProvider,
) {
    Box {
        var menuOpen by remember { mutableStateOf(false) }
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.draggableHandle()) {
            Icon(
                painter = painterResource(TableIcons.DragIndicator),
                contentDescription = strings.get(UiString.FormatRuleReorder),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(strings.get(UiString.FormatRuleMoveUp)) },
                leadingIcon = { Icon(painter = painterResource(TableIcons.ArrowUpward), contentDescription = null) },
                enabled = canMoveUp,
                onClick = {
                    menuOpen = false
                    onMove(-1)
                },
            )
            DropdownMenuItem(
                text = { Text(strings.get(UiString.FormatRuleMoveDown)) },
                leadingIcon = { Icon(painter = painterResource(TableIcons.ArrowDownward), contentDescription = null) },
                enabled = canMoveDown,
                onClick = {
                    menuOpen = false
                    onMove(1)
                },
            )
        }
    }
}

/** Names of the style properties a rule sets, in the order the row's overline lists them. */
@Composable
private fun TableCellStyleConfig.styleLabels(strings: StringProvider): List<String> =
    buildList {
        if (textStyle != null) add(strings.get(UiString.FormatLabelTypography))
        if (vertical != null) add(strings.get(UiString.FormatLabelVerticalAlignment))
        if (horizontal != null) add(strings.get(UiString.FormatLabelHorizontalAlignment))
        if (backgroundColor != null) add(strings.get(UiString.FormatBackgroundColor))
        if (contentColor != null) add(strings.get(UiString.FormatContentColor))
    }

private class RulePreviewColors(
    val listItem: ListItemColors,
    val headline: Color,
    val warning: Color,
    val lowContrast: Boolean,
)

/**
 * Colors for a rule's row. The headline previews the rule's text color; every other line and icon takes a color
 * readable on the row's container (the copy highlight, else the rule's background). [RulePreviewColors.lowContrast]
 * flags a text color that is hard to read on the rule's background (or on the surface when it has none).
 */
@Composable
private fun rulePreviewColors(
    style: TableCellStyleConfig,
    highlighted: Boolean,
    settings: FormatDialogSettings,
): RulePreviewColors {
    val scheme = MaterialTheme.colorScheme
    val content = style.contentColor?.toColor()
    val background = style.backgroundColor?.toColor()
    val lowContrast = content != null && contrastRatio(content, background ?: scheme.surface) < MIN_TEXT_CONTRAST
    val highlight = settings.copiedItemHighlightColor
    val (container, onContainer) =
        when {
            highlighted && highlight.isUnspecified -> scheme.tertiaryContainer to scheme.onTertiaryContainer

            highlighted -> highlight to highlight.readableContentColor()

            background != null -> background to background.readableContentColor()

            else -> return RulePreviewColors(
                ListItemDefaults.colors(),
                content ?: Color.Unspecified,
                scheme.error,
                lowContrast,
            )
        }
    val headline =
        when {
            content == null -> onContainer
            highlighted && contrastRatio(content, container) < MIN_TEXT_CONTRAST -> onContainer
            else -> content
        }
    return RulePreviewColors(
        listItem =
            ListItemDefaults.colors(
                containerColor = container,
                headlineColor = onContainer,
                overlineColor = onContainer,
                supportingColor = onContainer,
                leadingIconColor = onContainer,
                trailingIconColor = onContainer,
            ),
        headline = headline,
        warning = onContainer,
        lowContrast = lowContrast,
    )
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
