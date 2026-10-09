package ua.wwind.table.component.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class HeaderArrangementTest {
    @Test
    fun `relative alignments keep their relative arrangement in both directions`() {
        for (direction in LayoutDirection.entries) {
            assertThat(Alignment.CenterStart.horizontalArrangement(direction)).isEqualTo(Arrangement.Start)
            assertThat(Alignment.CenterEnd.horizontalArrangement(direction)).isEqualTo(Arrangement.End)
            assertThat(Alignment.Center.horizontalArrangement(direction)).isEqualTo(Arrangement.Center)
        }
    }

    @Test
    fun `absolute alignments keep their physical side in RTL`() {
        assertThat(AbsoluteAlignment.CenterRight.horizontalArrangement(LayoutDirection.Ltr)).isEqualTo(Arrangement.End)
        assertThat(
            AbsoluteAlignment.CenterRight.horizontalArrangement(LayoutDirection.Rtl),
        ).isEqualTo(Arrangement.Start)
        assertThat(AbsoluteAlignment.CenterLeft.horizontalArrangement(LayoutDirection.Rtl)).isEqualTo(Arrangement.End)
    }
}
