package ua.wwind.table.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.unit.Dp
import co.touchlab.kermit.Logger
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.header.computeReorderMove
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDimensions

private val logger = Logger.withTag("TableAutoWidth")

/**
 * Column layout of one table: the order columns render in, the width overrides that apply to them,
 * and the measurements the auto-fit works from.
 *
 * Reached as [TableState.columns]. Every member here has a deprecated forwarder on [TableState]
 * itself, kept for one release so existing call sites compile with a warning that names the
 * replacement; the forwarders go away in the next major.
 *
 * `TooManyFunctions` is suppressed because this is the single holder for column layout operations
 * (order, widths, pinning, visibility); splitting it would scatter one concept.
 */
@Suppress("TooManyFunctions")
@Stable
public class TableColumnsState<C>
    internal constructor(
        initialOrder: List<C>,
        initialWidths: Map<C, Dp>,
        private val dimensions: TableDimensions,
        initialPinnedCount: Int = 0,
        private val pinnedSide: PinnedSide = PinnedSide.Start,
    ) {
        /** Column keys in render order. */
        public val order: SnapshotStateList<C> =
            mutableStateListOf<C>().apply { addAll(initialOrder) }

        /**
         * Width overrides per column. A missing key falls back to the spec width, then the default.
         *
         * Holds both explicit widths (from `initialWidths`, [setWidths], [resize], a resizer drag or a
         * direct write) and the widths auto-fit chose for `autoWidth` columns; [explicitWidths] holds
         * only the former. Auto-fit never overwrites an explicit width, except through
         * [recalculateAutoWidths].
         */
        public val widths: SnapshotStateMap<C, Dp> =
            mutableStateMapOf<C, Dp>().apply { putAll(initialWidths) }

        /** The width auto-fit last wrote per column; a [widths] entry that differs from it is explicit. */
        private val autoFitWidths: SnapshotStateMap<C, Dp> = mutableStateMapOf()

        /**
         * Whether the next auto-fit pass also replaces explicit widths of `autoWidth` columns. Set by
         * [recalculateAutoWidths].
         */
        internal var autoFitOverridesExplicit: Boolean by mutableStateOf(false)

        /**
         * The entries of [widths] that were set explicitly rather than by auto-fit: the widths to persist
         * and pass back as `initialWidths` or through [setWidths] when the table is shown again.
         */
        public val explicitWidths: Map<C, Dp>
            get() = widths.filter { (key, width) -> autoFitWidths[key] != width }

        /** Whether [column] has an explicit width, which auto-fit leaves alone. */
        internal fun hasExplicitWidth(column: C): Boolean {
            val width = widths[column] ?: return false
            return autoFitWidths[column] != width
        }

        /** Whether the auto-fit pass sizes [column]: it has no explicit width, or the pass overrides them. */
        internal fun needsAutoFit(column: C): Boolean = autoFitOverridesExplicit || !hasExplicitWidth(column)

        /** Write widths chosen by auto-fit, so they are not counted as explicit. */
        internal fun applyAutoFit(newWidths: Map<C, Dp>) {
            widths.putAll(newWidths)
            autoFitWidths.putAll(newWidths)
        }

        /**
         * Tracks the maximum measured minimal content width per column across visible rows. Used to
         * auto-fit columns on demand.
         */
        public val contentMaxWidths: SnapshotStateMap<C, Dp> = mutableStateMapOf()

        /**
         * Tracks header widths separately. These are preserved during auto-width recalculation
         * and used as base values after reset.
         */
        public val headerWidths: SnapshotStateMap<C, Dp> = mutableStateMapOf()

        /** Whether automatic width fitting has been applied for the empty (header-only) state. */
        public var autoWidthAppliedForEmpty: Boolean by mutableStateOf(false)

        /** Whether automatic width fitting has been applied for the first data batch render. */
        public var autoWidthAppliedForData: Boolean by mutableStateOf(false)

        /**
         * Resolves the effective width for a column given its key and optional spec.
         *
         * Resolution priority:
         * 1. Explicit or auto-fit width from [widths]
         * 2. Spec-defined width from [spec]
         * 3. Default width from [TableDimensions.defaultColumnWidth]
         *
         * @param key column key
         * @param spec optional column spec; if null, only [widths] and default are considered
         * @return effective column width
         */
        public fun resolveWidth(
            key: C,
            spec: ColumnSpec<*, C, *>? = null,
        ): Dp = widths[key] ?: spec?.width ?: dimensions.defaultColumnWidth

        /**
         * Move a column from [fromIndex] to [toIndex] within the current order. Indices are validated;
         * dropping after the last element is supported.
         */
        public fun move(
            fromIndex: Int,
            toIndex: Int,
        ) {
            // Guard against invalid indices and no-op moves
            if (order.size < 2) return
            if (fromIndex !in order.indices) return

            // Allow dropping after the last element (append)
            var targetIndex = toIndex.coerceIn(0, order.size)
            if (fromIndex == targetIndex || fromIndex == targetIndex - 1) return

            val column = order.removeAt(fromIndex)
            // After removal, adjust target when moving forward
            if (targetIndex > fromIndex) targetIndex--
            order.add(targetIndex, column)
        }

        /**
         * Replace current column order with [newOrder]. Missing keys are ignored; unknown keys
         * appended.
         */
        public fun setOrder(newOrder: List<C>) {
            val current = order.toList()
            val filtered = newOrder.filter { current.contains(it) }
            val remaining = current.filterNot { filtered.contains(it) }
            order.clear()
            order.addAll(filtered + remaining)
        }

        /** Apply a width [action] for a [column] (set or reset override). */
        public fun resize(
            column: C,
            action: ColumnWidthAction,
        ) {
            when (action) {
                is ColumnWidthAction.Set -> {
                    widths[column] = action.width
                }

                ColumnWidthAction.Reset -> {
                    widths.remove(column)
                    autoFitWidths.remove(column)
                }
            }
        }

        /** Apply external [newWidths] in bulk. Null width removes the override for that column. */
        public fun setWidths(newWidths: Map<C, Dp?>) {
            newWidths.forEach { (col, width) ->
                if (width == null) {
                    widths.remove(col)
                    autoFitWidths.remove(col)
                } else {
                    widths[col] = width
                }
            }
        }

        /**
         * Number of pinned columns, counted among visible columns from the
         * [ua.wwind.table.config.TableSettings.pinnedColumnsSide] edge. Starts at
         * [ua.wwind.table.config.TableSettings.pinnedColumnsCount]; [pin] and [unpin] change it at
         * runtime. Pinning every visible column pins none.
         */
        public var pinnedCount: Int by mutableIntStateOf(initialPinnedCount.coerceAtLeast(0))
            private set

        /**
         * Keys of the columns whose spec is `visible`, ignoring [hidden]. `Table` sets it from the
         * column specs it composes; before the first composition it is `null` and every key in
         * [order] counts.
         */
        internal var specVisibleKeys: Set<C>? by mutableStateOf(null)

        /** Keys of the visible columns in render order: spec-visible and not in [hidden]. */
        internal fun visibleKeys(): List<C> {
            val specVisible = specVisibleKeys
            return order.filter { (specVisible == null || it in specVisible) && it !in hidden }
        }

        /**
         * Columns hidden at runtime, e.g. from the column menu. A column renders when its spec is
         * `visible` and its key is not in this set; [showAll] never reveals a column the spec hides.
         */
        public val hidden: SnapshotStateSet<C> = mutableStateSetOf()

        /** Whether [hide] would hide [column]: it is visible and not the last visible column. */
        public fun canHide(column: C): Boolean {
            val keys = visibleKeys()
            return column in keys && keys.size > 1
        }

        /**
         * Hide [column]. A pinned column is unpinned first: it moves just outside the pinned block,
         * which shrinks by one.
         */
        public fun hide(column: C) {
            if (!canHide(column)) return
            unpin(column)
            hidden.add(column)
        }

        /**
         * Show a column hidden by [hide]. A column whose place in [order] falls inside the pinned
         * block lands at the block's outer edge instead, so it never takes a pinned column's slot.
         */
        public fun show(column: C) {
            if (column !in hidden) return
            reveal(column)
        }

        /**
         * Show every column hidden by [hide], each one placed as [show] places it. Columns that all
         * land at the pinned block's outer edge keep their relative order.
         */
        public fun showAll() {
            val toShow = order.filter { it in hidden }
            // Each column lands at the edge, ahead of those placed before it on the left, behind them
            // on the right; walking against that direction keeps the original order.
            val sequence = if (pinnedSide == PinnedSide.Start) toShow.asReversed() else toShow
            sequence.forEach { reveal(it) }
        }

        /**
         * Removes [column] from [hidden] and moves it out of the pinned block if it would land
         * inside. A column the spec hides stays hidden, so it is only removed from [hidden].
         */
        private fun reveal(column: C) {
            hidden.remove(column)
            val keys = visibleKeys()
            if (column !in keys) return
            val pinned = effectivePinnedCount(keys.size)
            val index = keys.indexOf(column)
            // The block's outer edge, when the column would otherwise land inside the block.
            val target =
                when (pinnedSide) {
                    PinnedSide.Start -> pinned.takeIf { index < pinned }
                    PinnedSide.End -> (keys.size - pinned - 1).takeIf { index >= keys.size - pinned }
                } ?: return
            moveVisible(keys, index, target)
        }

        /** The pinned block size as rendered: a count covering every visible column pins none. */
        internal fun effectivePinnedCount(visibleCount: Int = visibleKeys().size): Int =
            if (pinnedCount >= visibleCount) 0 else pinnedCount

        /** Whether [column] is visible and inside the pinned block. */
        public fun isPinned(column: C): Boolean {
            val keys = visibleKeys()
            val index = keys.indexOf(column)
            if (index < 0) return false
            val pinned = effectivePinnedCount(keys.size)
            return when (pinnedSide) {
                PinnedSide.Start -> index < pinned
                PinnedSide.End -> index >= keys.size - pinned
            }
        }

        /** Whether [pin] would pin [column]: it is visible, unpinned, and one column would stay unpinned. */
        public fun canPin(column: C): Boolean {
            val keys = visibleKeys()
            return column in keys && !isPinned(column) && effectivePinnedCount(keys.size) + 1 < keys.size
        }

        /** Move [column] to the inner edge of the pinned block and grow the block by one. */
        public fun pin(column: C) {
            if (!canPin(column)) return
            val keys = visibleKeys()
            val pinned = effectivePinnedCount(keys.size)
            val target =
                when (pinnedSide) {
                    PinnedSide.Start -> pinned
                    PinnedSide.End -> keys.size - pinned - 1
                }
            moveVisible(keys, keys.indexOf(column), target)
            pinnedCount = pinned + 1
        }

        /** Move [column] just outside the pinned block and shrink the block by one. */
        public fun unpin(column: C) {
            if (!isPinned(column)) return
            val keys = visibleKeys()
            val pinned = effectivePinnedCount(keys.size)
            val target =
                when (pinnedSide) {
                    PinnedSide.Start -> pinned - 1
                    PinnedSide.End -> keys.size - pinned
                }
            moveVisible(keys, keys.indexOf(column), target)
            pinnedCount = pinned - 1
        }

        /**
         * Whether [moveBy] would move [column] by [delta] visible positions: the target exists and
         * sits in the same block (pinned or unpinned) as the column.
         */
        public fun canMoveBy(
            column: C,
            delta: Int,
        ): Boolean {
            val keys = visibleKeys()
            val from = keys.indexOf(column)
            if (from < 0 || delta == 0) return false
            val to = from + delta
            return to in keys.indices && isPinned(column) == isPinned(keys[to])
        }

        /** Move [column] by [delta] positions among visible columns, staying inside its block. */
        public fun moveBy(
            column: C,
            delta: Int,
        ) {
            if (!canMoveBy(column, delta)) return
            val keys = visibleKeys()
            val from = keys.indexOf(column)
            moveVisible(keys, from, from + delta)
        }

        /** Moves the visible column at [from] to visible index [to], mapping both onto [order]. */
        private fun moveVisible(
            keys: List<C>,
            from: Int,
            to: Int,
        ) {
            if (from == to) return
            val step = computeReorderMove(from, to, order.toList(), keys) ?: return
            move(step.first, step.second)
        }

        /**
         * Update the tracked maximum minimal content width for a [column]. If the provided [width] is
         * greater than the stored value, it will be recorded.
         *
         * @param column column key
         * @param width measured content width
         * @param source description of the measurement source (e.g. "Header" or "Row[5]")
         */
        public fun updateMaxContentWidth(
            column: C,
            width: Dp,
            source: String,
        ) {
            // Store header widths separately for preservation during reset
            if (source == "Header") {
                val currentHeader = headerWidths[column]
                if (currentHeader == null || width > currentHeader) {
                    logger.v { "AutoWidth: header column=$column updated $currentHeader -> $width" }
                    headerWidths[column] = width
                }
            }

            val current = contentMaxWidths[column]
            if (current == null || width > current) {
                logger.v { "AutoWidth: column=$column updated $current -> $width from $source" }
                contentMaxWidths[column] = width
            }
        }

        /**
         * Set the column width override to the tracked maximum minimal content width (if available).
         * No-op if no measured width is present for the [column].
         */
        public fun fitToContent(column: C) {
            val width = contentMaxWidths[column] ?: return
            widths[column] = width
        }

        /**
         * Recalculate auto-widths for columns with `autoWidth` enabled.
         *
         * This method is useful for scenarios with deferred/paginated data loading where initial
         * auto-width calculation happened on empty data. After data loads and content is measured, call
         * this method to recompute column widths based on the actual content.
         *
         * Header widths are preserved and used as base values for new measurements. Unlike the
         * automatic pass, the recalculation also replaces explicit widths of `autoWidth` columns.
         */
        public fun recalculateAutoWidths() {
            logger.v {
                "AutoWidth: reset flags, clearing ${contentMaxWidths.size} measured widths, " +
                    "preserving ${headerWidths.size} header widths"
            }
            // Reset flags to allow ApplyAutoWidthEffect to recompute on next frame
            autoWidthAppliedForEmpty = false
            autoWidthAppliedForData = false
            autoFitOverridesExplicit = true
            // Clear row measurements but preserve header widths
            contentMaxWidths.clear()
            // Initialize with header widths as base values
            contentMaxWidths.putAll(headerWidths)
        }
    }
