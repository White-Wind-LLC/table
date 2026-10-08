package ua.wwind.table.filter.component.fast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.buildFilterChipTextUnsafe
import ua.wwind.table.component.TableTextField
import ua.wwind.table.component.TableTextFieldDefaults
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.filter.data.isActive
import ua.wwind.table.filter.data.isNullCheck
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/** How a fast filter shows the operator a filter got from the panel (issue #105). */
internal sealed interface FastFilterMode {
    /**
     * The fast field edits the value under the filter's operator.
     *
     * @property hint operator to show before the value; null when it is the fast field's default
     */
    data class Editable(
        val hint: FilterConstraint?,
    ) : FastFilterMode

    /** The operator takes no value or several, which one fast field cannot edit: show a read-only summary. */
    data object Locked : FastFilterMode
}

/**
 * Picks the [FastFilterMode] for [state] of a [type] column. An inactive state edits under the fast
 * default, as the fast state holders do, so it never gets a hint.
 */
internal fun fastFilterMode(
    type: TableFilterType<*>,
    state: TableFilterState<*>?,
): FastFilterMode {
    val constraint = state?.takeIf { it.isActive() }?.constraint ?: return FastFilterMode.Editable(null)
    if (constraint.isNullCheck()) return FastFilterMode.Locked
    return when (type) {
        is TableFilterType.TextTableFilter -> {
            FastFilterMode.Editable(constraint.takeUnless { it == FilterConstraint.CONTAINS })
        }

        is TableFilterType.NumberTableFilter<*>, is TableFilterType.DateTableFilter -> {
            when (constraint) {
                FilterConstraint.EQUALS -> FastFilterMode.Editable(null)
                FilterConstraint.BETWEEN -> FastFilterMode.Locked
                else -> FastFilterMode.Editable(constraint)
            }
        }

        is TableFilterType.EnumTableFilter<*> -> {
            enumFastFilterMode(constraint, state.values.orEmpty().size)
        }

        else -> {
            FastFilterMode.Editable(null)
        }
    }
}

/** A single IN / NOT_IN value reads as equals / not equals; several values need the panel. */
private fun enumFastFilterMode(
    constraint: FilterConstraint,
    valueCount: Int,
): FastFilterMode =
    when {
        valueCount > 1 -> {
            FastFilterMode.Locked
        }

        constraint == FilterConstraint.NOT_EQUALS || constraint == FilterConstraint.NOT_IN -> {
            FastFilterMode.Editable(FilterConstraint.NOT_EQUALS)
        }

        else -> {
            FastFilterMode.Editable(null)
        }
    }

/** Comparison operators read as their symbol in any language; the rest use their localized name. */
@Composable
internal fun fastOperatorHint(
    constraint: FilterConstraint,
    strings: StringProvider,
): String =
    when (constraint) {
        FilterConstraint.EQUALS -> "="
        FilterConstraint.NOT_EQUALS -> "≠"
        FilterConstraint.GT -> ">"
        FilterConstraint.GTE -> "≥"
        FilterConstraint.LT -> "<"
        FilterConstraint.LTE -> "≤"
        else -> strings.get(constraint.toUiString())
    }

/** The × that clears a fast filter. */
@Composable
private fun FastClearButton(
    strings: StringProvider,
    onClear: () -> Unit,
) {
    IconButton(onClick = onClear) {
        Icon(
            painter = painterResource(TableIcons.Close),
            contentDescription = strings.get(UiString.FilterClear),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * A fast filter [field] between its operator hint and its clear ×. Both sit beside the field rather
 * than in its prefix and trailing slots: the outlined decoration hides a prefix, and a dropdown or
 * date field opens on any press inside it, so a × there would open it as well.
 */
@Composable
internal fun FastFieldFrame(
    mode: FastFilterMode,
    showClear: Boolean,
    strings: StringProvider,
    onClear: () -> Unit,
    field: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (mode as? FastFilterMode.Editable)?.hint?.let { hint ->
            Text(
                text = fastOperatorHint(hint, strings),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Box(modifier = Modifier.weight(1f)) { field() }
        if (showClear) FastClearButton(strings = strings, onClear = onClear)
    }
}

/**
 * Read-only fast field for a [FastFilterMode.Locked] filter: a summary of it, worded as the active
 * filter chips are, and a × that clears it. Editing it takes the filter panel.
 */
@Composable
internal fun FastLockedField(
    type: TableFilterType<*>,
    state: TableFilterState<*>,
    strings: StringProvider,
    onClear: () -> Unit,
) {
    FastFieldFrame(mode = FastFilterMode.Locked, showClear = true, strings = strings, onClear = onClear) {
        TableTextField(
            value = buildFilterChipTextUnsafe(type, state, strings).orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            contentPadding = TableTextFieldDefaults.reducedContentPadding(),
            showBorder = false,
        )
    }
}
