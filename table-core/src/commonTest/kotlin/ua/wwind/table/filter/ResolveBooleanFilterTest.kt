package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.booleann.resolveBooleanFilter
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

class ResolveBooleanFilterTest {
    @Test
    fun `true applies`() {
        assertThat(resolveBooleanFilter(true))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.EQUALS, listOf(true))))
    }

    @Test
    fun `false applies`() {
        assertThat(resolveBooleanFilter(false))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.EQUALS, listOf(false))))
    }

    @Test
    fun `null clears`() {
        assertThat(resolveBooleanFilter(null)).isEqualTo(FilterEmission.Clear)
    }
}
