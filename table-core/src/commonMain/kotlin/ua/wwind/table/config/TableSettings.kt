package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import ua.wwind.table.platform.getPlatform
import ua.wwind.table.platform.isMobile

/** Table behavior settings */
@Immutable
public data class TableSettings(
    /**
     * Enable drag and drop row reordering.
     *
     * This mode is incompatible with sorting and grouping. Incompatible settings are normalized in
     * [ua.wwind.table.state.rememberTableState].
     */
    val rowReorderEnabled: Boolean = false,
    /** Automatically apply filters while typing */
    val autoApplyFilters: Boolean = true,
    /** Show fast filters in the table header */
    val showFastFilters: Boolean = false,
    /** Delay for automatic filter application (ms) */
    val autoFilterDebounce: Long = 300,
    /** Show striped rows */
    val stripedRows: Boolean = false,
    /** Show built-in active filters header above the table */
    val showActiveFiltersHeader: Boolean = false,
    /** Selection mode */
    val selectionMode: SelectionMode = SelectionMode.None,
    /** Row height strategy: Fixed (default) or Dynamic per content. */
    val rowHeightMode: RowHeightMode = RowHeightMode.Fixed,
    /** Group content alignment */
    val groupContentAlignment: Alignment = Alignment.CenterStart,
    /**
     * Enable drag-to-scroll functionality. When disabled, traditional scrollbars are used
     * instead.
     */
    val enableDragToScroll: Boolean = getPlatform().isMobile(),
    /** Number of pinned columns */
    val pinnedColumnsCount: Int = 0,
    /** Side to pin columns to */
    val pinnedColumnsSide: PinnedSide = PinnedSide.Left,
    /**
     * Enable cell editing mode for the table. When disabled, all column-level edit settings are
     * ignored.
     */
    val editingEnabled: Boolean = false,
    /** Show footer row */
    val showFooter: Boolean = false,
    /** Pin footer at the bottom (only for non-embedded tables) */
    val footerPinned: Boolean = true,
    /** Enable text selection in table rows (wraps body in SelectionContainer) */
    val enableTextSelection: Boolean = false,
    /** Show vertical dividers between columns */
    val showVerticalDividers: Boolean = true,
    /** Show horizontal dividers between rows */
    val showRowDividers: Boolean = true,
    /** Show horizontal divider below the header */
    val showHeaderDivider: Boolean = true,
    /** Show horizontal divider below the fast filters row */
    val showFastFiltersDivider: Boolean = true,
)

/**
 * Interaction lock used when row reorder mode is active.
 *
 * In this mode sorting and grouping are treated as incompatible.
 */
public val TableSettings.isInteractionLockByRowReorderEnabled: Boolean
    get() = rowReorderEnabled

/** Row selection behavior. */
public enum class SelectionMode {
    None,
    Single,
    Multiple,
}

/** Side to pin columns to. */
public enum class PinnedSide {
    Left,
    Right,
}

/** Row height behavior. */
public enum class RowHeightMode {
    Fixed,
    Dynamic,
}
