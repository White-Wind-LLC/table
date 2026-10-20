// The file is named after DefaultActiveFilterChip; both its declarations go in 4.0.
@file:Suppress("MatchingDeclarationName")

package ua.wwind.table

import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * What a column's chip in the active filters row knows, passed to [ColumnSpec.activeFilterChip].
 *
 * @property column key of the column the filter belongs to
 * @property state the column's current filter state
 * @property title the column's plain-text title, or its key when it has none
 * @property text the filter described for the chip, e.g. `contains "x"`; null while the filter panel is open
 *   and the filter no longer describes a value
 * @property isOpen whether the column's filter panel is open under this chip
 * @property strings the table's string provider, for the chip's own labels
 * @property onOpen opens the column's filter panel under the chip; does nothing where the row has no panels
 * @property onChange replaces the column's filter state, e.g. from a value picked on the chip itself
 * @property onClear resets the column's filter and closes its panel
 */
@Stable
public class TableFilterChipContext<C> internal constructor(
    public val column: C,
    public val state: TableFilterState<*>,
    public val title: String,
    public val text: String?,
    public val isOpen: Boolean,
    public val strings: StringProvider,
    public val onOpen: () -> Unit,
    public val onChange: (TableFilterState<*>) -> Unit,
    public val onClear: () -> Unit,
) {
    /** The default chip's label: `"$title: $text"`, or only the title while there is no text. */
    public val label: String get() = if (text != null) "$title: $text" else title
}

/**
 * The active filters row's default chip: an [InputChip] with [TableFilterChipContext.label] whose click opens
 * the filter panel and whose × resets the filter. A column's [ColumnSpec.activeFilterChip] can call it to
 * change one part of the chip and keep the rest.
 *
 * @param ctx the chip's context
 * @param modifier modifier for the chip
 * @param onClick called on a click on the chip; null makes only the × clickable. Such a chip is reported to
 *   accessibility services as disabled and keeps its colors, so mark it with [leadingIcon] or [label]
 * @param label the chip's label
 * @param leadingIcon optional icon before the label; give it a `contentDescription` when it carries meaning
 * @param colors the chip's colors; the chip is always selected, so the `selected*` colors are the ones shown
 * @param clearContentDescription accessibility label of the ×
 */
@Composable
@Suppress("LongParameterList")
public fun <C> DefaultActiveFilterChip(
    ctx: TableFilterChipContext<C>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = ctx.onOpen,
    label: @Composable () -> Unit = { Text(ctx.label) },
    leadingIcon: (@Composable () -> Unit)? = null,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
    clearContentDescription: String = ctx.strings.get(UiString.ColumnMenuClearFilter),
) {
    FilterInputChip(
        onClick = onClick,
        onClear = ctx.onClear,
        label = label,
        leadingIcon = leadingIcon,
        colors = colors,
        clearContentDescription = clearContentDescription,
        modifier = modifier,
    )
}
