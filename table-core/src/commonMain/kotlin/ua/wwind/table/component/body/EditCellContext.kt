package ua.wwind.table.component.body

/**
 * Context for the currently editing cell, providing row index and column key.
 * Column is stored as Any to avoid star projection issues when updating selectedCell.
 */
public data class EditCellContext(
    val rowIndex: Int,
    val columnAsAny: Any,
)
