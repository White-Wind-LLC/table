package ua.wwind.table.sample.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.data.TableCellStyleConfig
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.sample.column.PersonColumn

/**
 * Provider for default conditional formatting rules.
 */
object DefaultFormatRulesProvider {
    /**
     * Create default conditional formatting rules.
     * - If RATING >= 4, set content color to gold for the Rating column
     * - If ACTIVE = false, set content color to gray for the whole row
     */
    fun createDefaultRules(): ImmutableList<
        TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>>,
    > {
        val ratingRule = createRatingRule()
        val activeRule = createActiveRule()

        return persistentListOf(ratingRule, activeRule)
    }

    private fun createRatingRule(): TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>> {
        val ratingFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.RATING to
                    TableFilterState(
                        constraint = FilterConstraint.GTE,
                        values = listOf(4),
                    ),
            )

        return TableFormatRule(
            id = 1L,
            enabled = true,
            base = false,
            columns = listOf(PersonColumn.RATING),
            cellStyle =
                TableCellStyleConfig(
                    contentColor = 0xFFFFD700.toInt(), // Gold
                ),
            filter = ratingFilter,
        )
    }

    private fun createActiveRule(): TableFormatRule<PersonColumn, Map<PersonColumn, TableFilterState<*>>> {
        val activeFilter: Map<PersonColumn, TableFilterState<*>> =
            mapOf(
                PersonColumn.ACTIVE to
                    TableFilterState(
                        constraint = FilterConstraint.EQUALS,
                        values = listOf(false),
                    ),
            )

        return TableFormatRule(
            id = 2L,
            enabled = true,
            base = false,
            columns = emptyList(),
            cellStyle =
                TableCellStyleConfig(
                    contentColor = Color.LightGray.toArgb(),
                ),
            filter = activeFilter,
        )
    }
}
