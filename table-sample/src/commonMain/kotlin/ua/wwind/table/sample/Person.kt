package ua.wwind.table.sample

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate

/**
 * Person data model with fields for table demonstration.
 */
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
)
