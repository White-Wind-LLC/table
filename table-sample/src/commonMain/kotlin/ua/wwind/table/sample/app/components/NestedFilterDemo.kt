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
 * A nested filter over the sample columns: two OR groups, a negated group and plain conditions under
 * the AND root, and a condition on [PersonColumn.REGION], which has no column, so the row shows the
 * chip of a key the table does not know (titled by [personUnknownColumnTitle]).
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
            condition(PersonColumn.REGION, FilterConstraint.EQUALS, "EMEA"),
        ),
    )

/** The chips' title of a key the table has no column for. */
internal fun personUnknownColumnTitle(column: PersonColumn): String? =
    if (column == PersonColumn.REGION) "Region" else null
