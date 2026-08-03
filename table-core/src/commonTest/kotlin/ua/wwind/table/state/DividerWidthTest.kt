package ua.wwind.table.state

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

class DividerWidthTest {
    private val dimensions = TableDefaults.standardDimensions()

    private fun widths(
        settings: TableSettings,
        columns: Int = 8,
    ) = (0 until columns).map { dividerWidthAfterColumn(it, columns, settings, dimensions) }

    @Test
    fun `every column carries a divider while they are shown`() {
        val settings = TableSettings(showVerticalDividers = true)
        assertThat(widths(settings)).isEqualTo(List(8) { dimensions.dividerThickness })
    }

    @Test
    fun `no column carries a divider while they are hidden`() {
        val settings = TableSettings(showVerticalDividers = false)
        assertThat(widths(settings)).isEqualTo(List(8) { 0.dp })
    }

    @Test
    fun `the last left-pinned column keeps its divider while the rest are hidden`() {
        val settings =
            TableSettings(
                showVerticalDividers = false,
                pinnedColumnsCount = 2,
                pinnedColumnsSide = PinnedSide.Left,
            )
        val expected = List(8) { if (it == 1) dimensions.pinnedColumnDividerThickness else 0.dp }
        assertThat(widths(settings)).isEqualTo(expected)
    }

    @Test
    fun `the column before a right-pinned block keeps its divider while the rest are hidden`() {
        val settings =
            TableSettings(
                showVerticalDividers = false,
                pinnedColumnsCount = 2,
                pinnedColumnsSide = PinnedSide.Right,
            )
        val expected = List(8) { if (it == 5) dimensions.pinnedColumnDividerThickness else 0.dp }
        assertThat(widths(settings)).isEqualTo(expected)
    }

    @Test
    fun `pinning every column pins none, so no divider survives hiding`() {
        val settings =
            TableSettings(
                showVerticalDividers = false,
                pinnedColumnsCount = 8,
                pinnedColumnsSide = PinnedSide.Left,
            )
        assertThat(widths(settings)).isEqualTo(List(8) { 0.dp })
    }
}
