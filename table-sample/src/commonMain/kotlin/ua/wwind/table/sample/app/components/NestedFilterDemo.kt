package ua.wwind.table.sample.app.components

import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Position

private const val YOUNG_AGE = 25

private fun <T> condition(
    column: PersonColumn,
    constraint: FilterConstraint,
    value: T,
): TableFilterNode<PersonColumn> = TableFilterNode.Condition(column, TableFilterState(constraint, listOf(value)))

/**
 * A nested filter over real sample columns: two OR groups, a negated group and plain conditions
 * under the AND root. Every [PersonColumn] has a column spec in the sample, so no condition here
 * uses a key the table does not know.
 */
internal fun nestedFilterDemo(): TableFilterNode.Group<PersonColumn> =
    TableFilterNode.Group(
        Operator.AND,
        listOf(
            TableFilterNode.Group(
                Operator.OR,
                listOf(
                    condition(PersonColumn.POSITION, FilterConstraint.EQUALS, Position.SENIOR_DEVELOPER),
                    condition(PersonColumn.POSITION, FilterConstraint.EQUALS, Position.TEAM_LEAD),
                ),
            ),
            TableFilterNode.Not(
                TableFilterNode.Group(
                    Operator.OR,
                    listOf(
                        condition(PersonColumn.AGE, FilterConstraint.LT, YOUNG_AGE),
                        condition(PersonColumn.RATING, FilterConstraint.EQUALS, 1),
                    ),
                ),
            ),
            condition(PersonColumn.NAME, FilterConstraint.CONTAINS, "a"),
            TableFilterNode.Group(
                Operator.OR,
                listOf(
                    TableFilterNode.Group(
                        Operator.AND,
                        listOf(
                            condition(PersonColumn.COUNTRY, FilterConstraint.EQUALS, "Ukraine"),
                            condition(PersonColumn.ACTIVE, FilterConstraint.EQUALS, true),
                        ),
                    ),
                    condition(PersonColumn.DEPARTMENT, FilterConstraint.EQUALS, "Engineering"),
                ),
            ),
        ),
    )
