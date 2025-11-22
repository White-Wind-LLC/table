package ua.wwind.table.sample

/**
 * UI events for sample app, including table editing events.
 */
sealed class SampleUiEvent {
    /** Start editing a row - creates a mutable copy */
    data class StartEditing(
        val rowIndex: Int,
        val person: Person,
    ) : SampleUiEvent()

    /** Update name field during editing */
    data class UpdateName(
        val name: String,
    ) : SampleUiEvent()

    /** Update age field during editing */
    data class UpdateAge(
        val age: Int,
    ) : SampleUiEvent()

    /** Update email field during editing */
    data class UpdateEmail(
        val email: String,
    ) : SampleUiEvent()

    /** Update position field during editing */
    data class UpdatePosition(
        val position: Position,
    ) : SampleUiEvent()

    /** Update salary field during editing */
    data class UpdateSalary(
        val salary: Int,
    ) : SampleUiEvent()

    /** Complete editing - saves changes to the people list */
    data object CompleteEditing : SampleUiEvent()

    /** Cancel editing - discards changes */
    data object CancelEditing : SampleUiEvent()
}
