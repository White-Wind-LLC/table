package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.strings.UiString
import kotlin.test.Test

class InHierarchyConstraintTest {
    @Test
    fun `no built-in filter type offers it by default`() {
        val builtIn =
            listOf(
                TableFilterType.TextTableFilter(),
                TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate),
                TableFilterType.BooleanTableFilter(),
                TableFilterType.DateTableFilter(),
                TableFilterType.EnumTableFilter(options = persistentListOf(Kind.A), getTitle = { it.name }),
                TableFilterType.DisabledTableFilter,
            )
        builtIn.forEach { assertThat(it.constraints).doesNotContain(FilterConstraint.IN_HIERARCHY) }
    }

    @Test
    fun `maps to its own string`() {
        assertThat(FilterConstraint.IN_HIERARCHY.toUiString()).isEqualTo(UiString.FilterConstraintInHierarchy)
    }

    private enum class Kind { A }
}
