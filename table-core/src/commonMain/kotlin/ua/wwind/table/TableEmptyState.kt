package ua.wwind.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/** Receiver of the table's `emptyContent` slot, shown in the body while the table has no rows. */
@Stable
public interface TableEmptyScope {
    /** Whether any column filter is active — the rows may exist but be filtered out. */
    public val isFiltered: Boolean

    /** Removes every column filter. */
    public fun clearFilters()
}

internal class TableStateEmptyScope<C>(
    private val state: TableState<C>,
) : TableEmptyScope {
    override val isFiltered: Boolean
        get() = state.filters.values.any { it.isActive() }

    override fun clearFilters() {
        state.filters.clear()
    }
}

/** Centres [content] across the body's width; adds no height of its own, so an empty slot stays blank. */
@Composable
internal fun TableEmptyStateBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
}

/** Default empty state: "No data", or "No results" with a button clearing the filters. */
@Composable
internal fun TableEmptyScope.DefaultTableEmptyContent() {
    val strings = currentStrings()
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(if (isFiltered) TableIcons.FilterAltOutlined else TableIcons.TableRows),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = strings.get(if (isFiltered) UiString.EmptyNoResults else UiString.EmptyNoData),
            color = color,
            textAlign = TextAlign.Center,
        )
        if (isFiltered) {
            TextButton(onClick = ::clearFilters) {
                Text(strings.get(UiString.EmptyClearFilters))
            }
        }
    }
}
