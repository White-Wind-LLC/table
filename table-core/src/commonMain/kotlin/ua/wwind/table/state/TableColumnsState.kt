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
        private val pinnedSide: PinnedSide = PinnedSide.Left,
    ) {
        /** Column keys in render order. */
        public val order: SnapshotStateList<C> =
            mutableStateListOf<C>().apply { addAll(initialOrder) }

        /** Width overrides per column. A missing key falls back to the spec width, then the default. */
        public val widths: SnapshotStateMap<C, Dp> =
            mutableStateMapOf<C, Dp>().apply { putAll(initialWidths) }

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
         * 1. User-resized width from [widths]
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
                is ColumnWidthAction.Set -> widths[column] = action.width
                ColumnWidthAction.Reset -> widths.remove(column)
            }
        }

        /** Apply external [newWidths] in bulk. Null width removes the override for that column. */
        public fun setWidths(newWidths: Map<C, Dp?>) {
            newWidths.forEach { (col, width) ->
                if (width == null) widths.remove(col) else widths[col] = width
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
         * Keys of the columns the table renders, in order. `TableState` wires this to the columns
         * `Table` composed; before the first composition it falls back to [order].
         *
         * The composed list lags one frame behind [hide], so [visibleKeys] also filters [hidden].
         */
        internal var renderedKeys: (() -> List<C>)? = null

        internal fun visibleKeys(): List<C> =
            renderedKeys?.invoke()?.filterNot { it in hidden }?.takeIf { it.isNotEmpty() }
                ?: order.filterNot { it in hidden }

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
            reveal(column, visibleKeys())
        }

        /** Show every column hidden by [hide], each one placed as [show] places it. */
        public fun showAll() {
            var visible = visibleKeys()
            order.filter { it in hidden }.forEach { visible = reveal(it, visible) }
            hidden.clear()
        }

        /**
         * Removes [column] from [hidden] and moves it out of the pinned block if it would land
         * inside. [visible] is the visible list without [column]: the rendered keys lag a frame, so
         * they cannot be read back here. Returns the visible list including [column].
         */
        private fun reveal(
            column: C,
            visible: List<C>,
        ): List<C> {
            hidden.remove(column)
            val visibleSet = visible.toSet()
            val keys = order.filter { it == column || it in visibleSet }
            val pinned = effectivePinnedCount(keys.size)
            val index = keys.indexOf(column)
            // The block's outer edge, when the column would otherwise land inside the block.
            val target =
                when (pinnedSide) {
                    PinnedSide.Left -> pinned.takeIf { index < pinned }
                    PinnedSide.Right -> (keys.size - pinned - 1).takeIf { index >= keys.size - pinned }
                } ?: return keys
            moveVisible(keys, index, target)
            return order.filter { it == column || it in visibleSet }
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
                PinnedSide.Left -> index < pinned
                PinnedSide.Right -> index >= keys.size - pinned
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
                    PinnedSide.Left -> pinned
                    PinnedSide.Right -> keys.size - pinned - 1
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
                    PinnedSide.Left -> pinned - 1
                    PinnedSide.Right -> keys.size - pinned
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
         * Header widths are preserved and used as base values for new measurements.
         */
        public fun recalculateAutoWidths() {
            logger.v {
                "AutoWidth: reset flags, clearing ${contentMaxWidths.size} measured widths, " +
                    "preserving ${headerWidths.size} header widths"
            }
            // Reset flags to allow ApplyAutoWidthEffect to recompute on next frame
            autoWidthAppliedForEmpty = false
            autoWidthAppliedForData = false
            // Clear row measurements but preserve header widths
            contentMaxWidths.clear()
            // Initialize with header widths as base values
            contentMaxWidths.putAll(headerWidths)
        }
    }
