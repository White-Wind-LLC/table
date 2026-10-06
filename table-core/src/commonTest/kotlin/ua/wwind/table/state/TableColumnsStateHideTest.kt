package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

class TableColumnsStateHideTest {
    private fun stateWith(pinned: Int = 0) =
        TableState(
            initialColumns = listOf("a", "b", "c"),
            initialSort = null,
            initialOrder = listOf("a", "b", "c"),
            initialWidths = emptyMap(),
            settings = TableSettings(pinnedColumnsCount = pinned),
            dimensions = TableDefaults.standardDimensions(),
        )

    @Test
    fun `hide and show toggle membership in hidden`() {
        val state = stateWith()
        state.columns.hide("b")
        assertThat(state.columns.hidden.toList()).containsExactly("b")
        assertThat(state.columns.visibleKeys()).containsExactly("a", "c")
        state.columns.show("b")
        assertThat(state.columns.hidden).isEmpty()
    }

    @Test
    fun `showAll clears every runtime-hidden column`() {
        val state = stateWith()
        state.columns.hide("a")
        state.columns.hide("b")
        state.columns.showAll()
        assertThat(state.columns.hidden).isEmpty()
    }

    @Test
    fun `the last visible column cannot be hidden`() {
        val state = stateWith()
        state.columns.hide("a")
        state.columns.hide("b")
        assertThat(state.columns.canHide("c")).isFalse()
        state.columns.hide("c")
        assertThat(state.columns.visibleKeys()).containsExactly("c")
    }

    @Test
    fun `hiding a pinned column shrinks the pinned block`() {
        val state = stateWith(pinned = 2)
        state.columns.hide("a")
        assertThat(state.columns.pinnedCount).isEqualTo(1)
    }
}
