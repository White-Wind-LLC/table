package ua.wwind.table.sample.app.components

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ua.wwind.table.sample.data.createDemoData
import kotlin.test.Test

class DemoPagingSourceTest {
    private val people = createDemoData()

    @Test
    fun `a page holds the requested positions`() =
        runTest {
            val portion = DemoPagingSource(people).read(position = 2, size = 3).first()
            assertThat(portion.totalSize).isEqualTo(people.size)
            assertThat(portion.values.keys.sorted()).isEqualTo(listOf(2, 3, 4))
            assertThat(portion.values[2]).isEqualTo(people[2])
        }

    @Test
    fun `an empty dataset reports no rows`() =
        runTest {
            val source = DemoPagingSource(people).apply { empty = true }
            val portion = source.read(position = 0, size = 20).first()
            assertThat(portion.totalSize).isEqualTo(0)
            assertThat(portion.values.size).isEqualTo(0)
        }

    @Test
    fun `failing loads throw`() =
        runTest {
            val source = DemoPagingSource(people).apply { failLoads = true }
            assertFailure { source.read(position = 0, size = 20).first() }
                .isInstanceOf(IllegalStateException::class)
        }
}
