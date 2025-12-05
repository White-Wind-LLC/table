package ua.wwind.table.sample.model

import androidx.compose.runtime.Immutable

/**
 * Table data that holds the current state of the people table.
 * This data is accessible in headers, footers, and edit cells.
 */
@Immutable
data class PersonTableData(
    /** Currently displayed (filtered and sorted) people */
    val displayedPeople: List<Person> = emptyList(),
    /** People filtered by all filters except salary filter (for salary range calculation) */
    val peopleExcludingSalaryFilter: List<Person> = emptyList(),
    /** Current editing state (validation errors, edited person) */
    val editState: PersonEditState = PersonEditState(),
    /** IDs of selected persons */
    val selectedIds: Set<Int> = emptySet(),
    /** Whether selection mode is enabled */
    val selectionModeEnabled: Boolean = false,
)
