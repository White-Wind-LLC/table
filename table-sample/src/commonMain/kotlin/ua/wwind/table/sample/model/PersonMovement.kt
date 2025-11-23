package ua.wwind.table.sample.model

import kotlinx.datetime.LocalDate

/** Demo data for career movements for a single person. */
data class PersonMovement(
    val date: LocalDate,
    val fromPosition: Position?,
    val toPosition: Position,
)

/** Columns for the embedded movements table. */
enum class PersonMovementColumn {
    DATE,
    FROM_POSITION,
    TO_POSITION,
}
