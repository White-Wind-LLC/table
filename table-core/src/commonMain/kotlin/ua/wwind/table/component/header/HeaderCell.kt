package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.MeasureCellMinWidth
import ua.wwind.table.component.LocalTableHeaderCellInfo
import ua.wwind.table.component.LocalTableHeaderIcons
import ua.wwind.table.component.TableHeaderCellInfo
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@Composable
internal fun <T : Any, C> HeaderCell(
    spec: ColumnSpec<T, C>,
    state: TableState<C>,
    strings: StringProvider,
    width: Dp,
    dividerThickness: Dp,
    isFilterOpen: Boolean,
    onOpenFilter: () -> Unit,
    onDismissFilter: () -> Unit,
    onToggleSort: () -> Unit,
    showLeftDivider: Boolean = false,
    leftDividerThickness: Dp = dividerThickness,
    showRightDivider: Boolean = true,
) {
    val sortOrder: SortOrder? = state.sort?.takeIf { it.column == spec.key }?.order
    val isFilterActive: Boolean = state.filters[spec.key]?.values?.isEmpty() == false

    val info =
        TableHeaderCellInfo(
            columnKey = spec.key as Any?,
            isSortable = spec.sortable,
            sortOrder = sortOrder,
            hasFilter = spec.filter != null,
            isFilterActive = isFilterActive,
            toggleSort = onToggleSort,
            sortIcon = {
                SortButton(
                    enabled = spec.sortable,
                    order = sortOrder,
                    icons = LocalTableHeaderIcons.current,
                    onToggle = onToggleSort,
                    clickable = !spec.headerClickToSort,
                )
            },
            filterIcon = {
                FilterButton(
                    enabled = spec.filter != null,
                    active = isFilterActive,
                    icons = LocalTableHeaderIcons.current,
                    isOpen = isFilterOpen,
                    onOpen = onOpenFilter,
                    onDismiss = onDismissFilter,
                )
            },
        )

    // Measure header content minimal width to contribute to max content width for resizable or auto-width columns
    if (spec.resizable || spec.autoWidth) {
        MeasureCellMinWidth(
            item = Unit,
            measureKey = Pair(spec.key, "header"),
            content = { _ ->
                HeaderMeasureContent(spec, info)
            },
        ) { measuredMinWidth ->
            val adjusted = maxOf(measuredMinWidth, spec.minWidth)
            state.updateMaxContentWidth(spec.key, adjusted)
        }
    }

    Row {
        if (showLeftDivider) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = leftDividerThickness,
            )
        }
        Box(
            modifier =
                Modifier
                    .width(width)
                    .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            HeaderContent(
                spec = spec,
                info = info,
                isFilterOpen = isFilterOpen,
                state = state,
                onDismissFilter = onDismissFilter,
                strings = strings,
            )
        }
        if (showRightDivider) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = dividerThickness,
            )
        }
    }
}

@Composable
private fun DefaultFilterIcon(info: TableHeaderCellInfo<Any?>) {
    Box(
        modifier = Modifier.padding(end = 6.dp),
    ) {
        info.filterIcon.invoke()
    }
}

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TruncationTooltipBox(
    title: String?,
    content: @Composable () -> Unit,
) {
    if (title == null) {
        content()
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val textStyle = LocalTextStyle.current
    var availableWidthPx by remember { mutableIntStateOf(0) }
    val measuredTitleWidthPx = textMeasurer.measure(text = title, style = textStyle, maxLines = 1).size.width
    val isTruncated = availableWidthPx in 1 until measuredTitleWidthPx

    val tooltipState = rememberTooltipState(isPersistent = false)
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()

    if (isTruncated) {
        TooltipBox(
            positionProvider = positionProvider,
            state = tooltipState,
            focusable = false,
            enableUserInput = true,
            tooltip = { PlainTooltip { Text(title) } },
        ) {
            Box(modifier = Modifier.onSizeChanged { availableWidthPx = it.width }) {
                content()
            }
        }
    } else {
        Box(modifier = Modifier.onSizeChanged { availableWidthPx = it.width }) {
            content()
        }
    }
}

@Composable
private fun <C> HeaderContent(
    spec: ColumnSpec<*, C>,
    info: TableHeaderCellInfo<Any?>,
    isFilterOpen: Boolean,
    state: TableState<C>,
    onDismissFilter: () -> Unit,
    strings: StringProvider,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = if (spec.headerDecorations) Modifier.weight(1f).padding(horizontal = 8.dp) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CompositionLocalProvider(
                LocalTableHeaderCellInfo provides (info as TableHeaderCellInfo<Any?>),
            ) {
                // Constrain header text area and attach tooltip only when truncated
                val titleText = spec.title?.invoke()
                TruncationTooltipBox(title = titleText) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        spec.header()
                        if (spec.headerDecorations) {
                            info.sortIcon.invoke()
                        }
                    }
                }
            }
        }
        if (spec.headerDecorations) {
            Box {
                DefaultFilterIcon(info)

                if (isFilterOpen) {
                    @Suppress("UNCHECKED_CAST")
                    FilterPanel(
                        type = spec.filter as? TableFilterType<Any?>,
                        state = state.filters[spec.key] as? TableFilterState<Any?>,
                        expanded = true,
                        onDismissRequest = onDismissFilter,
                        strings = strings,
                        autoApplyFilters = state.settings.autoApplyFilters,
                        autoFilterDebounce = state.settings.autoFilterDebounce,
                        onChange = { newState ->
                            state.setFilter(spec.key, newState)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderMeasureContent(
    spec: ColumnSpec<*, *>,
    info: TableHeaderCellInfo<Any?>,
) {
    // Do not render any popups inside measured content!
    Row(
        modifier = if (spec.headerDecorations) Modifier.padding(horizontal = 8.dp) else Modifier,
    ) {
        spec.title?.invoke()?.let { Text(it) }
        if (spec.headerDecorations) {
            info.sortIcon.invoke()
            DefaultFilterIcon(info)
        }
    }
}
