package ua.wwind.table.state

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import kotlin.test.Test

class DividerWidthTest {
    private val dimensions = TableDefaults.standardDimensions()
    private val pinnedThickness = dimensions.pinnedColumnDividerThickness

    private fun widths(
        settings: TableSettings,
        pinned: PinnedCounts = PinnedCounts.NONE,
        columns: Int = 8,
    ) = (0 until columns).map { dividerWidthAfterColumn(it, columns, pinned, settings, dimensions) }

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
    fun `the last start-pinned column keeps its divider while the rest are hidden`() {
        val settings = TableSettings(showVerticalDividers = false)
        val expected = List(8) { if (it == 1) pinnedThickness else 0.dp }
        assertThat(widths(settings, PinnedCounts(start = 2, end = 0))).isEqualTo(expected)
    }

    @Test
    fun `the column before an end-pinned block keeps its divider while the rest are hidden`() {
        val settings = TableSettings(showVerticalDividers = false)
        val expected = List(8) { if (it == 5) pinnedThickness else 0.dp }
        assertThat(widths(settings, PinnedCounts(start = 0, end = 2))).isEqualTo(expected)
    }

    @Test
    fun `both block edges keep the pinned divider while the rest are hidden`() {
        val settings = TableSettings(showVerticalDividers = false)
        val expected = listOf(0.dp, pinnedThickness, 0.dp, 0.dp, 0.dp, 0.dp, pinnedThickness, 0.dp)
        assertThat(widths(settings, PinnedCounts(start = 2, end = 1))).isEqualTo(expected)
    }

    @Test
    fun `pinning every column pins none so no divider survives hiding`() {
        val settings = TableSettings(showVerticalDividers = false)
        assertThat(widths(settings, PinnedCounts(start = 8, end = 0))).isEqualTo(List(8) { 0.dp })
        assertThat(widths(settings, PinnedCounts(start = 4, end = 4))).isEqualTo(List(8) { 0.dp })
    }
}
