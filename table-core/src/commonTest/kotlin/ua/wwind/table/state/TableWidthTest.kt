package ua.wwind.table.state

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.ColumnSpec
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

private fun spec(key: String): ColumnSpec<Any, String, Unit> =
    ColumnSpec(
        key = key,
        header = {},
        cell = { _, _ -> },
        valueOf = { null },
    )

/**
 * [TableState.tableWidth] is a `derivedStateOf` over [TableState.visibleColumns]. Snapshot state
 * only invalidates a derived value when a *tracked* read changes, so `visibleColumns` has to be
 * snapshot state itself: as a plain field its writes are invisible and the width freezes at
 * whatever the first composition computed.
 *
 * That freeze is not cosmetic. Rows are measured with `Modifier.width(state.tableWidth)` and the
 * horizontal scroll range follows it, so a stale width leaves newly shown columns off screen and
 * unreachable — while the header, which measures from its own width map, lays them out. The result
 * is a header that no longer lines up with the rows beneath it.
 */
class TableWidthTest {
    private fun newState(): TableState<String> =
        TableState(
            initialColumns = listOf("a", "b"),
            initialSort = null,
            initialOrder = listOf("a", "b"),
            initialWidths = mapOf("a" to 100.dp, "b" to 100.dp),
            settings = TableSettings(),
            dimensions = TableDefaults.standardDimensions(),
        )

    @Test
    fun `tableWidth follows a column being hidden`() {
        val state = newState()
        val divider = state.dimensions.dividerThickness

        state.visibleColumns = listOf(spec("a"), spec("b"))
        assertThat(state.tableWidth).isEqualTo(200.dp + divider * 2)

        state.visibleColumns = listOf(spec("a"))
        assertThat(state.tableWidth).isEqualTo(100.dp + divider)
    }

    @Test
    fun `tableWidth follows a column being shown again`() {
        val state = newState()
        val divider = state.dimensions.dividerThickness

        state.visibleColumns = listOf(spec("a"))
        assertThat(state.tableWidth).isEqualTo(100.dp + divider)

        state.visibleColumns = listOf(spec("a"), spec("b"))
        assertThat(state.tableWidth).isEqualTo(200.dp + divider * 2)
    }

    @Test
    fun `tableWidth follows a column width change`() {
        val state = newState()
        val divider = state.dimensions.dividerThickness

        state.visibleColumns = listOf(spec("a"))
        assertThat(state.tableWidth).isEqualTo(100.dp + divider)

        state.columns.widths["a"] = 150.dp
        assertThat(state.tableWidth).isEqualTo(150.dp + divider)
    }
}
