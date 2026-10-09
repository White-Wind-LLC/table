package ua.wwind.table.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class PinnedSideTest {
    @Test
    fun `the default pins the leading columns`() {
        assertThat(TableSettings().pinnedColumnsSide).isEqualTo(PinnedSide.Start)
    }
}
