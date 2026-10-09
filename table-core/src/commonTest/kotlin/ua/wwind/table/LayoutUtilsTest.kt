package ua.wwind.table

import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class LayoutUtilsTest {
    @Test
    fun `sign keeps a delta in LTR and flips it in RTL`() {
        assertThat(LayoutDirection.Ltr.sign(5f)).isEqualTo(5f)
        assertThat(LayoutDirection.Rtl.sign(5f)).isEqualTo(-5f)
    }

    @Test
    fun `physicalLeft mirrors a span inside its container in RTL`() {
        assertThat(LayoutDirection.Ltr.physicalLeft(start = 10f, width = 4f, containerWidth = 100f)).isEqualTo(10f)
        assertThat(LayoutDirection.Rtl.physicalLeft(start = 10f, width = 4f, containerWidth = 100f)).isEqualTo(86f)
    }

    @Test
    fun `physicalLeft of a zero-width point mirrors a pointer position`() {
        assertThat(LayoutDirection.Rtl.physicalLeft(start = 30f, width = 0f, containerWidth = 100f)).isEqualTo(70f)
    }

    @Test
    fun `toward the start is physically left only in LTR`() {
        assertThat(LayoutDirection.Ltr.isPhysicallyLeft(towardStart = true)).isTrue()
        assertThat(LayoutDirection.Rtl.isPhysicallyLeft(towardStart = true)).isFalse()
        assertThat(LayoutDirection.Rtl.isPhysicallyLeft(towardStart = false)).isTrue()
    }
}
