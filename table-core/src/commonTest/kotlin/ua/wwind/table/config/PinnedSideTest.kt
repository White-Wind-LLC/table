package ua.wwind.table.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class PinnedSideTest {
    @Suppress("DEPRECATION")
    @Test
    fun `deprecated Left and Right are the logical Start and End`() {
        assertThat(PinnedSide.Left).isSameInstanceAs(PinnedSide.Start)
        assertThat(PinnedSide.Right).isSameInstanceAs(PinnedSide.End)
    }

    @Test
    fun `the default pins the leading columns`() {
        assertThat(TableSettings().pinnedColumnsSide).isEqualTo(PinnedSide.Start)
    }
}
