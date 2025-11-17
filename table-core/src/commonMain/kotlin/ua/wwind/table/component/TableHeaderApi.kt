package ua.wwind.table.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import ua.wwind.table.data.SortOrder

/** Icons used by table header for sort and filter affordances. */
@Immutable
public data class TableHeaderIcons(
    val sortAsc: ImageVector,
    val sortDesc: ImageVector,
    val sortNeutral: ImageVector,
    val filterActive: ImageVector,
    val filterInactive: ImageVector,
)

public object TableHeaderDefaults {
    @Composable
    /** Factory for [TableHeaderIcons] with sensible defaults. */
    public fun icons(
        sortAsc: ImageVector = Icons.Rounded.ArrowUpward,
        sortDesc: ImageVector = Icons.Rounded.ArrowDownward,
        sortNeutral: ImageVector = Icons.AutoMirrored.Outlined.Sort,
        filterActive: ImageVector = Icons.Rounded.FilterAlt,
        filterInactive: ImageVector = Icons.Outlined.FilterAlt,
    ): TableHeaderIcons =
        TableHeaderIcons(
            sortAsc = sortAsc,
            sortDesc = sortDesc,
            sortNeutral = sortNeutral,
            filterActive = filterActive,
            filterInactive = filterInactive,
        )
}

/** Per-header cell info and helpers provided to the header slot via CompositionLocal. */
@Immutable
public data class TableHeaderCellInfo<C>(
    val columnKey: C,
    val isSortable: Boolean,
    val sortOrder: SortOrder?,
    val hasFilter: Boolean,
    val isFilterActive: Boolean,
    val toggleSort: () -> Unit,
    val sortIcon: @Composable () -> Unit,
    val filterIcon: @Composable () -> Unit,
)

/** Local provider for header cell info. Null outside of TableHeader. */
public val LocalTableHeaderCellInfo: ProvidableCompositionLocal<TableHeaderCellInfo<Any?>?> =
    compositionLocalOf { null }

/** Local provider for header icons (scoped per Table instance). */
public val LocalTableHeaderIcons: ProvidableCompositionLocal<TableHeaderIcons> =
    staticCompositionLocalOf {
        TableHeaderIcons(
            sortAsc = Icons.Rounded.ArrowUpward,
            sortDesc = Icons.Rounded.ArrowDownward,
            sortNeutral = Icons.AutoMirrored.Outlined.Sort,
            filterActive = Icons.Rounded.FilterAlt,
            filterInactive = Icons.Outlined.FilterAlt,
        )
    }

/** Helper that renders a sort icon with proper state and toggles sort on click. */
@Composable
public fun TableHeaderSortIcon() {
    val info = LocalTableHeaderCellInfo.current ?: return
    if (!info.isSortable) return
    info.sortIcon()
}

/** Helper that renders a filter icon and embedded FilterPanel when expanded. */
@Composable
public fun TableHeaderFilterIcon() {
    val info = LocalTableHeaderCellInfo.current ?: return
    if (!info.hasFilter) return
    info.filterIcon()
}
