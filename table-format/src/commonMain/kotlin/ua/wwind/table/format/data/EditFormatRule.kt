package ua.wwind.table.format.data

import androidx.compose.runtime.Immutable

@Immutable
internal data class EditFormatRule<E : Enum<E>, FILTER>(
    val index: Int,
    val item: TableFormatRule<E, FILTER>,
    val isNew: Boolean = false,
    /** A duplicate of another rule; once saved it is highlighted in the list. */
    val isCopy: Boolean = false,
)
