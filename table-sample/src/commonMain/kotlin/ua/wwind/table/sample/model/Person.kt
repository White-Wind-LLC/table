package ua.wwind.table.sample.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Person data model with fields for table demonstration. */
@Immutable
data class Person(
    val name: String,
    val age: Int,
    val active: Boolean,
    val id: Int,
    val email: String,
    val city: String,
    val country: String,
    val department: String,
    val position: Position,
    val salary: Int,
    val rating: Int,
    val hireDate: LocalDate,
    /** Multiline notes to demonstrate dynamic row height in table. */
    val notes: String =
        when {
            id % 4 == 0 ->
                """
                This is a sample multi-line note for demo purposes.
                It spans multiple lines to showcase dynamic row height.
                You can add more content here as needed.
                """.trimIndent()
            id % 7 == 0 ->
                """
                This is a sample multi-line note for demo purposes.
                It spans two lines to showcase dynamic row height.
                """.trimIndent()
            else -> "This is a single-line note."
        },
    val expandedMovement: Boolean = false,
) {
    /**
     * Career movements history for the person. Generated pseudo-randomly based on person immutable
     * data.
     */
    val movements: List<PersonMovement> = generateMovements()

    /**
     * Generates deterministic pseudo-random movements for the person. First movement is hiring on
     * [hireDate] to a random position. Then from 2 to 6 movements, where the last movement ends on
     * [position].
     */
    private fun generateMovements(): List<PersonMovement> {
        // Deterministic seed based on person id to keep data stable between runs
        val seed = id * 31 + age * 17 + salary
        val random = kotlin.random.Random(seed)

        val allPositions = Position.entries

        // Hire movement: hireDate to random position
        val initialPosition = allPositions.random(random)
        val movements = mutableListOf<PersonMovement>()

        movements +=
            PersonMovement(
                date = hireDate,
                fromPosition = null,
                toPosition = initialPosition,
            )

        // Decide total number of movements: from 2 to 6
        val totalMovements = random.nextInt(from = 2, until = 7)

        var currentDate = hireDate
        var currentPosition = initialPosition

        // Generate intermediate movements, reserving the last one for final position
        for (index in 1 until totalMovements) {
            val isLastMovement = index == totalMovements - 1

            val nextPosition =
                if (isLastMovement) {
                    // Ensure the last movement ends at the person final position
                    position
                } else {
                    // Pick a random position that may differ from current
                    var candidate: Position
                    do {
                        candidate = allPositions.random(random)
                    } while (candidate == currentPosition && allPositions.size > 1)
                    candidate
                }

            // Advance date by 90-540 days
            val daysToAdd = random.nextInt(from = 90, until = 541)
            currentDate = currentDate.plus(daysToAdd, DateTimeUnit.DAY)

            movements +=
                PersonMovement(
                    date = currentDate,
                    fromPosition = currentPosition,
                    toPosition = nextPosition,
                )

            currentPosition = nextPosition
        }

        return movements
    }
}
