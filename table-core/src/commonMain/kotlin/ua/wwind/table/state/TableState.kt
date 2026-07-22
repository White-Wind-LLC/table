package ua.wwind.table.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableDimensions
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState

private val settingsLogger = Logger.withTag("TableSettings")

/** Current sort state: which [column] is sorted and in which [order]. */
@Immutable
public data class SortState<C>(
    val column: C,
    val order: SortOrder,
)

/** Column width change request used by resizing logic. */
public sealed interface ColumnWidthAction {
    public data class Set(
        val width: Dp,
    ) : ColumnWidthAction

    public data object Reset : ColumnWidthAction
}

/**
 * Mutable state holder for a table instance.
 *
 * Three clusters of that state live in holders of their own, reached through this one:
 * [columns] (order, widths, auto-fit measurements), [selection] (focused row, checked rows,
 * selected cell) and [editing] (edited row and column, edit callbacks). What shapes the data —
 * [sort], [groupBy], [filters] — stays here, because it is what a table is asked for most and it
 * belongs to the table as a whole rather than to one of the three.
 *
 * The members those holders took over remain here as deprecated forwarders for one release, each
 * naming its replacement, so existing call sites keep compiling; they are removed in the next major.
 */
@Stable
public class TableState<C>
    internal constructor(
        initialColumns: List<C>,
        initialSort: SortState<C>?,
        initialOrder: List<C>,
        initialWidths: Map<C, Dp>,
        public val settings: TableSettings,
        public val dimensions: TableDimensions,
    ) {
        /** Column order, width overrides and the measurements auto-fit works from. */
        public val columns: TableColumnsState<C> =
            TableColumnsState(
                initialOrder = initialOrder.ifEmpty { initialColumns },
                initialWidths = initialWidths,
                dimensions = dimensions,
            )

        /** Focused row, checked rows and the selected cell. */
        public val selection: TableSelectionState<C> = TableSelectionState(settings)

        /** Edited row and column, and the consumer's edit callbacks. */
        public val editing: TableEditingState<C> = TableEditingState(columns, selection)

        /**
         * Current visible columns list. Must be set externally before tableWidth is accessed.
         *
         * Snapshot state, because [tableWidth] derives from it: a plain field would be an untracked
         * read, leaving the derived width frozen at whatever the first composition computed until
         * some *other* tracked read — a [TableColumnsState.widths] write — happened to invalidate it.
         */
        internal var visibleColumns: List<ColumnSpec<*, C, *>> by mutableStateOf(emptyList())

        /**
         * Row-to-unit mapping for the current data set. Identity unless the consumer passed
         * `rowBlocks`. Assigned by `Table` during composition, read by scroll/keyboard effects.
         *
         * Snapshot state, because the prefetcher reads it inside a `snapshotFlow`, which re-evaluates
         * only on a tracked read. As a plain field it would be carried along by the `layoutInfo` read
         * next to it — correct only for as long as something else keeps scrolling.
         *
         * `Table` holds the index in `remember`, and [RowUnitIndex] compares by identity, so
         * re-assigning the same instance each recomposition is a no-op and only a genuine rebuild
         * notifies.
         */
        internal var rowUnits: RowUnitIndex by mutableStateOf(RowUnitIndex.identity(0))

        /**
         * True while a non-null `rowBlocks` declaration is being ignored because [groupBy] is
         * active. The two features describe incompatible structures over the same rows, so the
         * library suppresses blocks instead of rendering both; this flag exists so consumers can
         * surface that conflict in their own UI rather than have blocks vanish unexplained.
         */
        public var rowBlocksSuppressedByGroupBy: Boolean by mutableStateOf(false)
            internal set

        /**
         * True while the table derives at least one row block. The column menu reads it to disable
         * the group-by action: activating `groupBy` would silently suppress visible blocks, and a
         * disabled item surfaces that instead of letting the feature disappear on a menu click.
         */
        internal var rowBlocksNonEmpty: Boolean by mutableStateOf(false)

        /**
         * Current table width computed from visible columns and their widths.
         * Automatically recalculates when column order, widths, or visibleColumns change.
         */
        public val tableWidth: Dp by derivedStateOf {
            computeTableWidth(visibleColumns)
        }

        /**
         * Computes the total table width from visible columns and dividers.
         */
        private fun computeTableWidth(columnSpecs: List<ColumnSpec<*, C, *>>): Dp {
            val effectivePinnedCount =
                if (settings.pinnedColumnsCount >= columnSpecs.size) 0 else settings.pinnedColumnsCount

            // Sum column widths (use stored widths, spec width or default)
            val columnsTotal: Dp =
                columnSpecs.fold(0.dp) { acc, spec ->
                    val w = columns.resolveWidth(spec.key, spec)
                    acc + w
                }

            // Calculate divider contribution:
            // - If there are no pinned columns: each column has its regular divider (count = columns)
            // - If there are pinned columns: all but one divider are regular, and one between pinned and scrollable is
            //   thicker
            val dividerTotal: Dp =
                if (effectivePinnedCount == 0) {
                    dimensions.dividerThickness * columnSpecs.size
                } else {
                    // total dividers = columns count, but one of them uses pinnedColumnDividerThickness
                    dimensions.dividerThickness * (columnSpecs.size - 1) + dimensions.pinnedColumnDividerThickness
                }

            return columnsTotal + dividerTotal
        }

        // Sorting
        public var sort: SortState<C>? by mutableStateOf(initialSort)
            private set

        // Grouping by column
        public var groupBy: C? by mutableStateOf(null)
            private set

        // Filters per column
        public val filters: MutableMap<C, TableFilterState<*>> =
            mutableStateMapOf<C, TableFilterState<*>>()

        // Cell selection
        public data class SelectedCell<C>(
            val rowIndex: Int,
            val column: C,
        )

        /**
         * Toggle or set sorting for a [column]. If [order] is null, cycles ASC -> DESC -> none.
         *
         * A no-op while row reorder is enabled, mirroring the `initialSort` normalization in
         * [rememberTableState]: under an active sort the view is invariant to source permutations,
         * so drag history would be unobservable — the two features cannot both apply.
         */
        public fun setSort(
            column: C,
            order: SortOrder? = null,
        ) {
            if (settings.rowReorderEnabled) {
                settingsLogger.w { "rowReorderEnabled is incompatible with sorting; setSort is ignored." }
                return
            }
            sort =
                if (order != null) {
                    SortState(column, order)
                } else {
                    val current = sort
                    if (current == null || current.column != column) {
                        SortState(column, SortOrder.ASCENDING)
                    } else {
                        when (current.order) {
                            SortOrder.ASCENDING -> SortState(column, SortOrder.DESCENDING)
                            SortOrder.DESCENDING -> null
                        }
                    }
                }
        }

        /** Enable or disable grouping by a [column] */
        public fun groupBy(column: C?) {
            groupBy = column
        }

        /** Set or clear filter [state] for [column]. Pass null to remove. */
        @Suppress("UNCHECKED_CAST")
        public fun <T> setFilter(
            column: C,
            state: TableFilterState<T>?,
        ) {
            if (state == null) {
                filters.remove(column)
            } else {
                filters[column] = state as TableFilterState<*>
            }
        }

        /** Tracks measured row heights in raw pixels for dynamic, precise scrolling. */
        public val rowHeightsPx: SnapshotStateMap<Int, Int> = mutableStateMapOf()

        /** Record measured row height (in px) for [index]. */
        public fun updateRowHeight(
            index: Int,
            heightPx: Int,
        ) {
            val current = rowHeightsPx[index]
            if (current == null || current != heightPx) {
                rowHeightsPx[index] = heightPx
            }
        }

        /**
         * Internal helper for selecting a cell from contexts where the column type is erased. Used by
         * [ua.wwind.table.component.TableCellTextField] when updating selectedCell on focus change.
         *
         * Coordination between two holders, so it lives here rather than in either of them.
         */
        internal fun selectCellUnchecked(
            rowIndex: Int,
            column: Any,
        ) {
            selection.selectCellUnchecked(rowIndex, column)
            editing.followSelectedColumn()
        }

        /**
         * Follows a committed block drag: positional runtime state must keep pointing at the rows
         * it pointed at before the permutation, or selection and editing silently jump to whichever
         * rows landed on the old positions.
         */
        internal fun remapRowPositions(remap: (Int) -> Int) {
            selection.remapPositions(remap)
            editing.remapRow(remap)
        }

        // ---------------------------------------------------------------------------------------
        // Deprecated forwarders. Each member below moved to one of the three holders above in
        // 2.1.0 and is kept for one release so existing call sites compile with a warning naming
        // the replacement. All of them are removed in the next major.
        // ---------------------------------------------------------------------------------------

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.order.",
            ReplaceWith("columns.order"),
        )
        public val columnOrder: SnapshotStateList<C> get() = columns.order

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.widths.",
            ReplaceWith("columns.widths"),
        )
        public val columnWidths: SnapshotStateMap<C, Dp> get() = columns.widths

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.contentMaxWidths.",
            ReplaceWith("columns.contentMaxWidths"),
        )
        public val columnContentMaxWidths: SnapshotStateMap<C, Dp> get() = columns.contentMaxWidths

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.headerWidths.",
            ReplaceWith("columns.headerWidths"),
        )
        public val columnHeaderWidths: SnapshotStateMap<C, Dp> get() = columns.headerWidths

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.autoWidthAppliedForEmpty.",
            ReplaceWith("columns.autoWidthAppliedForEmpty"),
        )
        public var autoWidthAppliedForEmpty: Boolean
            get() = columns.autoWidthAppliedForEmpty
            set(value) {
                columns.autoWidthAppliedForEmpty = value
            }

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.autoWidthAppliedForData.",
            ReplaceWith("columns.autoWidthAppliedForData"),
        )
        public var autoWidthAppliedForData: Boolean
            get() = columns.autoWidthAppliedForData
            set(value) {
                columns.autoWidthAppliedForData = value
            }

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.resolveWidth(key, spec).",
            ReplaceWith("columns.resolveWidth(key, spec)"),
        )
        public fun resolveColumnWidth(
            key: C,
            spec: ColumnSpec<*, C, *>? = null,
        ): Dp = columns.resolveWidth(key, spec)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.move(fromIndex, toIndex).",
            ReplaceWith("columns.move(fromIndex, toIndex)"),
        )
        public fun moveColumn(
            fromIndex: Int,
            toIndex: Int,
        ): Unit = columns.move(fromIndex, toIndex)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.setOrder(newOrder).",
            ReplaceWith("columns.setOrder(newOrder)"),
        )
        public fun setColumnOrder(newOrder: List<C>): Unit = columns.setOrder(newOrder)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.resize(column, action).",
            ReplaceWith("columns.resize(column, action)"),
        )
        public fun resizeColumn(
            column: C,
            action: ColumnWidthAction,
        ): Unit = columns.resize(column, action)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.setWidths(widths).",
            ReplaceWith("columns.setWidths(widths)"),
        )
        public fun setColumnWidths(widths: Map<C, Dp?>): Unit = columns.setWidths(widths)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.updateMaxContentWidth(column, width, source).",
            ReplaceWith("columns.updateMaxContentWidth(column, width, source)"),
        )
        public fun updateMaxContentWidth(
            column: C,
            width: Dp,
            source: String,
        ): Unit = columns.updateMaxContentWidth(column, width, source)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.fitToContent(column).",
            ReplaceWith("columns.fitToContent(column)"),
        )
        public fun setColumnWidthToMaxContent(column: C): Unit = columns.fitToContent(column)

        @Deprecated(
            "Column state moved to TableState.columns. Use columns.recalculateAutoWidths().",
            ReplaceWith("columns.recalculateAutoWidths()"),
        )
        public fun recalculateAutoWidths(): Unit = columns.recalculateAutoWidths()

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.selectedIndex.",
            ReplaceWith("selection.selectedIndex"),
        )
        public val selectedIndex: Int? get() = selection.selectedIndex

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.checkedIndices.",
            ReplaceWith("selection.checkedIndices"),
        )
        public val checkedIndices: SnapshotStateList<Int> get() = selection.checkedIndices

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.selectedCell.",
            ReplaceWith("selection.selectedCell"),
        )
        public val selectedCell: SelectedCell<C>? get() = selection.selectedCell

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.toggleRow(index).",
            ReplaceWith("selection.toggleRow(index)"),
        )
        public fun toggleSelect(index: Int): Unit = selection.toggleRow(index)

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.focusRow(index).",
            ReplaceWith("selection.focusRow(index)"),
        )
        public fun focusRow(index: Int): Unit = selection.focusRow(index)

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.toggleCheck(index).",
            ReplaceWith("selection.toggleCheck(index)"),
        )
        public fun toggleCheck(index: Int): Unit = selection.toggleCheck(index)

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.toggleCheckAll(count).",
            ReplaceWith("selection.toggleCheckAll(count)"),
        )
        public fun toggleCheckAll(count: Int): Unit = selection.toggleCheckAll(count)

        @Deprecated(
            "Selection state moved to TableState.selection. Use selection.selectCell(rowIndex, column).",
            ReplaceWith("selection.selectCell(rowIndex, column)"),
        )
        public fun selectCell(
            rowIndex: Int,
            column: C,
        ): Unit = selection.selectCell(rowIndex, column)

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.rowIndex.",
            ReplaceWith("editing.rowIndex"),
        )
        public val editingRow: Int? get() = editing.rowIndex

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.column.",
            ReplaceWith("editing.column"),
        )
        public val editingColumn: C? get() = editing.column

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.onRowEditStart.",
            ReplaceWith("editing.onRowEditStart"),
        )
        public val onRowEditStart: ((rowIndex: Int) -> Unit)? get() = editing.onRowEditStart

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.onRowEditComplete.",
            ReplaceWith("editing.onRowEditComplete"),
        )
        public val onRowEditComplete: ((rowIndex: Int) -> Boolean)? get() = editing.onRowEditComplete

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.onEditCancel.",
            ReplaceWith("editing.onEditCancel"),
        )
        public val onEditCancel: ((rowIndex: Int) -> Unit)? get() = editing.onEditCancel

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.start(item, rowIndex, column).",
            ReplaceWith("editing.start(item, rowIndex, column)"),
        )
        public fun startEditing(
            item: Any?,
            rowIndex: Int,
            column: C,
        ): Boolean = editing.start(item, rowIndex, column)

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.tryComplete().",
            ReplaceWith("editing.tryComplete()"),
        )
        public fun tryCompleteEditing(): Boolean = editing.tryComplete()

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.completeCurrentCell(visibleColumns).",
            ReplaceWith("editing.completeCurrentCell(visibleColumns)"),
        )
        public fun completeCurrentCellEdit(visibleColumns: List<ColumnSpec<*, C, *>>): Unit =
            editing.completeCurrentCell(visibleColumns)

        @Deprecated(
            "Editing state moved to TableState.editing. Use editing.cancel().",
            ReplaceWith("editing.cancel()"),
        )
        public fun cancelEditing(): Unit = editing.cancel()
    }

