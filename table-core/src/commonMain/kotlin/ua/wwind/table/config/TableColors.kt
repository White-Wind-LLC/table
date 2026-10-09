package ua.wwind.table.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import kotlin.math.max
import kotlin.math.min

/** Color palette used by the table header and rows. */
@Immutable
public data class TableColors(
    val headerContainerColor: Color,
    val headerContentColor: Color,
    val rowContainerColor: Color,
    val rowSelectedContainerColor: Color,
    val stripedRowContainerColor: Color,
    val groupContainerColor: Color,
    val footerContainerColor: Color,
    val footerContentColor: Color,
    /**
     * Container painted behind a row block declared via `rowBlocks`. Appended with a default —
     * never inserted as a required parameter — so pre-blocks positional constructor calls keep
     * compiling; [Color.Unspecified] resolves to `surfaceContainerHighest` at draw.
     */
    val rowBlockContainerColor: Color = Color.Unspecified,
    /**
     * Leading bar drawn on the selected row, so selection is not shown by color alone. Appended with
     * a default like [rowBlockContainerColor]; [Color.Unspecified] resolves to `primary` at draw, and
     * [Color.Transparent] hides the bar.
     */
    val rowSelectedIndicatorColor: Color = Color.Unspecified,
    /** Row, column, header and footer dividers. [Color.Unspecified] resolves to `outlineVariant`. */
    val dividerColor: Color = Color.Unspecified,
    /** Divider at the edge of the pinned columns. [Color.Unspecified] resolves to `outlineVariant`. */
    val pinnedDividerColor: Color = Color.Unspecified,
    /** Outer border of the table. [Color.Unspecified] resolves to `outlineVariant`. */
    val borderColor: Color = Color.Unspecified,
    /** Keyboard focus ring of a cell or header. [Color.Unspecified] resolves to `primary`. */
    val focusIndicatorColor: Color = Color.Unspecified,
    /**
     * State layer shown on hover and press of a row; the ripple applies its own alpha.
     * [Color.Unspecified] resolves to `onSurface`.
     */
    val hoverColor: Color = Color.Unspecified,
    /** Content of a group header. [Color.Unspecified] resolves to the content color for [groupContainerColor]. */
    val groupContentColor: Color = Color.Unspecified,
    /** Group header pinned to the top while its rows scroll. [Color.Unspecified] resolves to [groupContainerColor]. */
    val stickyGroupContainerColor: Color = Color.Unspecified,
    /**
     * Sort icon of a sorted column. [Color.Unspecified] resolves to `primary`, or to
     * [headerContentColor] when `primary` falls under 3:1 contrast with [headerContainerColor].
     */
    val headerSortIconActiveColor: Color = Color.Unspecified,
    /**
     * Keyboard focus ring of a header cell. [Color.Unspecified] resolves to [focusIndicatorColor], or to
     * [headerContentColor] when that falls under 3:1 contrast with [headerContainerColor].
     */
    val headerFocusIndicatorColor: Color = Color.Unspecified,
)

/**
 * Fills every optional color left [Color.Unspecified] from the theme, so a [TableColors] built
 * directly — without [TableDefaults.colors] — still draws themed parts instead of transparent ones.
 */
@Composable
@ReadOnlyComposable
internal fun TableColors.resolve(): TableColors {
    val scheme = MaterialTheme.colorScheme
    val resolvedFocusIndicatorColor = focusIndicatorColor.takeOrElse { scheme.primary }
    return copy(
        rowBlockContainerColor = rowBlockContainerColor.takeOrElse { scheme.surfaceContainerHighest },
        rowSelectedIndicatorColor = rowSelectedIndicatorColor.takeOrElse { scheme.primary },
        dividerColor = dividerColor.takeOrElse { scheme.outlineVariant },
        pinnedDividerColor = pinnedDividerColor.takeOrElse { scheme.outlineVariant },
        borderColor = borderColor.takeOrElse { scheme.outlineVariant },
        focusIndicatorColor = resolvedFocusIndicatorColor,
        hoverColor = hoverColor.takeOrElse { scheme.onSurface },
        groupContentColor = groupContentColor.takeOrElse { scheme.contentColorFor(groupContainerColor) },
        stickyGroupContainerColor = stickyGroupContainerColor.takeOrElse { groupContainerColor },
        headerSortIconActiveColor = headerSortIconActiveColor.takeOrElse { headerAccent(scheme.primary) },
        headerFocusIndicatorColor = headerFocusIndicatorColor.takeOrElse { headerAccent(resolvedFocusIndicatorColor) },
    )
}

/** [accent] when it keeps the WCAG 1.4.11 non-text contrast with the header, else the header content color. */
private fun TableColors.headerAccent(accent: Color): Color =
    if (contrastRatio(accent, headerContainerColor) >= MIN_NON_TEXT_CONTRAST) accent else headerContentColor

/** WCAG 2 contrast ratio, compositing a translucent [background] over white. */
private fun contrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val solidBackground = background.compositeOver(Color.White)
    val fg = foreground.compositeOver(solidBackground).luminance()
    val bg = solidBackground.luminance()
    return (max(fg, bg) + LUMINANCE_OFFSET) / (min(fg, bg) + LUMINANCE_OFFSET)
}

private const val MIN_NON_TEXT_CONTRAST = 3f
private const val LUMINANCE_OFFSET = 0.05f

/** Resolved colors set by the table root; null only for components composed outside a table. */
internal val LocalTableColors: ProvidableCompositionLocal<TableColors?> = staticCompositionLocalOf { null }

/** The resolved colors of the enclosing table, or the themed defaults outside one. */
@Composable
internal fun currentTableColors(): TableColors = LocalTableColors.current ?: TableDefaults.colors().resolve()

/**
 * Resolved at draw rather than defaulted in [TableDefaults.colors] so that a [TableColors] built
 * directly — without the composable factory — still lands on the themed band color instead of
 * painting a transparent band.
 */
@Composable
@ReadOnlyComposable
internal fun resolveRowBlockContainerColor(colors: TableColors): Color =
    colors.rowBlockContainerColor.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHighest }

/** Resolved at draw for the same reason as [resolveRowBlockContainerColor]. */
@Composable
@ReadOnlyComposable
internal fun resolveRowSelectedIndicatorColor(colors: TableColors): Color =
    colors.rowSelectedIndicatorColor.takeOrElse { MaterialTheme.colorScheme.primary }
