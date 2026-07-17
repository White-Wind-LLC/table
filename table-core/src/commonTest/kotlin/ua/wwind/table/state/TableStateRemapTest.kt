package ua.wwind.table.state

import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

/** All positional runtime state must move through one remap, or the pieces drift apart at commit. */
class TableStateRemapTest {
    @Test
    fun `remapRowPositions moves selection editing and checks together`() {
        val state =
            TableState(
                initialColumns = listOf("a"),
                initialSort = null,
                initialOrder = listOf("a"),
                initialWidths = emptyMap(),
                settings =
                    TableSettings(
                        selectionMode = SelectionMode.Multiple,
                        editingEnabled = true,
                    ),
                dimensions = TableDefaults.standardDimensions(),
            )
        state.startEditing(item = Any(), rowIndex = 2, column = "a")
        state.toggleSelect(3)
        state.selectCell(3, "a")
        state.toggleCheck(1)
        state.toggleCheck(4)

        state.remapRowPositions { position ->
            when (position) {
                3 -> 1
                2 -> 0
                1 -> 3
                4 -> 2
                else -> position
            }
        }

        assertThat(state.selectedIndex).isEqualTo(1)
        assertThat(state.selectedCell).isEqualTo(TableState.SelectedCell(1, "a"))
        assertThat(state.editingRow).isEqualTo(0)
        assertThat(state.checkedIndices.toList()).isEqualTo(listOf(3, 2))
    }
}
