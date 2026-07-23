package ua.wwind.table.format

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class InfraSmokeTest {
    @Test
    fun `test source set compiles and runs`() {
        assertThat(1 + 1).isEqualTo(2)
    }
}
