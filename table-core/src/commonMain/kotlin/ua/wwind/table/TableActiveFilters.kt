package ua.wwind.table

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.component.header.ColumnFilterPanel
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * Renders a compact header row with chips describing currently active filters.
 * Clicking a chip opens that column's filter panel under the chip; its × removes the filter.
 *
 * @param columns all column specs used by the table
 * @param state table state containing current filters
 * @param tableData table data passed to the filter panels (custom filters read it)
 * @param modifier layout modifier for the chips row
 * @param strings string provider for localized labels
 * @param includeClearAllChip whether to include a leading "Clear all" chip to reset all filters
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
) {
    ActiveFilterChips(
        columns = columns,
        state = state,
        strings = strings,
        includeClearAllChip = includeClearAllChip,
        filterPanel = { spec, onDismiss -> ColumnFilterPanel(spec, state, tableData, strings, onDismiss) },
        modifier = modifier,
    )
}

/**
 * Renders a compact header row with chips describing currently active filters.
 * Without table data the chips cannot open filter panels; their × removes the filter.
 */
@Deprecated(
    "Pass tableData so that clicking a chip opens its filter panel.",
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
) {
    ActiveFilterChips(columns, state, strings, includeClearAllChip, filterPanel = null, modifier = modifier)
}

/**
 * The chips row. [filterPanel] renders a column's filter panel under its chip; null makes chip clicks do nothing.
 * [footer] is shown under the row while the row is shown.
 */
@Composable
@Suppress("LongParameterList")
internal fun <T : Any, C, E> ActiveFilterChips(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    state: TableState<C>,
    strings: StringProvider,
    includeClearAllChip: Boolean,
    filterPanel: (@Composable (spec: ColumnSpec<T, C, E>, onDismiss: () -> Unit) -> Unit)?,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    val keyToSpec: Map<C, ColumnSpec<T, C, E>> = remember(columns) { columns.associateBy { it.key } }
    var openColumn by remember { mutableStateOf<C?>(null) }
    // The open panel's chip stays while the panel is open, so editing the filter down to empty
    // does not take the panel away under the user.
    val shownFilters: List<Pair<C, TableFilterState<*>>> =
        state.filters.filter { (col, st) -> st.isActive() || col == openColumn }.toList()
    if (shownFilters.isEmpty()) return
    val listState: LazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (includeClearAllChip) {
                InputChip(
                    selected = true,
                    onClick = {
                        openColumn = null
                        shownFilters.forEach { (col, _) -> clearFilter(state, col, keyToSpec[col]) }
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
                listState = listState,
                enabled = listState.canScrollBackward,
                icon = TableIcons.KeyboardArrowLeft,
                modifier = Modifier.padding(end = 4.dp),
                onClick = {
                    scope.launch {
                        val target = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(target)
                    }
                },
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shownFilters.forEach { (col, st) ->
                    item {
                        keyToSpec[col]?.let { spec ->
                            val isOpen = col == openColumn
                            ActiveFilterChip(
                                spec = spec,
                                filter = st,
                                isOpen = isOpen,
                                strings = strings,
                                onClick = { if (filterPanel != null) openColumn = col },
                                onClear = {
                                    if (isOpen) openColumn = null
                                    clearFilter(state, col, spec)
                                },
                            ) {
                                filterPanel?.invoke(spec) { openColumn = null }
                            }
                        }
                    }
                }
            }
            ChipsScrollButton(
                listState = listState,
                enabled = listState.canScrollForward,
                icon = TableIcons.KeyboardArrowRight,
                modifier = Modifier.padding(start = 4.dp),
                onClick = {
                    scope.launch {
                        val lastIndex = (shownFilters.size - 1).coerceAtLeast(0)
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
    listState: LazyListState,
    enabled: Boolean,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = listState.canScrollBackward || listState.canScrollForward) {
        IconButton(enabled = enabled, onClick = onClick, modifier = modifier) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
    }
}

/**
 * [spec]'s chip: a click calls [onClick], its × calls [onClear]. While [isOpen], [panel] is anchored under
 * the chip, and the chip stays with only the column title if [filter] no longer describes a value.
 */
@Composable
@Suppress("LongParameterList")
private fun <T : Any, C, E> ActiveFilterChip(
    spec: ColumnSpec<T, C, E>,
    filter: TableFilterState<*>,
    isOpen: Boolean,
    strings: StringProvider,
    onClick: () -> Unit,
    onClear: () -> Unit,
    panel: @Composable () -> Unit,
) {
    val text = spec.filter?.let { buildFilterChipTextUnsafe(it, filter, strings) }
    if (text == null && !isOpen) return
    val title = spec.title?.invoke() ?: spec.key.toString()
    Box {
        InputChip(
            selected = true,
            onClick = onClick,
            label = { Text(if (text != null) "$title: $text" else title) },
            trailingIcon = {
                Icon(
                    painter = painterResource(TableIcons.Close),
                    contentDescription = strings.get(UiString.ColumnMenuClearFilter),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onClear),
                )
            },
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        if (isOpen) panel()
    }
}

/** Resets [col]'s filter to its first constraint with no value. */
private fun <C> clearFilter(
    state: TableState<C>,
    col: C,
    spec: ColumnSpec<*, C, *>?,
) {
    val defaultConstraint = spec?.filter?.constraints?.firstOrNull() ?: FilterConstraint.EQUALS
    state.setFilter(col, TableFilterState<Any?>(defaultConstraint, null))
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
