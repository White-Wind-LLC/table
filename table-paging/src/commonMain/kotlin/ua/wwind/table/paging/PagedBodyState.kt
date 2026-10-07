package ua.wwind.table.paging

import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData

/**
 * What a paged table shows for one snapshot. The pager reports a single global [LoadState] over a
 * list whose total size is known up front, so "no rows yet" and "rows on screen" are the only
 * axes: without rows the state fills the body; with rows it is an indicator or an error bar.
 */
internal sealed interface PagedBodyState {
    /** No rows, and the first ones are on their way: no snapshot yet, or size 0 while loading. */
    data object Loading : PagedBodyState

    /** No rows, because loading them failed. */
    data class Failed(
        val error: LoadState.Error,
    ) : PagedBodyState

    /** Loaded, and the source has no rows. */
    data object Empty : PagedBodyState

    /** Rows (loaded or placeholders) are on screen; some may be loading or have failed. */
    data class Rows(
        val loading: Boolean,
        val error: LoadState.Error?,
    ) : PagedBodyState
}

internal fun pagedBodyState(items: PagingData<*>?): PagedBodyState {
    if (items == null) return PagedBodyState.Loading
    val loadState = items.loadState
    if (items.data.size > 0) {
        return PagedBodyState.Rows(loading = loadState == LoadState.Loading, error = loadState as? LoadState.Error)
    }
    return when (loadState) {
        LoadState.Loading -> PagedBodyState.Loading
        is LoadState.Error -> PagedBodyState.Failed(loadState)
        LoadState.Success -> PagedBodyState.Empty
    }
}
