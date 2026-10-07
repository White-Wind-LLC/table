package ua.wwind.table.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/** Default load-state content of the paged `Table`. */
public object PagedTableDefaults {
    /** Body while the first rows load: a centred progress indicator. */
    public val LoadingContent: @Composable () -> Unit = { DefaultPagedLoadingContent() }

    /** Body when the first load fails: "Couldn't load data" with a Retry button. */
    public val ErrorContent: @Composable PagedTableErrorScope.() -> Unit = { DefaultPagedErrorContent() }

    /** Shown at the top of the rows while more rows load: a full-width linear progress bar. */
    public val LoadingIndicator: @Composable () -> Unit = { DefaultPagedLoadingIndicator() }

    /** Shown at the bottom of the rows when a load fails: "Couldn't load some rows" with Retry. */
    public val ErrorBar: @Composable PagedTableErrorScope.() -> Unit = { DefaultPagedErrorBar() }
}

@Composable
internal fun DefaultPagedLoadingContent() {
    val description = currentStrings().get(UiString.PagingLoading)
    CircularProgressIndicator(Modifier.padding(32.dp).semantics { contentDescription = description })
}

/** Laid out like the core empty state, so the body does not jump between the two. */
@Composable
internal fun PagedTableErrorScope.DefaultPagedErrorContent() {
    val strings = currentStrings()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(TableIcons.ErrorOutline),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = strings.get(UiString.PagingLoadError),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = ::retry) {
            Text(strings.get(UiString.PagingRetry))
        }
    }
}

@Composable
internal fun DefaultPagedLoadingIndicator() {
    val description = currentStrings().get(UiString.PagingLoading)
    LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
}

@Composable
internal fun PagedTableErrorScope.DefaultPagedErrorBar() {
    val strings = currentStrings()
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(TableIcons.ErrorOutline), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = strings.get(UiString.PagingLoadMoreError),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = ::retry,
                colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
            ) {
                Text(strings.get(UiString.PagingRetry))
            }
        }
    }
}
