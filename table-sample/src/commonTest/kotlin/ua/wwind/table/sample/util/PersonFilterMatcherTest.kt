package ua.wwind.table.sample.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.column.PersonColumn
import ua.wwind.table.sample.model.Person
import ua.wwind.table.sample.model.Position
import kotlin.test.Test

class PersonFilterMatcherTest {
    private fun person(
        id: Int,
        name: String,
        position: Position,
    ) = Person(
        name = name,
        age = 30,
        active = true,
        id = id,
        email = null,
        city = "Kyiv",
        country = "UA",
        department = "R&D",
        position = position,
        salary = 1000,
        rating = 3,
        hireDate = LocalDate(2020, 1, 1),
    )

    private val ann = person(1, "Ann", Position.DESIGNER)
    private val bob = person(2, "Bob", Position.TEAM_LEAD)
    private val cid = person(3, "Cid", Position.TEAM_LEAD)

    private fun position(vararg values: Position): TableFilterNode<PersonColumn> =
        TableFilterNode.Condition(PersonColumn.POSITION, TableFilterState(FilterConstraint.IN, values.toList()))

    private fun name(text: String): TableFilterNode<PersonColumn> =
        TableFilterNode.Condition(PersonColumn.NAME, TableFilterState(FilterConstraint.CONTAINS, listOf(text)))

    private fun matching(expression: TableFilterNode<PersonColumn>): List<String> =
        listOf(ann, bob, cid).filter { PersonFilterMatcher.matchesPerson(it, expression) }.map { it.name }

    @Test
    fun `matcher honors OR, AND and NOT`() {
        val or = TableFilterNode.Group(TableFilterNode.Operator.OR, listOf(name("Ann"), name("Bob")))
        assertThat(matching(or)).isEqualTo(listOf("Ann", "Bob"))

        val and = TableFilterNode.Group(TableFilterNode.Operator.AND, listOf(position(Position.TEAM_LEAD), name("C")))
        assertThat(matching(and)).isEqualTo(listOf("Cid"))

        assertThat(matching(TableFilterNode.Not(or))).isEqualTo(listOf("Cid"))
    }

    @Test
    fun `inactive conditions match everything`() {
        val inactive = TableFilterNode.Condition(PersonColumn.NAME, TableFilterState<String>(null, null))
        assertThat(matching(inactive)).isEqualTo(listOf("Ann", "Bob", "Cid"))
    }
}
