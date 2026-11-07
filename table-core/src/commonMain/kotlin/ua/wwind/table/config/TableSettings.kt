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
    /**
     * Initial number of columns pinned at the [pinnedColumnsSide] edge, added to that side's
     * [pinnedStartCount] or [pinnedEndCount].
     */
    @Deprecated("Use pinnedStartCount / pinnedEndCount. Removed in 4.0.")
    val pinnedColumnsCount: Int = 0,
    /**
     * Side [pinnedColumnsCount] pins to, and the side the deprecated single-side calls
     * (`TableColumnsState.pinnedCount`, `pin(column)`, `canPin(column)`) act on.
     */
    @Deprecated("Use pinnedStartCount / pinnedEndCount. Removed in 4.0.")
    val pinnedColumnsSide: PinnedSide = PinnedSide.Start,
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
    /**
     * Show a ⋮ button in each header with `headerDecorations` on that opens the column menu. The
     * menu is always reachable by right-click, long-press and Shift+F10 / the Menu key; the button
     * only makes it visible.
     */
    val showColumnMenuButton: Boolean = false,
    /** Whether the table animates; [TableMotion.System] follows the platform's reduced-motion setting. */
    val motion: TableMotion = TableMotion.System,
    /**
     * Initial number of leading columns pinned at the start edge. The live count, changed by pinning
     * from the column menu, is [ua.wwind.table.state.TableColumnsState.pinnedStartCount].
     */
    val pinnedStartCount: Int = 0,
    /**
     * Initial number of trailing columns pinned at the end edge. The live count, changed by pinning
     * from the column menu, is [ua.wwind.table.state.TableColumnsState.pinnedEndCount].
     */
    val pinnedEndCount: Int = 0,
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

/** Side to pin columns to, relative to the layout direction. */
public enum class PinnedSide {
    /** The leading columns: physically left in LTR, right in RTL. */
    Start,

    /** The trailing columns: physically right in LTR, left in RTL. */
    End,
    ;

    public companion object {
        @Deprecated(
            "Pins the leading columns, which are on the right in RTL. Removed in 4.0.",
            ReplaceWith("PinnedSide.Start"),
        )
        public val Left: PinnedSide get() = Start

        @Deprecated(
            "Pins the trailing columns, which are on the left in RTL. Removed in 4.0.",
            ReplaceWith("PinnedSide.End"),
        )
        public val Right: PinnedSide get() = End
    }
}

/** Row height behavior. */
public enum class RowHeightMode {
    Fixed,
    Dynamic,
}
