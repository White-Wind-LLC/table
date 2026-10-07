package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.table.TableEmptyScope

/**
 * How long rows must keep loading before the indicator shows. The pager reports one global state
 * and goes `Loading` on every scroll preload; most of those pulses are shorter than this.
 */
internal const val LOADING_INDICATOR_DELAY_MS: Long = 400

/** What the paged table hands the core table for the current load state. */
internal class PagedLoadSlots(
    /** Core `emptyContent`: the body while there are no rows. */
    val emptyContent: @Composable TableEmptyScope.() -> Unit,
    /** Core `bodyOverlay`: drawn over the rows area. */
    val bodyOverlay: @Composable BoxScope.() -> Unit,
)

private val NoRetry: (Int) -> Unit = {}

@Suppress("LongParameterList")
@Composable
internal fun pagedLoadSlots(
    items: PagingData<*>?,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable PagedTableErrorScope.() -> Unit,
    emptyContent: @Composable TableEmptyScope.() -> Unit,
    loadingIndicator: (@Composable () -> Unit)?,
    errorBar: (@Composable PagedTableErrorScope.() -> Unit)?,
): PagedLoadSlots {
    val bodyState = pagedBodyState(items)
    val loadError = items?.loadState as? LoadState.Error
    LogLoadError(loadError)
    val retry = items?.retry ?: NoRetry
    val errorScope = remember(loadError, retry) { loadError?.let { PagingErrorScope(it, retry) } }
    val showIndicator =
        delayedFlag(
            active = loadingIndicator != null && bodyState is PagedBodyState.Rows && bodyState.loading,
            delayMillis = LOADING_INDICATOR_DELAY_MS,
        )
    val barScope = errorScope.takeIf { bodyState is PagedBodyState.Rows }

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
    val overlay: @Composable BoxScope.() -> Unit = {
        if (showIndicator && loadingIndicator != null) {
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()) { loadingIndicator() }
        }
        if (barScope != null && errorBar != null) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { barScope.errorBar() }
        }
    }
    return PagedLoadSlots(emptyContent = body, bodyOverlay = overlay)
}

/** [active], but only once it has stayed true for [delayMillis]; false the moment it drops. */
@Composable
private fun delayedFlag(
    active: Boolean,
    delayMillis: Long,
): Boolean {
    var elapsed by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        elapsed = false
        if (active) {
            delay(delayMillis)
            elapsed = true
        }
    }
    return active && elapsed
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