public inline fun <T, R : Any> Iterable<T>.mapNotNullToImmutable(transform: (T) -> R?): ImmutableList<R> =
    buildList {
        for (item in this@mapNotNullToImmutable) {
            transform(item)?.let(::add)
        }
    }.toPersistentList()

/**
 * Remember and create a [TableState] tied to the composition. Initial parameters are used only
 * once; runtime mutations will not recreate the state.
 */
@Composable
@Suppress("LongParameterList")
public fun <C> rememberTableState(
    columns: ImmutableList<C>,
    initialSort: SortState<C>? = null,
    initialOrder: ImmutableList<C> = columns,
    initialWidths: ImmutableMap<C, Dp> = persistentMapOf(),
    settings: TableSettings = TableSettings(),
    dimensions: TableDimensions = TableDefaults.standardDimensions(),
): TableState<C> {
    // Important: Do not include initialOrder/initialWidths/initialSort in the remember keys.
    // These parameters should only be used for initial state, not for triggering state recreation
    // on every reorder/resize/sort. Recreating the state would wipe runtime data such as filters.
    return remember(columns, settings, dimensions) {
        val normalized = normalizeTableStateInput(settings, initialSort)
        if (normalized.warnings.isNotEmpty()) {
            settingsLogger.w { normalized.warnings.joinToString("; ") }
        }
        TableState(
            initialColumns = columns,
            initialSort = normalized.initialSort,
            initialOrder = initialOrder,
            initialWidths = initialWidths,
            settings = normalized.settings,
            dimensions = dimensions,
        )
    }
}

private data class NormalizedTableStateInput<C>(
    val settings: TableSettings,
    val initialSort: SortState<C>?,
    val warnings: List<String>,
)

private fun <C> normalizeTableStateInput(
    settings: TableSettings,
    initialSort: SortState<C>?,
): NormalizedTableStateInput<C> {
    if (!settings.rowReorderEnabled) {
        return NormalizedTableStateInput(settings = settings, initialSort = initialSort, warnings = emptyList())
    }

    val warnings = mutableListOf<String>()
    var normalizedInitialSort = initialSort

    if (initialSort != null) {
        normalizedInitialSort = null
        warnings += "rowReorderEnabled is incompatible with initialSort; initialSort is ignored."
    }

    return NormalizedTableStateInput(
        settings = settings,
        initialSort = normalizedInitialSort,
        warnings = warnings,
    )
}
