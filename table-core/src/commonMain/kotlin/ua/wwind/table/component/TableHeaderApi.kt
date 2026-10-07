package ua.wwind.table.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.data.SortOrder
import ua.wwind.table.icon.TableIcons

/** Icons used by table header for sort and filter affordances. */
@Immutable
public data class TableHeaderIcons(
    val sortAsc: Painter,
    val sortDesc: Painter,
    val sortNeutral: Painter,
    val filterActive: Painter,
    val filterInactive: Painter,
)

public object TableHeaderDefaults {
    /**
     * Factory for [TableHeaderIcons] with sensible defaults. Any [Painter] fits a slot:
     * `painterResource(TableIcons.X)`, a resource of your own, or `rememberVectorPainter(imageVector)`.
     */
    @Composable
    public fun icons(
        sortAsc: Painter = painterResource(TableIcons.ArrowUpward),
        sortDesc: Painter = painterResource(TableIcons.ArrowDownward),
        sortNeutral: Painter = painterResource(TableIcons.Sort),
        filterActive: Painter = painterResource(TableIcons.FilterAltFilled),
        filterInactive: Painter = painterResource(TableIcons.FilterAltOutlined),
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
@Stable
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

/**
 * Local provider for header icons (scoped per Table instance). A resource painter exists only inside
 * composition, so there is no static default; `Table` always provides one.
 */
public val LocalTableHeaderIcons: ProvidableCompositionLocal<TableHeaderIcons> =
    staticCompositionLocalOf { error("TableHeaderIcons are provided by Table") }

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
