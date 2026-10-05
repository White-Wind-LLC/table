// core/component/ColumnMenuApi.kt
package ua.wwind.table.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.jvm.JvmInline

/** Stable identifier of a column menu item. Built-in items use the values in [ColumnMenuDefaults.Ids]. */
@JvmInline
public value class ColumnMenuItemId(
    public val value: String,
)

/**
 * One entry of the column header menu.
 *
 * @property label text shown in the menu and used as the accessibility action label
 * @property icon leading icon, or null for none
 * @property enabled whether the item can be activated right now
 * @property disabledReason shown under the label while [enabled] is false
 * @property checked draws a trailing check, e.g. for the active sort direction
 * @property onClick action; the menu closes before it runs
 */
@Immutable
public data class ColumnMenuItem(
    val id: ColumnMenuItemId,
    val label: String,
    val icon: ImageVector?,
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val checked: Boolean = false,
    val onClick: () -> Unit,
)

/** A group of [items] separated from its neighbours by a divider. Built-in ids are in [ColumnMenuDefaults.Sections]. */
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
 */
public fun interface ColumnMenuBuilder<C> {
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
        public val SortAscending: ColumnMenuItemId = ColumnMenuItemId("sort-ascending")
        public val SortDescending: ColumnMenuItemId = ColumnMenuItemId("sort-descending")
        public val ClearSort: ColumnMenuItemId = ColumnMenuItemId("clear-sort")
        public val OpenFilter: ColumnMenuItemId = ColumnMenuItemId("open-filter")
        public val ClearFilter: ColumnMenuItemId = ColumnMenuItemId("clear-filter")
        public val Pin: ColumnMenuItemId = ColumnMenuItemId("pin")
        public val Unpin: ColumnMenuItemId = ColumnMenuItemId("unpin")
        public val MoveLeft: ColumnMenuItemId = ColumnMenuItemId("move-left")
        public val MoveRight: ColumnMenuItemId = ColumnMenuItemId("move-right")
        public val AutoFit: ColumnMenuItemId = ColumnMenuItemId("auto-fit")
        public val ResetWidth: ColumnMenuItemId = ColumnMenuItemId("reset-width")
        public val GroupBy: ColumnMenuItemId = ColumnMenuItemId("group-by")
        public val Ungroup: ColumnMenuItemId = ColumnMenuItemId("ungroup")
        public val Hide: ColumnMenuItemId = ColumnMenuItemId("hide")
        public val ShowHidden: ColumnMenuItemId = ColumnMenuItemId("show-hidden")
    }

    /** Ids of the built-in sections, in their default order. */
    public object Sections {
        public const val Sort: String = "sort"
        public const val Filter: String = "filter"
        public const val Layout: String = "layout"
        public const val Group: String = "group"
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
