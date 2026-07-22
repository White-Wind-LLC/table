package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.number.resolveNumberFilter
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import kotlin.test.Test

class ResolveNumberFilterTest {
    private fun resolve(
        text: String,
        secondText: String = "",
        constraint: FilterConstraint,
    ): FilterEmission<Int> = resolveNumberFilter(text, secondText, constraint, IntDelegate)

    @Test
    fun `single value applies`() {
        assertThat(resolve("5", constraint = FilterConstraint.EQUALS))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.EQUALS, listOf(5))))
    }

    @Test
    fun `blank single value clears`() {
        assertThat(resolve("", constraint = FilterConstraint.EQUALS)).isEqualTo(FilterEmission.Clear)
    }

    @Test
    fun `unparsable non blank single value is invalid`() {
        assertThat(resolve("-", constraint = FilterConstraint.EQUALS)).isEqualTo(FilterEmission.Invalid)
    }

    @Test
    fun `null check applies with empty values`() {
        assertThat(resolve("", constraint = FilterConstraint.IS_NULL))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.IS_NULL, emptyList<Int>())))
    }

    @Test
    fun `valid ascending range applies`() {
        assertThat(resolve("5", "10", FilterConstraint.BETWEEN))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.BETWEEN, listOf(5, 10))))
    }

    @Test
    fun `equal endpoints range applies`() {
        assertThat(resolve("5", "5", FilterConstraint.BETWEEN))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.BETWEEN, listOf(5, 5))))
    }

    @Test
    fun `inverted range is invalid`() {
        assertThat(resolve("100", "5", FilterConstraint.BETWEEN)).isEqualTo(FilterEmission.Invalid)
    }

    @Test
    fun `half filled range is invalid`() {
        assertThat(resolve("5", "", FilterConstraint.BETWEEN)).isEqualTo(FilterEmission.Invalid)
    }

    @Test
    fun `blank range clears`() {
        assertThat(resolve("", "", FilterConstraint.BETWEEN)).isEqualTo(FilterEmission.Clear)
    }
}
