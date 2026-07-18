package ua.wwind.table.sample.viewmodel

import ua.wwind.table.RowBlockMove
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.Position

/** UI events for sample app, including table editing events. */
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

    /** Toggle selection for a person by ID */
    data class ToggleSelection(
        val personId: Int,
    ) : SampleUiEvent()

    /** Toggle selection for all displayed persons */
    data object ToggleSelectAll : SampleUiEvent()

    /** Delete all selected persons */
    data object DeleteSelected : SampleUiEvent()

    /** Pull the selected rows together and give them a shared group id. */
    data object GroupSelected : SampleUiEvent()

    /** Move the selected rows out of their groups, dropping them just below the block they left. */
    data object UngroupSelected : SampleUiEvent()

    /** Rename a group: every row carrying [groupId] gets [newGroupId] instead. */
    data class RenameGroup(
        val groupId: String,
        val newGroupId: String,
    ) : SampleUiEvent()

    /** Clear all selections */
    data object ClearSelection : SampleUiEvent()

    /** Swap two person rows in backing list by IDs resolved from displayed positions */
    data class RowMove(
        val fromPersonId: Int,
        val toPersonId: Int,
    ) : SampleUiEvent()

    /** Apply a completed block-drag gesture to the master people list. */
    data class BlockMove(
        val move: RowBlockMove,
    ) : SampleUiEvent()

    /** Apply a completed within-block row-reorder gesture to the master people list. */
    data class RowWithinBlockMove(
        val move: ua.wwind.table.RowWithinBlockMove,
    ) : SampleUiEvent()

    /** Apply a completed block-drag gesture inside one person's embedded movements table. */
    data class MovementBlockMove(
        val personId: Int,
        val move: RowBlockMove,
    ) : SampleUiEvent()

    /** Apply a within-block row-reorder inside one person's embedded movements table. */
    data class MovementRowWithinBlockMove(
        val personId: Int,
        val move: ua.wwind.table.RowWithinBlockMove,
    ) : SampleUiEvent()

    /** Reorder embedded movement rows for a specific person by movement indices. */
    data class MovementRowMove(
        val personId: Int,
        val fromIndex: Int,
        val toIndex: Int,
    ) : SampleUiEvent()
}
