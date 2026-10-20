package ua.wwind.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.UiString

/** Radius of the round press and hover indication around a chip's ×; fits the 32 dp chip. */
private val ClearIconRippleRadius = 12.dp

private const val BADGE_BACKGROUND_ALPHA = 0.12f

/** Chip colors of the enclosing active filters row; null outside one. */
internal val LocalFilterChipColors: ProvidableCompositionLocal<TableFilterChipColors?> =
    staticCompositionLocalOf { null }

/**
 * Colors of each kind of filter chip. Chips are always selected, so the `selected*` colors are the ones shown.
 *
 * @property condition a condition on a known column
 * @property group an AND/OR group
 * @property negatedGroup a negated group or condition
 * @property unknown a condition on a column the table does not know
 */
@Immutable
public data class TableFilterChipColors(
    val condition: SelectableChipColors,
    val group: SelectableChipColors,
    val negatedGroup: SelectableChipColors,
    val unknown: SelectableChipColors,
)

/**
 * How the table draws its filter chips, in the active filters row and in a group's popover.
 *
 * @property colors colors of each chip kind
 * @property unknownTitle a readable title for a column key the table does not know; null or a null result
 *   shows the key itself
 * @property unresolvedTitle a readable title for an unresolved condition from its raw key and payload; null or
 *   a null result shows the key itself
 * @property chip draws every chip in place of [DefaultFilterChip], which it can call to keep parts of the
 *   default; the library still owns the popover and its navigation
 */
@Immutable
public data class TableFilterChipStyle<C>(
    val colors: TableFilterChipColors,
    val unknownTitle: ((C) -> String?)? = null,
    val unresolvedTitle: ((key: String, payload: Any?) -> String?)? = null,
    val chip: (@Composable (TableFilterChipNode<C>) -> Unit)? = null,
)

/** Defaults for [TableFilterChipStyle]. */
public object TableFilterChipDefaults {
    /**
     * Colors of each chip kind: conditions keep the plain input chip look, groups use the primary
     * container, negated groups the error container and unknown columns the tertiary container.
     */
    @Composable
    public fun colors(
        condition: SelectableChipColors = InputChipDefaults.inputChipColors(),
        group: SelectableChipColors =
            containerColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
        negatedGroup: SelectableChipColors =
            containerColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
        unknown: SelectableChipColors =
            containerColors(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            ),
    ): TableFilterChipColors = TableFilterChipColors(condition, group, negatedGroup, unknown)

    /** A [TableFilterChipStyle] with the default [colors]. */
    @Composable
    public fun <C> style(
        colors: TableFilterChipColors = colors(),
        unknownTitle: ((C) -> String?)? = null,
        unresolvedTitle: ((key: String, payload: Any?) -> String?)? = null,
        chip: (@Composable (TableFilterChipNode<C>) -> Unit)? = null,
    ): TableFilterChipStyle<C> = TableFilterChipStyle(colors, unknownTitle, unresolvedTitle, chip)

    @Composable
    private fun containerColors(
        container: Color,
        content: Color,
    ): SelectableChipColors =
        InputChipDefaults.inputChipColors(
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedLeadingIconColor = content,
            selectedTrailingIconColor = content,
        )
}

/**
 * The library's filter chip: an [InputChip] whose click opens the condition's panel or the group's popover
 * and whose × removes the node. A [TableFilterChipStyle.chip] can call it to change one part of a chip and
 * keep the rest.
 *
 * @param node the chip's node
 * @param modifier modifier for the chip
 * @param onClick called on a click on the chip; null makes only the × clickable, as for an unknown column or a
 *   node whose `canOpen` is false. Such a chip is reported to accessibility services as disabled and keeps its
 *   colors
 * @param label the chip's label; a condition's defaults to its column's [TableFilterChipContent.label] or
 *   [TableFilterChipNode.Condition.label], a group's to its badges, text and an expand arrow
 * @param leadingIcon optional icon before the label; give it a `contentDescription` when it carries meaning
 * @param colors the chip's colors; the chip is always selected, so the `selected*` colors are the ones shown
 * @param clearContentDescription accessibility label of the ×
 */
@Composable
@Suppress("LongParameterList")
public fun <C> DefaultFilterChip(
    node: TableFilterChipNode<C>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = node.defaultOnClick(),
    label: @Composable () -> Unit = { DefaultFilterChipLabel(node) },
    leadingIcon: (@Composable () -> Unit)? = node.defaultLeadingIcon(),
    colors: SelectableChipColors = (LocalFilterChipColors.current ?: TableFilterChipDefaults.colors()).of(node),
    clearContentDescription: String = node.defaultClearContentDescription(),
) {
    FilterInputChip(
        onClick = onClick,
        onClear = node.onClear,
        label = label,
        leadingIcon = leadingIcon,
        colors = colors,
        clearContentDescription = clearContentDescription,
        modifier = modifier,
    )
}

