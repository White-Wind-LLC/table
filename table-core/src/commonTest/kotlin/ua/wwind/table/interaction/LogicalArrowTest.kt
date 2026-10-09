package ua.wwind.table.interaction

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class LogicalArrowTest {
    @Test
    fun `LTR keeps every key`() {
        assertThat(Key.DirectionLeft.logicalArrow(LayoutDirection.Ltr)).isEqualTo(Key.DirectionLeft)
        assertThat(Key.DirectionRight.logicalArrow(LayoutDirection.Ltr)).isEqualTo(Key.DirectionRight)
    }

    @Test
    fun `RTL swaps left and right only`() {
        assertThat(Key.DirectionLeft.logicalArrow(LayoutDirection.Rtl)).isEqualTo(Key.DirectionRight)
        assertThat(Key.DirectionRight.logicalArrow(LayoutDirection.Rtl)).isEqualTo(Key.DirectionLeft)
        assertThat(Key.DirectionUp.logicalArrow(LayoutDirection.Rtl)).isEqualTo(Key.DirectionUp)
        assertThat(Key.MoveHome.logicalArrow(LayoutDirection.Rtl)).isEqualTo(Key.MoveHome)
    }
}
