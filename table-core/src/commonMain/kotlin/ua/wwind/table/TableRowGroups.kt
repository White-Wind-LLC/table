package ua.wwind.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

/**
 * Row grouping for the table: which runs of adjacent rows drag as one unit, what happens when one is
 * moved, and what is drawn in the band above each block.
 *
 * Hold the instance in `remember`, e.g.
 * `remember(rows) { TableRowGroups(rows.rowGroupsOf { it.groupId }) }`. This type compares by
 * identity — carrying a `@Composable` slot, it has no meaningful structural equality — so a fresh
 * instance on each recomposition makes the `rowGroups` argument look changed and stops `Table` from
 * skipping. Keying the `remember` on the rendered data also keeps [ranges] in step with it.
 */
@Stable
public class TableRowGroups(
    /**
     * Ranges of adjacent rows that drag as one unit. Must be sorted, non-empty, non-overlapping and
     * within the table's `itemsCount`. Derive them from the same data the table renders — see
     * [rowGroupsOf].
     */
    public val ranges: ImmutableList<IntRange>,
    /**
     * Called when a unit is moved, with row ranges rather than single indices; `from.first` is the
     * leader row. Required when [ranges] is non-empty and row reorder is enabled.
     *
     * In a scrolling table this fires **during** the drag and the consumer must apply the move to its
     * list synchronously (see [moveRowGroup]); in an `embedded` table it fires once on drop.
     *
     * Do NOT collapse `to` to a single index: for a downward move the block is inserted after
     * `to.last`, so passing only the target's leader row would drop the block inside the target
     * group and split it.
     */
    public val onMove: ((from: IntRange, to: IntRange) -> Unit)? = null,
    /**
     * Optional content rendered in the band above each group block, pinned horizontally to the
     * viewport so it stays visible while the table scrolls sideways. Use it for a group name, a
     * rename affordance, a count — attach your own `Modifier.clickable` inside it; the library adds
     * no click handling of its own.
     */
    public val header: (@Composable (rows: IntRange) -> Unit)? = null,
)
