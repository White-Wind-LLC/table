package ua.wwind.table.paging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/** Default load-state content of the paged `Table`. */
public object PagedTableDefaults {
    /** Body while the first rows load: a centred progress indicator. */
    public val LoadingContent: @Composable () -> Unit = { DefaultPagedLoadingContent() }

    /** Body when the first load fails: "Couldn't load data" with a Retry button. */
    public val ErrorContent: @Composable PagedTableErrorScope.() -> Unit = { DefaultPagedErrorContent() }
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
            imageVector = TableIcons.ErrorOutline,
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
