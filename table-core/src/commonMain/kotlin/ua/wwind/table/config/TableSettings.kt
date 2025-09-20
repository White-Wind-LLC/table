package ua.wwind.table.config

/**
 * Table behavior settings
 */
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
)

public enum class SelectionMode { None, Single, Multiple }
