package ua.wwind.table.sample.viewmodel

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterNode.Operator
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import kotlin.test.Test

class SalaryExclusionTest {
    private fun condition(
        column: PersonColumn,
        value: Int,
    ) = TableFilterNode.Condition(column, TableFilterState(FilterConstraint.GTE, listOf(value)))

    private val salary = condition(PersonColumn.SALARY, 1000)
    private val age = condition(PersonColumn.AGE, 30)

    @Test
    fun `top-level salary conditions are left out`() {
        val root = TableFilterNode.Group(Operator.AND, listOf(salary, age, condition(PersonColumn.SALARY, 2000)))

        assertThat(root.withoutTopLevelConditions(PersonColumn.SALARY))
            .isEqualTo(TableFilterNode.Group(Operator.AND, listOf(age)))
    }

    @Test
    fun `salary conditions inside groups and nots stay`() {
        val group = TableFilterNode.Group(Operator.OR, listOf(salary, age))
        val not = TableFilterNode.Not(salary)
        val root = TableFilterNode.Group(Operator.AND, listOf(group, not, salary))

        assertThat(root.withoutTopLevelConditions(PersonColumn.SALARY))
            .isEqualTo(TableFilterNode.Group(Operator.AND, listOf(group, not)))
    }
}
