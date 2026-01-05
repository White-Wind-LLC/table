package ua.wwind.table.sample.app.components

import androidx.compose.runtime.Immutable
import ua.wwind.table.config.PinnedSide

/**
 * Configuration data class for sample table settings.
 * Groups all parameters that affect TableSettings to reduce parameter count.
 */
@Immutable
data class SampleTableConfig(
    val useStripedRows: Boolean = true,
    val showFastFilters: Boolean = true,
    val enableDragToScroll: Boolean = true,
    val pinnedColumnsCount: Int = 0,
    val pinnedColumnsSide: PinnedSide = PinnedSide.Left,
    val enableEditing: Boolean = false,
    val useCompactMode: Boolean = true,
    val showFooter: Boolean = true,
    val footerPinned: Boolean = true,
    val showVerticalDividers: Boolean = true,
    val showRowDividers: Boolean = true,
    val showHeaderDivider: Boolean = true,
    val showFastFiltersDivider: Boolean = true,
)
