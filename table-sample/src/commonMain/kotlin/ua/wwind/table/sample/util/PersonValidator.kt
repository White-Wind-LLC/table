package ua.wwind.table.sample.util

import ua.wwind.table.sample.model.Person

/**
 * Result of person validation containing error messages for each field.
 */
data class PersonValidationResult(
    val isValid: Boolean,
    val nameError: String = "",
    val ageError: String = "",
    val salaryError: String = "",
)

/**
 * Utility class for validating Person objects.
 */
object PersonValidator {
    /**
     * Validate a person and return validation result with error messages.
     */
    fun validate(person: Person): PersonValidationResult {
        var nameError = ""
        var ageError = ""
        var salaryError = ""

        // Validate name
        if (person.name.isBlank()) {
            nameError = "Name cannot be empty"
        }

        // Validate age
        when {
            person.age < 18 -> ageError = "Age must be at least 18"
            person.age > 100 -> ageError = "Age must not exceed 100"
        }

        // Validate salary
        if (person.salary < 0) {
            salaryError = "Salary cannot be negative"
        }

        val isValid = nameError.isEmpty() && ageError.isEmpty() && salaryError.isEmpty()

        return PersonValidationResult(
            isValid = isValid,
            nameError = nameError,
            ageError = ageError,
            salaryError = salaryError,
        )
    }
}
