package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ua.wwind.table.ColumnSpec
import ua.wwind.table.MeasureCellMinWidth
import ua.wwind.table.component.LocalTableHeaderCellInfo
import ua.wwind.table.component.LocalTableHeaderIcons
import ua.wwind.table.component.TableHeaderCellInfo
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.currentTableColors
import ua.wwind.table.config.isInteractionLockByRowReorderEnabled
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.component.main.FilterPanel
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.state.PinnedEdge
import ua.wwind.table.state.TableState
import ua.wwind.table.state.currentTableState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Composable
internal fun <T : Any, C, E> HeaderCell(
    spec: ColumnSpec<T, C, E>,
    state: TableState<C>,
    tableData: E,
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
    /** The pinned-run edge this cell sits on; its right divider then takes the pinned color. */
    pinnedEdge: PinnedEdge = PinnedEdge.None,
    onOpenMenu: (() -> Unit)? = null,
) {
    val colors = currentTableColors()
    val interactionLocked = state.settings.isInteractionLockByRowReorderEnabled
    val sortOrder: SortOrder? = state.sort?.takeIf { it.column == spec.key }?.order
    val isFilterActive: Boolean = state.filters[spec.key]?.isActive() == true

    val info =
        TableHeaderCellInfo(
            columnKey = spec.key as Any?,
            isSortable = spec.sortable && !interactionLocked,
            sortOrder = sortOrder,
            hasFilter = spec.filter != null,
            isFilterActive = isFilterActive,
            toggleSort = onToggleSort,
            sortIcon = {
                SortButton(
                    enabled = spec.sortable && !interactionLocked,
                    order = sortOrder,
                    icons = LocalTableHeaderIcons.current,
                    onToggle = onToggleSort,
                    clickable = !spec.headerClickToSort,
                    targetSize = state.dimensions.headerIconTargetSize,
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
                    targetSize = state.dimensions.headerIconTargetSize,
                )
            },
        )

    // Measure header content minimal width to contribute to max content width for resizable or auto-width columns
    if (spec.resizable || spec.autoWidth) {
        // Include title text in measureKey so that when localized strings load, re-measurement is triggered
        val titleText = spec.title?.invoke()
        MeasureCellMinWidth(
            item = Unit,
            tableData = tableData,
            measureKey = listOf(spec.key, "header", titleText, onOpenMenu != null),
            onMeasure = { measuredMinWidth ->
                val adjusted = maxOf(measuredMinWidth, spec.minWidth)
                state.columns.updateMaxContentWidth(spec.key, adjusted, source = "Header")
            },
        ) { _, _ ->
            HeaderMeasureContent(spec, info, showMenuButton = onOpenMenu != null, dimensions = state.dimensions)
        }
    }

    Row {
        if (showLeftDivider) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = leftDividerThickness,
                color = colors.pinnedDividerColor,
            )
        }
        Box(
            modifier =
                Modifier
                    .width(width)
                    .fillMaxHeight(),
            contentAlignment = spec.headerAlignment ?: spec.alignment,
        ) {
            HeaderContent(
                spec = spec,
                info = info,
                isFilterOpen = isFilterOpen,
                state = state,
                tableData = tableData,
                onDismissFilter = onDismissFilter,
                strings = strings,
                onOpenMenu = onOpenMenu,
            )
            // Without decorations there is no filter icon to anchor the panel to, so it opens at the
            // end of the cell. This keeps "Filter…" in the column menu and custom filter icons working.
            if (isFilterOpen && !spec.headerDecorations) {
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
                    HeaderFilterPanel(spec, state, tableData, strings, onDismissFilter)
                }
            }
        }
        if (showRightDivider) {
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = dividerThickness,
                color = if (pinnedEdge == PinnedEdge.Right) colors.pinnedDividerColor else colors.dividerColor,
            )
        }
    }
}

@Composable
private fun DefaultFilterIcon(info: TableHeaderCellInfo<Any?>) {
    Box(
        modifier = Modifier.padding(end = currentTableState().dimensions.headerIconSpacing),
    ) {
        info.filterIcon.invoke()
    }
}

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TruncationTooltipBox(
    title: (@Composable () -> String)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val movableContent = remember(content) { movableContentOf { content() } }
    val titleText = title?.invoke()
    if (titleText == null) {
        Box(modifier) { movableContent() }
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = LocalTextStyle.current
    var availableWidthPx by remember { mutableIntStateOf(0) }
    val measuredTitleWidthPx = textMeasurer.measure(text = titleText, style = textStyle, maxLines = 1).size.width
    val isTruncated = availableWidthPx in 1 until measuredTitleWidthPx

    val tooltipState = rememberTooltipState(isPersistent = false)
    val positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above)

    // The caller's modifier sits on a box that survives the switch to and from the tooltip, so
    // layout parent data such as a row weight keeps applying while the title is truncated.
    Box(modifier) {
        if (isTruncated) {
            TooltipBox(
                positionProvider = positionProvider,
                state = tooltipState,
                focusable = false,
                enableUserInput = true,
                tooltip = { PlainTooltip { Text(titleText) } },
            ) {
                Box(modifier = Modifier.onSizeChanged { availableWidthPx = it.width }) {
                    movableContent()
                }
            }
        } else {
            Box(modifier = Modifier.onSizeChanged { availableWidthPx = it.width }) {
                movableContent()
            }
        }
    }
}