/** The input chip shell shared by [DefaultFilterChip] and [DefaultActiveFilterChip]. */
@Composable
@Suppress("LongParameterList")
internal fun FilterInputChip(
    onClick: (() -> Unit)?,
    onClear: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)?,
    colors: SelectableChipColors,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
) {
    InputChip(
        selected = true,
        onClick = onClick ?: NoClick,
        label = label,
        enabled = onClick != null,
        leadingIcon = leadingIcon,
        trailingIcon = {
            Icon(
                painter = painterResource(TableIcons.Close),
                contentDescription = clearContentDescription,
                // A round, unbounded indication like an IconButton's, without its 48 dp target that would
                // make the chip taller.
                modifier =
                    Modifier.clickable(
                        interactionSource = null,
                        indication = ripple(bounded = false, radius = ClearIconRippleRadius),
                        role = Role.Button,
                        onClick = onClear,
                    ),
            )
        },
        // A chip without a click is disabled only so that it does not react; it keeps the look of an active one.
        colors =
            colors.copy(
                disabledSelectedContainerColor = colors.selectedContainerColor,
                disabledLabelColor = colors.selectedLabelColor,
                disabledLeadingIconColor = colors.selectedLeadingIconColor,
                disabledTrailingIconColor = colors.selectedTrailingIconColor,
            ),
        border = InputChipDefaults.inputChipBorder(enabled = true, selected = true),
        modifier = modifier,
    )
}

private val NoClick: () -> Unit = {}

private fun <C> TableFilterChipNode<C>.defaultOnClick(): (() -> Unit)? =
    when (this) {
        is TableFilterChipNode.Condition -> onOpen.takeIf { canOpen }
        is TableFilterChipNode.Group -> onOpen.takeIf { canOpen }
        is TableFilterChipNode.Unknown, is TableFilterChipNode.Unresolved -> null
    }

private fun <C> TableFilterChipNode<C>.defaultLeadingIcon(): (@Composable () -> Unit)? {
    val condition = this as? TableFilterChipNode.Condition ?: return null
    val icon = condition.content?.leadingIcon ?: return null
    return { icon(condition) }
}

private fun <C> TableFilterChipColors.of(node: TableFilterChipNode<C>): SelectableChipColors =
    when (node) {
        is TableFilterChipNode.Condition -> condition
        is TableFilterChipNode.Group -> if (node.negated) negatedGroup else group
        is TableFilterChipNode.Unknown, is TableFilterChipNode.Unresolved -> unknown
    }

@Composable
private fun <C> TableFilterChipNode<C>.defaultClearContentDescription(): String =
    when (this) {
        is TableFilterChipNode.Condition -> {
            when (placement) {
                TableFilterChipPlacement.Row -> strings.get(UiString.ColumnMenuClearFilter)
                TableFilterChipPlacement.Tree -> strings.removeFilterLabel(title)
            }
        }

        is TableFilterChipNode.Group -> {
            strings.removeFilterGroupLabel(columnTitles.joinToString())
        }

        is TableFilterChipNode.Unknown -> {
            strings.removeFilterLabel(title)
        }

        is TableFilterChipNode.Unresolved -> {
            strings.removeFilterLabel(title)
        }
    }

@Composable
private fun <C> DefaultFilterChipLabel(node: TableFilterChipNode<C>) {
    when (node) {
        is TableFilterChipNode.Condition -> {
            val content = node.content
            if (content != null) content.label(node) else Text(node.label)
        }

        is TableFilterChipNode.Group -> {
            GroupLabel(node)
        }

        is TableFilterChipNode.Unknown -> {
            WarningLabel(node.title, node.strings.get(UiString.FilterUnknownField))
        }

        is TableFilterChipNode.Unresolved -> {
            WarningLabel(node.title, node.strings.get(UiString.FilterUnknownField))
        }
    }
}

@Composable
private fun WarningLabel(
    title: String,
    iconDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(TableIcons.Warning),
            contentDescription = iconDescription,
            modifier = Modifier.size(InputChipDefaults.IconSize),
        )
        Text(title)
    }
}

@Composable
private fun <C> GroupLabel(node: TableFilterChipNode.Group<C>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = node.description },
    ) {
        // The description already names the unresolved leaves.
        if (node.hasUnresolved) {
            Icon(
                painter = painterResource(TableIcons.Warning),
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        }
        if (node.negated) FilterChipBadge(node.strings.get(UiString.FilterNot))
        if (node.isGroup) FilterChipBadge(node.strings.filterOperatorBadge(node.operator, node.itemCount))
        Text(node.text)
        Icon(
            painter = painterResource(TableIcons.KeyboardArrowDown),
            contentDescription = null,
            modifier = Modifier.size(InputChipDefaults.IconSize),
        )
    }
}

/** A small tag on a filter chip, e.g. a group's `NOT` or its operator. */
@Composable
internal fun FilterChipBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalContentColor.current.copy(alpha = BADGE_BACKGROUND_ALPHA),
        contentColor = LocalContentColor.current,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
