package ua.wwind.table.filter

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import ua.wwind.table.filter.component.fast.FastFilterMode
import ua.wwind.table.filter.component.fast.fastFilterMode
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.IntDelegate
import kotlin.test.Test

private enum class Shade { Red, Green }

/** How the fast row shows a filter whose operator came from the panel (issue #105). */
class FastFilterModeTest {
    private val text = TableFilterType.TextTableFilter()
    private val number = TableFilterType.NumberTableFilter(delegate = IntDelegate)
    private val date = TableFilterType.DateTableFilter()
    private val enum =
        TableFilterType.EnumTableFilter(options = persistentListOf(Shade.Red, Shade.Green), getTitle = { it.name })
    private val day = LocalDate(2026, 1, 1)

    private fun editable(hint: FilterConstraint? = null) = FastFilterMode.Editable(hint)

    @Test
    fun `no state is editable without a hint`() {
        assertThat(fastFilterMode(text, null)).isEqualTo(editable())
    }

    @Test
    fun `inactive state keeps the fast default whatever its operator`() {
        assertThat(fastFilterMode(number, TableFilterState<Int>(FilterConstraint.GT, null))).isEqualTo(editable())
    }

    @Test
    fun `text default operator has no hint`() {
        assertThat(fastFilterMode(text, TableFilterState(FilterConstraint.CONTAINS, listOf("a"))))
            .isEqualTo(editable())
    }

    @Test
    fun `text single value operators are hinted`() {
        listOf(
            FilterConstraint.EQUALS,
            FilterConstraint.NOT_EQUALS,
            FilterConstraint.STARTS_WITH,
            FilterConstraint.ENDS_WITH,
        ).forEach { constraint ->
            assertThat(fastFilterMode(text, TableFilterState(constraint, listOf("a")))).isEqualTo(editable(constraint))
        }
    }

    @Test
    fun `null checks lock the field`() {
        assertThat(fastFilterMode(text, TableFilterState<String>(FilterConstraint.IS_NULL, emptyList())))
            .isEqualTo(FastFilterMode.Locked)
        assertThat(fastFilterMode(number, TableFilterState<Int>(FilterConstraint.IS_NOT_NULL, null)))
            .isEqualTo(FastFilterMode.Locked)
    }

    @Test
    fun `number comparisons are hinted and equals is not`() {
        assertThat(fastFilterMode(number, TableFilterState(FilterConstraint.EQUALS, listOf(5)))).isEqualTo(editable())
        listOf(
            FilterConstraint.NOT_EQUALS,
            FilterConstraint.GT,
            FilterConstraint.GTE,
            FilterConstraint.LT,
            FilterConstraint.LTE,
        ).forEach { constraint ->
            assertThat(fastFilterMode(number, TableFilterState(constraint, listOf(5)))).isEqualTo(editable(constraint))
        }
    }

    @Test
    fun `between locks number and date fields`() {
        assertThat(fastFilterMode(number, TableFilterState(FilterConstraint.BETWEEN, listOf(1, 10))))
            .isEqualTo(FastFilterMode.Locked)
        assertThat(fastFilterMode(date, TableFilterState(FilterConstraint.BETWEEN, listOf(day, day))))
            .isEqualTo(FastFilterMode.Locked)
    }

    @Test
    fun `date comparisons are hinted`() {
        assertThat(fastFilterMode(date, TableFilterState(FilterConstraint.GTE, listOf(day))))
            .isEqualTo(editable(FilterConstraint.GTE))
    }

    @Test
    fun `enum single selection is editable`() {
        assertThat(fastFilterMode(enum, TableFilterState(FilterConstraint.EQUALS, listOf(Shade.Red))))
            .isEqualTo(editable())
        assertThat(fastFilterMode(enum, TableFilterState(FilterConstraint.IN, listOf(Shade.Red))))
            .isEqualTo(editable())
        assertThat(fastFilterMode(enum, TableFilterState(FilterConstraint.NOT_IN, listOf(Shade.Red))))
            .isEqualTo(editable(FilterConstraint.NOT_EQUALS))
    }

    @Test
    fun `enum multi selection locks the field`() {
        assertThat(fastFilterMode(enum, TableFilterState(FilterConstraint.IN, listOf(Shade.Red, Shade.Green))))
            .isEqualTo(FastFilterMode.Locked)
    }
}
