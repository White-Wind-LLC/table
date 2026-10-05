package ua.wwind.table.component.header

import androidx.compose.ui.unit.dp
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import ua.wwind.table.config.TableDefaults
import kotlin.test.Test

class ColumnResizeScrollTest {
    @Test
    fun `a boundary inside the viewport needs no scrolling`() {
        assertThat(resizeScrollPullback(boundaryPx = 400f, scrollValue = 0, viewportPx = 600)).isEqualTo(0f)
    }

    @Test
    fun `a boundary exactly at the right edge needs no scrolling`() {
        assertThat(resizeScrollPullback(boundaryPx = 600f, scrollValue = 0, viewportPx = 600)).isEqualTo(0f)
    }

    @Test
    fun `a boundary past the right edge is pulled back by the overshoot`() {
        assertThat(resizeScrollPullback(boundaryPx = 1000f, scrollValue = 300, viewportPx = 600)).isEqualTo(100f)
    }

    @Test
    fun `a boundary scrolled out to the left is not pulled back`() {
        assertThat(resizeScrollPullback(boundaryPx = 100f, scrollValue = 800, viewportPx = 600)).isEqualTo(0f)
    }

    @Test
    fun `the edge zone starts one zone width before the right edge`() {
        assertThat(shouldGrowAtResizeEdge(4f, pointerViewportPx = 575f, viewportPx = 600, zonePx = 24f)).isFalse()
        assertThat(shouldGrowAtResizeEdge(4f, pointerViewportPx = 576f, viewportPx = 600, zonePx = 24f)).isTrue()
    }

    @Test
    fun `a pointer beyond the right edge keeps growing the column`() {
        assertThat(shouldGrowAtResizeEdge(4f, pointerViewportPx = 900f, viewportPx = 600, zonePx = 24f)).isTrue()
    }

    @Test
    fun `a pointer pulling left at the edge shrinks instead of growing`() {
        assertThat(shouldGrowAtResizeEdge(-4f, pointerViewportPx = 900f, viewportPx = 600, zonePx = 24f)).isFalse()
        assertThat(shouldGrowAtResizeEdge(-1f, pointerViewportPx = 598f, viewportPx = 600, zonePx = 24f)).isFalse()
    }

    @Test
    fun `a pointer held still at the edge keeps growing the column`() {
        assertThat(shouldGrowAtResizeEdge(0f, pointerViewportPx = 598f, viewportPx = 600, zonePx = 24f)).isTrue()
    }

    @Test
    fun `an unmeasured viewport has no edge zone`() {
        assertThat(shouldGrowAtResizeEdge(4f, pointerViewportPx = 0f, viewportPx = 0, zonePx = 24f)).isFalse()
    }

    @Test
    fun `a resize strip is centred on the divider after the boundary`() {
        assertThat(resizeHandleSpan(boundaryX = 100.dp, handleWidth = 8.dp, dividerThickness = 2.dp, isLast = false))
            .isEqualTo(ResizeHandleSpan(left = 97.dp, width = 8.dp))
    }

    @Test
    fun `the last resize strip ends at its divider and grows to the left`() {
        assertThat(resizeHandleSpan(boundaryX = 300.dp, handleWidth = 24.dp, dividerThickness = 1.dp, isLast = true))
            .isEqualTo(ResizeHandleSpan(left = 277.dp, width = 24.dp))
    }

    @Test
    fun `a resize strip is never narrower than the divider`() {
        assertThat(resizeHandleSpan(boundaryX = 100.dp, handleWidth = 0.dp, dividerThickness = 2.dp, isLast = false))
            .isEqualTo(ResizeHandleSpan(left = 100.dp, width = 2.dp))
    }

    @Test
    fun `a negative pointer target size is rejected`() {
        assertFailure { TableDefaults.standardDimensions().copy(columnResizeHandleWidth = (-1).dp) }
            .isInstanceOf<IllegalArgumentException>()
        assertFailure { TableDefaults.standardDimensions().copy(headerIconTargetSize = (-1).dp) }
            .isInstanceOf<IllegalArgumentException>()
        assertFailure { TableDefaults.standardDimensions().copy(columnDragHandleSize = (-1).dp) }
            .isInstanceOf<IllegalArgumentException>()
    }
}
