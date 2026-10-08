package ua.wwind.table.config

import androidx.compose.runtime.Composable
import ua.wwind.table.platform.systemPrefersReducedMotion

/**
 * How much the table animates: rows moving after a sort or a reorder, the header and fast filters
 * sliding in, the lift of a dragged row or column, the fade of pinned-column shadows.
 */
public enum class TableMotion {
    /**
     * Follow the platform's reduced-motion setting: Reduce Motion on iOS, `prefers-reduced-motion`
     * on the web. Android needs nothing extra: Compose already scales every animation by the system
     * animation scale, so "Remove animations" turns them off. Desktop has no such setting and animates.
     */
    System,

    /** Always animate. */
    Full,

    /** Never animate: every change lands in its final place at once. */
    Reduced,
}

/** Whether this setting turns animations off on the current device. */
@Composable
public fun TableMotion.isReduced(): Boolean =
    when (this) {
        TableMotion.System -> systemPrefersReducedMotion()
        TableMotion.Full -> false
        TableMotion.Reduced -> true
    }