@Composable
private fun <C, E> HeaderContent(
    spec: ColumnSpec<*, C, E>,
    info: TableHeaderCellInfo<Any?>,
    isFilterOpen: Boolean,
    state: TableState<C>,
    tableData: E,
    onDismissFilter: () -> Unit,
    strings: StringProvider,
    onOpenMenu: (() -> Unit)?,
) {
    val arrangement = (spec.headerAlignment ?: spec.alignment).horizontalArrangement()
    // In an end-aligned header the icon leads, so the title's end lines up with the cells below.
    val sortIconFirst = arrangement == Arrangement.End
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val padding = state.dimensions.cellHorizontalPadding
        Row(
            modifier = if (spec.headerDecorations) Modifier.weight(1f).padding(horizontal = padding) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = arrangement,
        ) {
            CompositionLocalProvider(
                LocalTableHeaderCellInfo provides info,
            ) {
                if (spec.headerDecorations && sortIconFirst) info.sortIcon.invoke()
                // The title takes only the space the icon leaves, so it truncates instead of the icon
                // shrinking; the tooltip attaches only when it is truncated.
                TruncationTooltipBox(title = spec.title, modifier = Modifier.weight(1f, fill = false)) {
                    spec.header(tableData)
                }
                if (spec.headerDecorations && !sortIconFirst) info.sortIcon.invoke()
            }
        }
        if (spec.headerDecorations) {
            if (onOpenMenu != null) {
                val title = spec.title?.invoke()
                val options = strings.get(UiString.ColumnMenuOptions)
                ColumnMenuButton(
                    contentDescription = if (title != null) "$options: $title" else options,
                    targetSize = state.dimensions.headerIconTargetSize,
                    onClick = onOpenMenu,
                )
            }
            Box {
                DefaultFilterIcon(info)

                if (isFilterOpen) HeaderFilterPanel(spec, state, tableData, strings, onDismissFilter)
            }
        }
    }
}

/** The filter dropdown of [spec]'s column, anchored to the enclosing layout. */
@Composable
private fun <C, E> HeaderFilterPanel(
    spec: ColumnSpec<*, C, E>,
    state: TableState<C>,
    tableData: E,
    strings: StringProvider,
    onDismissFilter: () -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    FilterPanel(
        type = spec.filter as? TableFilterType<Any?>,
        state = state.filters[spec.key] as? TableFilterState<Any?>,
        tableData = tableData,
        expanded = true,
        onDismissRequest = onDismissFilter,
        strings = strings,
        autoApplyFilters = state.settings.autoApplyFilters,
        autoFilterDebounce = state.settings.autoFilterDebounce,
        onChange = { newState -> state.setFilter(spec.key, newState) },
    )
}

@Composable
private fun HeaderMeasureContent(
    spec: ColumnSpec<*, *, *>,
    info: TableHeaderCellInfo<Any?>,
    showMenuButton: Boolean,
    dimensions: TableDimensions,
) {
    // Do not render any popups inside measured content!
    val padding = dimensions.cellHorizontalPadding
    Row(
        modifier = if (spec.headerDecorations) Modifier.padding(horizontal = padding) else Modifier,
    ) {
        spec.title?.invoke()?.let { Text(it) }
        if (spec.headerDecorations) {
            info.sortIcon.invoke()
            if (showMenuButton) Spacer(Modifier.width(dimensions.headerIconTargetSize))
            DefaultFilterIcon(info)
        }
    }
}

/** The horizontal part of this alignment as a row arrangement: start, centre or end. */
private fun Alignment.horizontalArrangement(): Arrangement.Horizontal {
    // Aligning a zero-width item in a 2px space yields 0, 1 or 2 for start, centre and end.
    val x = align(IntSize.Zero, IntSize(2, 0), LayoutDirection.Ltr).x
    return when {
        x <= 0 -> Arrangement.Start
        x >= 2 -> Arrangement.End
        else -> Arrangement.Center
    }
}
