package ua.wwind.table

import androidx.compose.ui.unit.dp
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import ua.wwind.table.config.TableDefaults
import kotlin.test.Test

class TableDimensionsTest {
    private val standard = TableDefaults.standardDimensions()

    @Test
    fun `standard dimensions keep the previous sizes and lift a dragged item by 8dp`() {
        assertThat(standard.cellHorizontalPadding).isEqualTo(8.dp)
        assertThat(standard.headerIconSpacing).isEqualTo(6.dp)
        assertThat(standard.dragHandleIconSize).isEqualTo(16.dp)
        assertThat(standard.focusIndicatorWidth).isEqualTo(2.dp)
        assertThat(standard.pinnedColumnShadowWidth).isEqualTo(6.dp)
        assertThat(standard.dragElevation).isEqualTo(8.dp)
    }

    @Test
    fun `negative sizes are rejected`() {
        assertFailure { standard.copy(cellHorizontalPadding = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { standard.copy(headerIconSpacing = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { standard.copy(dragHandleIconSize = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { standard.copy(focusIndicatorWidth = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { standard.copy(pinnedColumnShadowWidth = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { standard.copy(dragElevation = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun `fast filter row defaults to 40dp and font scaling is on`() {
        assertThat(standard.fastFilterRowHeight).isEqualTo(40.dp)
        assertThat(standard.scaleWithFontSize).isEqualTo(true)
    }

    @Test
    fun `a negative fast filter row height is rejected`() {
        assertFailure { standard.copy(fastFilterRowHeight = (-1).dp) }.isInstanceOf<IllegalArgumentException>()
    }
}
