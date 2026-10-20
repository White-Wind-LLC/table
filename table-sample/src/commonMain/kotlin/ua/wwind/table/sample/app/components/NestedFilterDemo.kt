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
 * the AND root.
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

/** Payload of the demo's unresolved condition that the app can still name. */
private const val SALARY_BAND_PAYLOAD = "salaryBand"

/**
 * A saved filter with fields the app no longer knows: `(Senior developer OR Team lead OR legacyField) AND
 * name contains "a"`, plus a second unresolved condition the app can still title. The table shows both, and
 * the sample does not apply the filter until they are removed.
 */
internal fun unresolvedFilterDemo(): TableFilterNode.Group<PersonColumn> =
    TableFilterNode.Group(
        Operator.AND,
        listOf(
            TableFilterNode.Group(
                Operator.OR,
                listOf(
                    condition(PersonColumn.POSITION, FilterConstraint.EQUALS, Position.SENIOR_DEVELOPER),
                    condition(PersonColumn.POSITION, FilterConstraint.EQUALS, Position.TEAM_LEAD),
                    TableFilterNode.Unresolved("legacyField", payload = mapOf("eq" to "x")),
                ),
            ),
            condition(PersonColumn.NAME, FilterConstraint.CONTAINS, "a"),
            TableFilterNode.Unresolved("salary_band", payload = SALARY_BAND_PAYLOAD),
        ),
    )

/** The chips' title of an unresolved condition the sample can still name. */
internal fun personUnresolvedTitle(
    key: String,
    payload: Any?,
): String? = if (key == "salary_band" && payload == SALARY_BAND_PAYLOAD) "Salary band" else null
