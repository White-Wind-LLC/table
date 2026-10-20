package ua.wwind.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.strings.StringProvider

/** Where a filter chip is drawn. */
public enum class TableFilterChipPlacement {
    /** A top-level chip in the active filters row. */
    Row,

    /** A line inside a group's popover. */
    Tree,
}

/**
 * What a chip of the active filters row or of a group's popover shows: one node of the filter tree,
 * passed to [TableFilterChipStyle.chip] and [DefaultFilterChip].
 */
@Stable
public sealed interface TableFilterChipNode<C> {
    /** Where the chip is drawn. */
    public val placement: TableFilterChipPlacement

    /** The table's string provider, for the chip's own labels. */
    public val strings: StringProvider

    /** Removes the node; a top-level condition is reset instead, as before the filter tree. */
    public val onClear: () -> Unit

    /**
     * A condition on a column the table knows.
     *
     * @property column key of the column the filter belongs to
     * @property state the condition's current filter state
     * @property title the column's plain-text title, or its key when it has none
     * @property text the filter described for the chip, e.g. `contains "x"`; null while its panel is open
     *   and the filter no longer describes a value
     * @property isOpen whether the condition's filter panel is open
     * @property onOpen opens the condition's filter panel; does nothing where there are no panels
     * @property onChange replaces the condition's filter state, e.g. from a value picked on the chip itself
     */
    public class Condition<C> internal constructor(
        public val column: C,
        public val state: TableFilterState<*>,
        public val title: String,
        public val text: String?,
        public val isOpen: Boolean,
        override val placement: TableFilterChipPlacement,
        override val strings: StringProvider,
        public val onOpen: () -> Unit,
        public val onChange: (TableFilterState<*>) -> Unit,
        override val onClear: () -> Unit,
        internal val content: TableFilterChipContent<C>?,
    ) : TableFilterChipNode<C> {
        /** The default chip's label: `"$title: $text"`, or only the title while there is no text. */
        public val label: String get() = if (text != null) "$title: $text" else title
    }

    /**
     * An AND/OR group, or a NOT.
     *
     * @property operator the operator joining the group's children
     * @property negated whether the group is negated
     * @property itemCount the number of active children
     * @property columnTitles the distinct titles of the columns under the group, in tree order
     * @property sharedColumn the column of a non-negated OR group whose children are all conditions on it
     * @property text the chip's text: `"Status: Active or Pending"` for a shared column, else the column titles,
     *   e.g. `"Type, Qty, Vip"`
     * @property description the whole group read as a sentence, for accessibility
     * @property isOpen whether the group's popover is open
     * @property onOpen opens the group's popover
     */
    public class Group<C> internal constructor(
        public val operator: TableFilterNode.Operator,
        public val negated: Boolean,
        public val itemCount: Int,
        public val columnTitles: List<String>,
        public val sharedColumn: C?,
        public val text: String,
        public val description: String,
        public val isOpen: Boolean,
        public val onOpen: () -> Unit,
        override val placement: TableFilterChipPlacement,
        override val strings: StringProvider,
        override val onClear: () -> Unit,
    ) : TableFilterChipNode<C>

    /**
     * A condition on a column the table does not know; it can only be removed.
     *
     * @property column the condition's column key
     * @property state the condition's filter state
     * @property title [TableFilterChipStyle.unknownTitle] of the key, or the key itself
     */
    public class Unknown<C> internal constructor(
        public val column: C,
        public val state: TableFilterState<*>,
        public val title: String,
        override val placement: TableFilterChipPlacement,
        override val strings: StringProvider,
        override val onClear: () -> Unit,
    ) : TableFilterChipNode<C>
}

/**
 * A column's chip content, drawn inside the library's chip in the row and in the popover.
 *
 * @property label the chip's label, in place of [TableFilterChipNode.Condition.label]
 * @property leadingIcon optional icon before the label; give it a `contentDescription` when it carries meaning
 */
@Immutable
public class TableFilterChipContent<C>(
    public val label: @Composable (TableFilterChipNode.Condition<C>) -> Unit,
    public val leadingIcon: (@Composable (TableFilterChipNode.Condition<C>) -> Unit)? = null,
)
