package ua.wwind.table.paging

import androidx.compose.runtime.Stable
import ua.wwind.paging.core.LoadState

/** Receiver of the paged table's `errorContent` and `errorBar` slots. */
@Stable
public interface PagedTableErrorScope {
    /** The failure the pager reported. */
    public val error: Throwable

    /** Loads the failed position again. */
    public fun retry()
}

internal class PagingErrorScope(
    private val loadError: LoadState.Error,
    private val onRetry: (key: Int) -> Unit,
) : PagedTableErrorScope {
    override val error: Throwable
        get() = loadError.throwable

    override fun retry() {
        onRetry(loadError.key)
    }
}
