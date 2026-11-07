package ua.wwind.table.state

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.containsExactly
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

    @Test
    fun `both blocks are pinned at their own side`() {
        val pinned = PinnedCounts(start = 1, end = 2)
        assertThat((0 until 6).map { pinnedSideAt(it, 6, pinned) })
            .containsExactly(PinnedSide.Start, null, null, null, PinnedSide.End, PinnedSide.End)
    }

    @Test
    fun `counts covering every column pin none`() {
        assertThat((0 until 4).map { pinnedSideAt(it, 4, PinnedCounts(2, 2)) }).containsExactly(null, null, null, null)
    }

    @Test
    fun `edge flags mark both block borders in one row`() {
        val scroll = ScrollState(initial = 0)
        val flags =
            (0 until 6).map {
                calculatePinnedColumnState(it, 6, PinnedCounts(2, 1), scroll, LayoutDirection.Ltr)
            }
        assertThat(flags.map { it.isLastStartPinned }).containsExactly(false, true, false, false, false, false)
        assertThat(flags.map { it.isLastBeforeEndPinned }).containsExactly(false, false, false, false, true, false)
        assertThat(flags.map { it.isFirstEndPinned }).containsExactly(false, false, false, false, false, true)
        assertThat(flags.map { it.edge })
            .containsExactly(
                PinnedEdge.None,
                PinnedEdge.End,
                PinnedEdge.None,
                PinnedEdge.None,
                PinnedEdge.None,
                PinnedEdge.Start,
            )
    }
}
