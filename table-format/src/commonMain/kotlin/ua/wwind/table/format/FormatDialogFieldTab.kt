package ua.wwind.table.format

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.format.data.TableFormatRule

@Composable
public fun <E : Enum<E>, FILTER> FormatDialogFieldTab(
    item: TableFormatRule<E, FILTER>,
    entries: ImmutableList<E>,
    getTitle: @Composable (E) -> String,
    onChange: (TableFormatRule<E, FILTER>) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Box {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState) {
            items(entries) { field ->
                ListItem(
                    headlineContent = {
                        Text(getTitle(field))
                    },
                    leadingContent = {
                        Checkbox(
                            checked = item.columns.contains(field),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    onChange(item.copy(columns = item.columns + field))
                                } else {
                                    onChange(item.copy(columns = item.columns - field))
                                }
                            },
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
