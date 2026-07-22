package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.filter.component.main.FilterEmission
import ua.wwind.table.filter.component.main.text.resolveTextFilter
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import kotlin.test.Test

class ResolveTextFilterTest {
    @Test
    fun `non blank text applies`() {
        assertThat(resolveTextFilter("abc", FilterConstraint.CONTAINS))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.CONTAINS, listOf("abc"))))
    }

    @Test
    fun `blank text clears`() {
        assertThat(resolveTextFilter("   ", FilterConstraint.CONTAINS)).isEqualTo(FilterEmission.Clear)
    }

    @Test
    fun `null check applies with empty values`() {
        assertThat(resolveTextFilter("", FilterConstraint.IS_NULL))
            .isEqualTo(FilterEmission.Apply(TableFilterState(FilterConstraint.IS_NULL, emptyList<String>())))
    }
}
