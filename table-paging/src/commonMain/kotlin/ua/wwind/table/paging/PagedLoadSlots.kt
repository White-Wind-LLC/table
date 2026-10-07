package ua.wwind.table.paging

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.table.TableEmptyScope

/** What the paged table hands the core table for the current load state. */
internal class PagedLoadSlots(
    /** Core `emptyContent`: the body while there are no rows. */
    val emptyContent: @Composable TableEmptyScope.() -> Unit,
    /** Core `bodyOverlay`: drawn over the rows area. */
    val bodyOverlay: @Composable BoxScope.() -> Unit,
)

private val NoRetry: (Int) -> Unit = {}

@Composable
internal fun pagedLoadSlots(
    items: PagingData<*>?,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable PagedTableErrorScope.() -> Unit,
    emptyContent: @Composable TableEmptyScope.() -> Unit,
): PagedLoadSlots {
    val bodyState = pagedBodyState(items)
    val loadError = items?.loadState as? LoadState.Error
    LogLoadError(loadError)
    val retry = items?.retry ?: NoRetry
    val errorScope = remember(loadError, retry) { loadError?.let { PagingErrorScope(it, retry) } }

    val body: @Composable TableEmptyScope.() -> Unit =
        when (bodyState) {
            PagedBodyState.Loading -> {
                { loadingContent() }
            }

            is PagedBodyState.Failed -> {
                { errorScope?.errorContent() }
            }

            // Rows: the core table never shows the empty slot while it has rows.
            PagedBodyState.Empty, is PagedBodyState.Rows -> {
                emptyContent
            }
        }
    return PagedLoadSlots(emptyContent = body, bodyOverlay = {})
}

/** Logs each distinct failure once, as the deprecated `handleLoadState` did. */
@Composable
private fun LogLoadError(error: LoadState.Error?) {
    LaunchedEffect(error) {
        if (error != null) {
            Logger.e(error.throwable) { "Paged table load failed at key=${error.key}" }
        }
    }
}
