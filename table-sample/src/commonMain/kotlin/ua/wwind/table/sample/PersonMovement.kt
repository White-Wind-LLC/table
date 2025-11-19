package ua.wwind.table.sample

import kotlinx.datetime.LocalDate

/**
 * Demo data for Person objects used in the table sample.
 */
@Suppress("LongMethod")
data class PersonMovement(
    val date: LocalDate,
    val fromPosition: Position?,
    val toPosition: Position,
)