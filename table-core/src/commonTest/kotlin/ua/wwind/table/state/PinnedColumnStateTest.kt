package ua.wwind.table.state

import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import ua.wwind.table.config.PinnedSide
import kotlin.test.Test

class PinnedColumnStateTest {
    @Test
    fun `a start-pinned column follows the scroll in LTR and opposes it in RTL`() {
        assertThat(pinnedTranslationX(PinnedSide.Start, scrollValue = 30, maxScroll = 100, LayoutDirection.Ltr))
            .isEqualTo(30f)
        assertThat(pinnedTranslationX(PinnedSide.Start, scrollValue = 30, maxScroll = 100, LayoutDirection.Rtl))
            .isEqualTo(-30f)
    }

    @Test
    fun `an end-pinned column is mirrored in RTL`() {
        assertThat(pinnedTranslationX(PinnedSide.End, scrollValue = 30, maxScroll = 100, LayoutDirection.Ltr))
            .isEqualTo(-70f)
        assertThat(pinnedTranslationX(PinnedSide.End, scrollValue = 30, maxScroll = 100, LayoutDirection.Rtl))
            .isEqualTo(70f)
    }
}
