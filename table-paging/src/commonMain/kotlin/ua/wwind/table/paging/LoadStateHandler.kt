package ua.wwind.table.paging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import co.touchlab.kermit.Logger
import ua.wwind.paging.core.LoadState
import ua.wwind.paging.core.PagingData
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/**
 * Renders load states as list items. The paged `Table` now shows every load state itself.
 */
@Deprecated(
    message =
        "The paged Table renders load states itself; use its loadingContent, errorContent, " +
            "emptyContent, loadingIndicator and errorBar parameters. Removed in 4.0.",
    level = DeprecationLevel.ERROR,
)
public fun <T : Any> LazyListScope.handleLoadState(
    data: PagingData<T>,
    width: Dp? = null,
    height: Dp? = null,
) {
    val sizeModifier =
        Modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .then(if (height != null) Modifier.height(height) else Modifier.fillMaxHeight())
    when (data.loadState) {
        is LoadState.Success if data.data.size < 1 -> {
            item {
                Box(
                    modifier = sizeModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = currentStrings().get(UiString.EmptyNoData),
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        is LoadState.Loading -> {
            item {
                Box(
                    modifier = sizeModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is LoadState.Error -> {
            val errorState = data.loadState as LoadState.Error
            Logger.e(errorState.throwable) {
                "Paging error in AppTable. key=${errorState.key}, message=${errorState.throwable.message}"
            }
        }

        else -> { /* NoOp */ }
    }
}
