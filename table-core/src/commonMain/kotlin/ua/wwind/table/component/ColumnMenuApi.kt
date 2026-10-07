package ua.wwind.table.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import kotlin.jvm.JvmInline

/** Stable identifier of a column menu item. Built-in items use the values in [ColumnMenuDefaults.Ids]. */
@JvmInline
public value class ColumnMenuItemId(
    /** Raw identifier string. */
    public val value: String,
)

/**
 * One entry of the column header menu.
 *
 * @property id stable identifier, see [ColumnMenuDefaults.Ids]
 * @property label text shown in the menu and used as the accessibility action label
 * @property icon leading icon, e.g. painterResource(TableIcons.Close), or null for none
 * @property enabled whether the item can be activated right now
 * @property disabledReason shown under the label while [enabled] is false
 * @property checked draws a trailing check, e.g. for the active sort direction
 * @property onClick action; the menu closes before it runs
 */
@Immutable
public data class ColumnMenuItem(
    val id: ColumnMenuItemId,
    val label: String,
    val icon: Painter?,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val checked: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * A group of [items] separated from its neighbours by a divider.
 *
 * @property id section identifier; built-in ids are in [ColumnMenuDefaults.Sections]
 * @property items items of the section, in display order
 */
@Immutable
public data class ColumnMenuSection(
    val id: String,
    val items: List<ColumnMenuItem>,
)

/**
 * Shapes the column header menu. [build] receives the default sections for [column] and returns the
 * sections to show: remove, reorder or add items as needed. Returning an empty list removes the menu
 * for that column, together with its accessibility actions and its ⋮ button.
 *
 * The same list feeds the dropdown and the header cell's accessibility custom actions, so the two
 * never disagree.
 *
 * The builder shapes the column-header menu only. The menu of a sticky group header (Sort and
 * Ungroup) always shows its default items and is not customizable.
 *
 * @param C column key type
 */
public fun interface ColumnMenuBuilder<C> {
    /**
     * @param column key of the column the menu belongs to
     * @param defaults default sections for [column]
     * @return the sections to show; empty removes the menu for that column
     */
    @Composable
    public fun build(
        column: C,
        defaults: List<ColumnMenuSection>,
    ): List<ColumnMenuSection>
}

/** Defaults for the column header menu. */
public object ColumnMenuDefaults {
    /** Ids of the built-in items. */
    public object Ids {
        /** Sort ascending. */
        public val SortAscending: ColumnMenuItemId = ColumnMenuItemId("sort-ascending")

        /** Sort descending. */
        public val SortDescending: ColumnMenuItemId = ColumnMenuItemId("sort-descending")

        /** Clear the sort. */
        public val ClearSort: ColumnMenuItemId = ColumnMenuItemId("clear-sort")

        /** Open the column filter. */
        public val OpenFilter: ColumnMenuItemId = ColumnMenuItemId("open-filter")

        /** Clear the column filter. */
        public val ClearFilter: ColumnMenuItemId = ColumnMenuItemId("clear-filter")

        /** Pin the column. */
        public val Pin: ColumnMenuItemId = ColumnMenuItemId("pin")

        /** Unpin the column. */
        public val Unpin: ColumnMenuItemId = ColumnMenuItemId("unpin")

        /** Move the column one step left. */
        public val MoveLeft: ColumnMenuItemId = ColumnMenuItemId("move-left")

        /** Move the column one step right. */
        public val MoveRight: ColumnMenuItemId = ColumnMenuItemId("move-right")

        /** Fit the column width to its content. */
        public val AutoFit: ColumnMenuItemId = ColumnMenuItemId("auto-fit")

        /** Reset the column width. */
        public val ResetWidth: ColumnMenuItemId = ColumnMenuItemId("reset-width")

        /** Group rows by the column. */
        public val GroupBy: ColumnMenuItemId = ColumnMenuItemId("group-by")

        /** Remove grouping. */
        public val Ungroup: ColumnMenuItemId = ColumnMenuItemId("ungroup")

        /** Hide the column. */
        public val Hide: ColumnMenuItemId = ColumnMenuItemId("hide")

        /** Show all hidden columns. */
        public val ShowHidden: ColumnMenuItemId = ColumnMenuItemId("show-hidden")
    }

    /** Ids of the built-in sections, in their default order. */
    @Suppress("ktlint:standard:property-naming")
    public object Sections {
        /** Sort section: sort direction and clear sort. */
        public const val Sort: String = "sort"

        /** Filter section: open and clear the filter. */
        public const val Filter: String = "filter"

        /** Layout section: pin, move and width items. */
        public const val Layout: String = "layout"

        /** Group section: group by and ungroup. */
        public const val Group: String = "group"

        /** Visibility section: hide and show columns. */
        public const val Visibility: String = "visibility"
    }

    private val Identity: ColumnMenuBuilder<Any?> =
        object : ColumnMenuBuilder<Any?> {
            @Composable
            override fun build(
                column: Any?,
                defaults: List<ColumnMenuSection>,
            ): List<ColumnMenuSection> = defaults
        }

    /** A builder that shows the default sections unchanged. */
    @Suppress("UNCHECKED_CAST")
    public fun <C> builder(): ColumnMenuBuilder<C> = Identity as ColumnMenuBuilder<C>
}

/** The table's [ColumnMenuBuilder], provided by `Table`. */
@Suppress("CompositionLocalAllowlist")
internal val LocalColumnMenuBuilder: ProvidableCompositionLocal<ColumnMenuBuilder<Any?>> =
    staticCompositionLocalOf { ColumnMenuDefaults.builder() }

/** Erases the column type so the builder can travel through [LocalColumnMenuBuilder]. */
@Suppress("UNCHECKED_CAST")
internal fun <C> ColumnMenuBuilder<C>.erased(): ColumnMenuBuilder<Any?> = this as ColumnMenuBuilder<Any?>
