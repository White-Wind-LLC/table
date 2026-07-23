package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.format.component.rememberFormatDialogState
import ua.wwind.table.format.data.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.strings.StringProvider

/**
 * Conditional-formatting UI as portable content: host it in any container (a custom dialog host, a side panel, a
 * split pane) instead of only [FormatDialog]. The container owns visibility, scrim, background and elevation; this
 * composable applies only [FormatDialogColors.titleContentColor] and [FormatDialogColors.textContentColor] from
 * [colors]. The host MUST impose a bounded height (a fixed height or a fill-height container): the rule list is a
 * vertical viewport, so an unbounded-height parent (e.g. a scrolling Column) throws at composition. [key] resets the
 * in-flight edit list when it changes. Pass [onDismissRequest] `= null` (the default) to hide the close button for
 * embedded, non-modal placements.
 */
@Composable
@Suppress("LongParameterList")
public fun <E : Enum<E>, FILTER> FormatDialogContent(
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    entries: ImmutableList<E>,
    key: Any,
    strings: StringProvider,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    colors: FormatDialogColors = FormatDialogDefaults.colors(),
    settings: FormatDialogSettings = FormatDialogSettings(),
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
) {
    val state = rememberFormatDialogState<E, FILTER>(settings)
    Column(modifier = modifier.fillMaxSize()) {
        FormatDialogTitle(state, strings, colors.titleContentColor, onDismissRequest)
        HorizontalDivider()
        CompositionLocalProvider(LocalContentColor provides colors.textContentColor) {
            FormatDialogBody(
                state = state,
                rules = rules,
                onRulesChange = onRulesChange,
                getTitle = getTitle,
                filters = filters,
                entries = entries,
                key = key,
                strings = strings,
                settings = settings,
                scrollbarRenderer = scrollbarRenderer,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            FormatDialogButtons(state, rules, onRulesChange, getNewRule)
        }
    }
}
