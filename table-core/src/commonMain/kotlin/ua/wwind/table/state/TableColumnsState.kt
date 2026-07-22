package ua.wwind.table.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.Dp
import co.touchlab.kermit.Logger
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDimensions

private val logger = Logger.withTag("TableAutoWidth")

/**
 * Column layout of one table: the order columns render in, the width overrides that apply to them,
 * and the measurements the auto-fit works from.
 *
 * Reached as [TableState.columns]. Every member here has a deprecated forwarder on [TableState]
 * itself, kept for one release so existing call sites compile with a warning that names the
 * replacement; the forwarders go away in the next major.
 */
@Stable
public class TableColumnsState<C>
    internal constructor(
        initialOrder: List<C>,
        initialWidths: Map<C, Dp>,
        private val dimensions: TableDimensions,
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
