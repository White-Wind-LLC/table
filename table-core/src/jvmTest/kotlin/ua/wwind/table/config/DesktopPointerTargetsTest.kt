package ua.wwind.table.config

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DesktopPointerTargetsTest {
    @Test
    fun `desktop pointer targets default to the WCAG minimum or wider`() {
        val dimensions = TableDefaults.compactDimensions()
        assertThat(dimensions.columnResizeHandleWidth).isEqualTo(8.dp)
        assertThat(dimensions.headerIconTargetSize).isEqualTo(24.dp)
        assertThat(dimensions.columnDragHandleSize).isEqualTo(24.dp)
    }
}
