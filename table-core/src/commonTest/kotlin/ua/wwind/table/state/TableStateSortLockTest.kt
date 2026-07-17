package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import kotlin.test.Test

/**
 * The consistency half of the reorder interaction lock: `initialSort` was already normalized away
 * in `rememberTableState`, but a runtime `setSort` used to slip through — under an active sort the
 * view is invariant to source permutations, so a committed drag would be unobservable.
 */
class TableStateSortLockTest {
    private fun stateWith(settings: TableSettings) =
        TableState(
            initialColumns = listOf("a", "b"),
            initialSort = null,
            initialOrder = listOf("a", "b"),
            initialWidths = emptyMap(),
            settings = settings,
            dimensions = TableDefaults.standardDimensions(),
        )

    @Test
    fun `setSort is a no-op while row reorder is enabled`() {
        val state = stateWith(TableSettings(rowReorderEnabled = true))
        state.setSort("a")
        assertThat(state.sort).isNull()
        state.setSort("b", SortOrder.DESCENDING)
        assertThat(state.sort).isNull()
    }

    @Test
    fun `setSort applies when row reorder is disabled`() {
        val state = stateWith(TableSettings())
        state.setSort("a")
        assertThat(state.sort).isEqualTo(SortState("a", SortOrder.ASCENDING))
        state.setSort("a")
        assertThat(state.sort).isEqualTo(SortState("a", SortOrder.DESCENDING))
        state.setSort("a")
        assertThat(state.sort).isNull()
    }
}
