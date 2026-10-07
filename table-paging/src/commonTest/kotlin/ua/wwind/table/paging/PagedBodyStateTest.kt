package ua.wwind.table.paging

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.collections.immutable.persistentMapOf
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.paging.core.PagingMap
import kotlin.test.Test

/** Every cell of the spec's mapping table: size zero vs. rows, against each load state. */
class PagedBodyStateTest {
    private val error = LoadState.Error(IllegalStateException("boom"), key = 40)

    private fun data(
        size: Int,
        loadState: LoadState,
    ) = PagingData(
        data = PagingMap<String>(size = size, values = persistentMapOf(), onGet = {}),
        loadState = loadState,
        retry = {},
    )

    @Test
    fun `no snapshot yet is loading`() {
        assertThat(pagedBodyState(null)).isEqualTo(PagedBodyState.Loading)
    }

    @Test
    fun `size zero while loading is loading`() {
        assertThat(pagedBodyState(data(0, LoadState.Loading))).isEqualTo(PagedBodyState.Loading)
    }

    @Test
    fun `size zero with an error is failed`() {
        assertThat(pagedBodyState(data(0, error))).isEqualTo(PagedBodyState.Failed(error))
    }

    @Test
    fun `size zero after success is empty`() {
        assertThat(pagedBodyState(data(0, LoadState.Success))).isEqualTo(PagedBodyState.Empty)
    }

    @Test
    fun `rows while loading are rows that are loading`() {
        assertThat(pagedBodyState(data(3, LoadState.Loading)))
            .isEqualTo(PagedBodyState.Rows(loading = true, error = null))
    }

    @Test
    fun `rows with an error carry the error`() {
        assertThat(pagedBodyState(data(3, error)))
            .isEqualTo(PagedBodyState.Rows(loading = false, error = error))
    }

    @Test
    fun `rows after success are settled`() {
        assertThat(pagedBodyState(data(3, LoadState.Success)))
            .isEqualTo(PagedBodyState.Rows(loading = false, error = null))
    }
}
