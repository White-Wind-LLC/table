package ua.wwind.table.config

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment

/**
 * Table behavior settings
 */
@Immutable
public data class TableSettings(
    /** Enable drag and drop for rows */
    val isDragEnabled: Boolean = false,
    /** Automatically apply filters while typing */
    val autoApplyFilters: Boolean = true,
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
)

/** Row selection behavior. */
public enum class SelectionMode { None, Single, Multiple }

/** Row height behavior. */
public enum class RowHeightMode { Fixed, Dynamic }
