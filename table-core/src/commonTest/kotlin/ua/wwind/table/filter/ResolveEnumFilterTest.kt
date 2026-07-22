package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.enumm.resolveEnumFilter
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

private enum class Color { RED, GREEN, BLUE }

class ResolveEnumFilterTest {
    @Test
    fun `empty selection clears`() {
        assertThat(resolveEnumFilter(emptyList<Color>(), FilterConstraint.EQUALS))
            .isEqualTo(FilterEmission.Clear)
    }

    @Test
    fun `single selection applies`() {
        assertThat(resolveEnumFilter(listOf(Color.RED), FilterConstraint.EQUALS))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.EQUALS, listOf(Color.RED))))
    }

    @Test
    fun `multi selection applies`() {
        assertThat(resolveEnumFilter(listOf(Color.RED, Color.BLUE), FilterConstraint.IN))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.IN, listOf(Color.RED, Color.BLUE))))
    }

    @Test
    fun `null check with empty selection stays applied`() {
        assertThat(resolveEnumFilter(emptyList<Color>(), FilterConstraint.IS_NULL))
            .isEqualTo(FilterEmission.Apply(TableFilterState<Color>(FilterConstraint.IS_NULL, null)))
    }
}
