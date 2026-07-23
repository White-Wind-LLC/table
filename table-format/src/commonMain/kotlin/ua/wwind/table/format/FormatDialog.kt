package ua.wwind.table.format

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.format.component.rememberFormatDialogState
import ua.wwind.table.format.data.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.scrollbar.VerticalScrollbarRenderer
import ua.wwind.table.strings.StringProvider

/**
 * Modal editor for a table's conditional-formatting rules, rendered in a Material3 `AlertDialog`. Shares its title,
 * buttons and body with [FormatDialogContent], which hosts the same editor in a caller-supplied container.
 */
@Composable
@Suppress("LongParameterList")
public fun <E : Enum<E>, FILTER> FormatDialog(
    showDialog: Boolean,
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
    getTitle: @Composable (E) -> String,
    filters: (TableFormatRule<E, FILTER>, onApply: (TableFormatRule<E, FILTER>) -> Unit) -> List<FormatFilterData<E>>,
    entries: ImmutableList<E>,
    key: Any,
    strings: StringProvider,
    onDismissRequest: () -> Unit,
    settings: FormatDialogSettings = FormatDialogSettings(),
    scrollbarRenderer: VerticalScrollbarRenderer? = null,
    colors: FormatDialogColors = FormatDialogDefaults.colors(),
) {
    if (!showDialog) return
    val state = rememberFormatDialogState<E, FILTER>(settings)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colors.containerColor,
        titleContentColor = colors.titleContentColor,
        textContentColor = colors.textContentColor,
        tonalElevation = colors.tonalElevation,
        confirmButton = { FormatDialogButtons(state, rules, onRulesChange, getNewRule) },
        title = { FormatDialogTitle(state, strings, colors.titleContentColor, onDismissRequest) },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
        text = {
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
            )
        },
    )
}
