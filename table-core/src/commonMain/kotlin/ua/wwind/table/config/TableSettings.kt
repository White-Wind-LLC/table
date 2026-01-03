package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment

/** Table behavior settings */
@Immutable
public data class TableSettings(
    /** Enable drag and drop for rows */
    val isDragEnabled: Boolean = false,
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
    val enableDragToScroll: Boolean = true,
    /** Number of pinned columns */
    val pinnedColumnsCount: Int = 0,
    /** Side to pin columns to */
    val pinnedColumnsSide: PinnedSide = PinnedSide.Left,
    /** Number of fixed columns */
    @Deprecated(
        "Use pinnedColumnsCount instead",
        ReplaceWith("pinnedColumnsCount"),
        level = DeprecationLevel.WARNING,
    )
    val fixedColumnsCount: Int = pinnedColumnsCount,
    /** Side to fix columns to */
    @Deprecated(
        "Use pinnedColumnsSide instead",
        ReplaceWith("pinnedColumnsSide"),
        level = DeprecationLevel.WARNING,
    )
    val fixedColumnsSide: FixedSide =
        when (pinnedColumnsSide) {
            PinnedSide.Left -> FixedSide.Left
            PinnedSide.Right -> FixedSide.Right
        },
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
)

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

/** Side to fix columns to. */
@Deprecated(
    "Use PinnedSide instead",
    ReplaceWith("PinnedSide", "ua.wwind.table.config.PinnedSide"),
    level = DeprecationLevel.WARNING,
)
public typealias FixedSide = PinnedSide

/** Row height behavior. */
public enum class RowHeightMode {
    Fixed,
    Dynamic,
}
