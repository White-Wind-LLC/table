package ua.wwind.table.sample.model

import kotlinx.datetime.LocalDate

/** Demo data for career movements for a single person. */
data class PersonMovement(
    val date: LocalDate,
    val fromPosition: Position?,
    val toPosition: Position,
)

/**
 * Block identity for the embedded movements demo: adjacent same-year movements render and drag as
 * one unit. Declared once so the table's `blockOf` and the ViewModel's lift stay the same rule —
 * they MUST agree, or a committed move would relocate a different set of rows than was dragged.
 */
val PersonMovement.movementBlockId: Int get() = date.year

/** Columns for the embedded movements table. */
enum class PersonMovementColumn {
    REORDER,
    DATE,
    FROM_POSITION,
    TO_POSITION,
}
