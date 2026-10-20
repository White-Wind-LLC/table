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
import ua.wwind.table.state.clearComplexFilters
import ua.wwind.table.strings.UiString
import ua.wwind.table.strings.currentStrings

/** Receiver of the table's `emptyContent` slot, shown in the body while the table has no rows. */
@Stable
public interface TableEmptyScope {
    /** Whether any filter, including nested ones, is active — the rows may exist but be filtered out. */
    public val isFiltered: Boolean

    /**
     * Whether the filter has unresolved conditions, so the app shows no rows until they are removed
     * (see [ua.wwind.table.state.TableState.unresolvedFilters]).
     */
    public val hasUnresolvedFilters: Boolean get() = false

    /** Removes every filter, including nested ones. */
    public fun clearFilters()
}

internal class TableStateEmptyScope<C>(
    private val state: TableState<C>,
) : TableEmptyScope {
    override val isFiltered: Boolean
        get() = state.filterExpression.isActive()

    override val hasUnresolvedFilters: Boolean
        get() = state.unresolvedFilters.isNotEmpty()

    override fun clearFilters() {
        state.filters.clear()
        state.clearComplexFilters()
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

/**
 * Default empty state: "No data", or "No results" with a button clearing the filters; while the filter has
 * unresolved conditions, it says so instead of "No results".
 */
@Composable
internal fun TableEmptyScope.DefaultTableEmptyContent() {
    val strings = currentStrings()
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val icon =
            when {
                hasUnresolvedFilters -> TableIcons.Warning
                isFiltered -> TableIcons.FilterAltOutlined
                else -> TableIcons.TableRows
            }
        val text =
            when {
                hasUnresolvedFilters -> UiString.FilterUnresolvedBlocked
                isFiltered -> UiString.EmptyNoResults
                else -> UiString.EmptyNoData
            }
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = strings.get(text),
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
