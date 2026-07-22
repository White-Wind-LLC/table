package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.date.resolveDateFilter
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

class ResolveDateFilterTest {
    private val first = LocalDate(2026, 1, 1)
    private val second = LocalDate(2026, 12, 31)

    @Test
    fun `single date applies`() {
        assertThat(resolveDateFilter(first, null, FilterConstraint.EQUALS))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.EQUALS, listOf(first))))
    }

    @Test
    fun `missing single date clears`() {
        assertThat(resolveDateFilter(null, null, FilterConstraint.EQUALS)).isEqualTo(FilterEmission.Clear)
    }

    @Test
    fun `complete range applies`() {
        assertThat(resolveDateFilter(first, second, FilterConstraint.BETWEEN))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.BETWEEN, listOf(first, second))))
    }

    @Test
    fun `incomplete range clears`() {
        assertThat(resolveDateFilter(first, null, FilterConstraint.BETWEEN)).isEqualTo(FilterEmission.Clear)
    }

    @Test
    fun `null check applies with empty values`() {
        assertThat(resolveDateFilter(null, null, FilterConstraint.IS_NULL))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.IS_NULL, emptyList<LocalDate>())))
    }
}
