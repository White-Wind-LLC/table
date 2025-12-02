package ua.wwind.table

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.data.TableFilterType

/**
 * Declarative specification of a table column.
 *
 * Contains header/cell content, sizing, alignment, sorting and filtering capabilities.
 *
 * @param key unique column key
 * @param header composable content for the header cell; receives table state data
 * @param cell composable content for each row cell
 * @param valueOf function that extracts the comparable value of this column from [T]; used for
 * grouping and other logic
 * @param title optional plain-text title for use in places like active filter chips
 * @param sortable whether the column participates in sorting
 * @param resizable whether user can resize the column
 * @param visible whether column is currently visible
 * @param width preferred width override; null uses table defaults
 * @param minWidth minimal width when resizable
 * @param autoWidth whether to auto-fit width to measured content after first render
 * @param autoMaxWidth optional cap for auto-fitted width
 * @param alignment alignment for cell content
 * @param minRowHeight optional minimal row height when table uses dynamic row height
 * @param maxRowHeight optional maximal row height when table uses dynamic row height
 * @param filter optional filter type provided by this column
 * @param groupHeader optional custom renderer for the group header
 * @param headerDecorations whether to render built-in sort/filter icons in the header cell
 * @param headerClickToSort whether clicking the entire header cell toggles sorting
 * @param footer optional composable content for the footer cell; receives table state data
 */
@Immutable
public data class ColumnSpec<T : Any, C, E>(
    val key: C,
    val header: @Composable (E) -> Unit,
    val cell: @Composable BoxScope.(T) -> Unit,
    val valueOf: (T) -> Any?,
    val title: (@Composable () -> String)? = null,
    val sortable: Boolean = false,
    val resizable: Boolean = true,
    val visible: Boolean = true,
    val width: Dp? = null,
    val minWidth: Dp = 10.dp,
    val autoWidth: Boolean = false,
    val autoMaxWidth: Dp? = null,
    val alignment: Alignment = Alignment.CenterStart,
    val minRowHeight: Dp? = null,
    val maxRowHeight: Dp? = null,
    val filter: TableFilterType<*>? = null,
    val groupHeader: (@Composable BoxScope.(Any?) -> Unit)? = null,
    /**
     * Whether to render default header decorations (sort/filter icons) provided by the table.
     */
    val headerDecorations: Boolean = true,
    /** Whether a click on the whole header cell should toggle sort when sortable. */
    val headerClickToSort: Boolean = true,
    /** Whether this column supports editing. Only applies if table's editingEnabled is true. */
    val editable: Boolean = false,
    /**
     * Optional callback to check if editing can start for a specific item. Returns true to
     * allow, false to deny.
     */
    val canStartEdit: ((T, Int) -> Boolean)? = null,
    /**
     * Composable slot for editing UI. Receives the item, table data, and an onComplete callback to signal
     * edit completion. Table data allows access to shared state (e.g., validation errors, aggregated values).
     */
    val editCell: (@Composable BoxScope.(T, E, onComplete: () -> Unit) -> Unit)? = null,
    /** Optional composable content for the footer cell; receives table state data */
    val footer: (@Composable BoxScope.(E) -> Unit)? = null,
)

/** DSL builder for a list of readonly [ColumnSpec]. */
public class ReadonlyTableColumnsBuilder<T : Any, C, E> internal constructor() {
    private val specs = mutableListOf<ColumnSpec<T, C, E>>()

    /** Declare a column with the given [key] and configure it via [block]. */
    public fun column(
        key: C,
        valueOf: (T) -> Any?,
        block: ReadonlyColumnBuilder<T, C, E>.() -> Unit,
    ) {
        val builder = ReadonlyColumnBuilder<T, C, E>(key, valueOf)
        builder.block()
        specs += builder.build()
    }

    internal fun build(): ImmutableList<ColumnSpec<T, C, E>> = specs.toImmutableList()
}

/** DSL builder for a list of editable [ColumnSpec]. */
public class EditableTableColumnsBuilder<T : Any, C, E> internal constructor() {
    private val specs = mutableListOf<ColumnSpec<T, C, E>>()

    /** Declare a column with the given [key] and configure it via [block]. */
    public fun column(
        key: C,
        valueOf: (T) -> Any?,
        block: EditableColumnBuilder<T, C, E>.() -> Unit,
    ) {
        val builder = EditableColumnBuilder<T, C, E>(key, valueOf)
        builder.block()
        specs += builder.build()
    }

    internal fun build(): ImmutableList<ColumnSpec<T, C, E>> = specs.toImmutableList()
}

/** Builder for a single readonly [ColumnSpec]. */
@Suppress("TooManyFunctions")
public open class ReadonlyColumnBuilder<T : Any, C, E>
    internal constructor(
        protected val key: C,
        protected val valueOf: ((T) -> Any?),
    ) {
        protected var header: (@Composable (E) -> Unit)? = null
        protected var title: (@Composable () -> String)? = null
        protected var cell: (@Composable BoxScope.(T) -> Unit)? = null
        protected var sortable: Boolean = false
        protected var resizable: Boolean = true
        protected var visible: Boolean = true
        protected var width: Dp? = null
        protected var minWidth: Dp = 10.dp
        protected var autoWidth: Boolean = false
        protected var autoMaxWidth: Dp? = null
        protected var alignment: Alignment = Alignment.CenterStart
        protected var minRowHeight: Dp? = null
        protected var maxRowHeight: Dp? = null
        protected var filter: TableFilterType<*>? = null
        protected var headerDecorations: Boolean = true
        protected var headerClickToSort: Boolean = true
        protected var groupHeader: (@Composable BoxScope.(Any?) -> Unit)? = null
        protected var editable: Boolean = false
        protected var canStartEdit: ((T, Int) -> Boolean)? = null
        protected var editCell: (@Composable BoxScope.(T, E, onComplete: () -> Unit) -> Unit)? = null
        protected var footer: (@Composable BoxScope.(E) -> Unit)? = null

        /** Set simple text header. */
        public fun header(text: String) {
            // Ensure single-line headers truncate gracefully and can signal overflow for tooltips
            header = {
                Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            }
            // Default the title to the same text so it can be used for tooltips unless overridden
            if (title == null) {
                title = { text }
            }
        }

        /** Set custom composable header content. */
        public fun header(content: @Composable (E) -> Unit) {
            header = content
        }

        /** Set optional plain-text title (used for chips/tooltips). */
        public fun title(content: @Composable () -> String) {
            title = content
            if (header == null) {
                header = {
                    Text(
                        text = content(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
        }

        /** Define body cell content. */
        public fun cell(content: @Composable BoxScope.(T) -> Unit) {
            cell = content
        }

        /** Mark column as sortable. */
        public fun sortable() {
            sortable = true
        }

        /** Enable/disable user resizing. */
        public fun resizable(value: Boolean) {
            resizable = value
        }

        /** Set visibility for this column. */
        public fun visible(value: Boolean) {
            visible = value
        }

        /** Set minimum and preferred width. */
        public fun width(
            min: Dp,
            pref: Dp? = null,
        ) {
            minWidth = min
            width = pref ?: width
        }

        /**
         * Enable automatic width based on measured content on first render; optionally cap with [max].
         */
        public fun autoWidth(max: Dp? = null) {
            autoWidth = true
            autoMaxWidth = max
        }

        /** Set alignment for cell content. */
        public fun align(alignment: Alignment) {
            this.alignment = alignment
        }

        /**
         * Configure row height bounds that will be considered when the table uses dynamic row height.
         * If the table is in fixed height mode, these values are ignored.
         */
        public fun rowHeight(
            min: Dp? = null,
            max: Dp? = null,
        ) {
            minRowHeight = min
            maxRowHeight = max
        }

        /** Attach a filter type supported by this column. */
        public fun filter(type: TableFilterType<*>) {
            filter = type
        }

        /** Optional custom renderer for the group header. */
        public fun groupHeader(renderer: @Composable BoxScope.(Any?) -> Unit) {
            groupHeader = renderer
        }

        /** Enable or disable default header decorations for this column. */
        public fun headerDecorations(value: Boolean) {
            headerDecorations = value
        }

        /** Enable or disable click-to-sort for the whole header cell. */
        public fun headerClickToSort(value: Boolean) {
            headerClickToSort = value
        }

        /** Define footer cell content. */
        public fun footer(content: @Composable BoxScope.(E) -> Unit) {
            footer = content
        }

        internal fun build(): ColumnSpec<T, C, E> =
            ColumnSpec(
                key = key,
                header = checkNotNull(header) { "Column header must be provided" },
                title = title,
                cell = checkNotNull(cell) { "Column cell must be provided" },
                valueOf = checkNotNull(valueOf) { "Column valueOf must be provided" },
                sortable = sortable,
                resizable = resizable,
                visible = visible,
                width = width,
                minWidth = minWidth,
                autoWidth = autoWidth,
                autoMaxWidth = autoMaxWidth,
                alignment = alignment,
                minRowHeight = minRowHeight,
                maxRowHeight = maxRowHeight,
                filter = filter,
                groupHeader = groupHeader,
                headerDecorations = headerDecorations,
                headerClickToSort = headerClickToSort,
                editable = editable,
                canStartEdit = canStartEdit,
                editCell = editCell,
                footer = footer,
            )
    }

/** Builder for a single editable [ColumnSpec]. Extends [ReadonlyColumnBuilder] with editing capabilities. */
public class EditableColumnBuilder<T : Any, C, E>
    internal constructor(
        key: C,
        valueOf: ((T) -> Any?),
    ) : ReadonlyColumnBuilder<T, C, E>(key, valueOf) {
        /** Define editing UI content. Receives the item, edit state, and an onComplete callback. */
        public fun editCell(
            canEdit: ((T, Int) -> Boolean)? = null,
            content: @Composable BoxScope.(T, E, onComplete: () -> Unit) -> Unit,
        ) {
            this.editable = true
            this.canStartEdit = canEdit
            this.editCell = content
        }
    }

/** DSL entry to declare readonly table columns with custom table data type. */
public fun <T : Any, C, E> tableColumns(build: ReadonlyTableColumnsBuilder<T, C, E>.() -> Unit): ImmutableList<ColumnSpec<T, C, E>> =
    ReadonlyTableColumnsBuilder<T, C, E>().apply(build).build()

/** DSL entry to declare editable table columns. */
public fun <T : Any, C, E> editableTableColumns(
    build: EditableTableColumnsBuilder<T, C, E>.() -> Unit,
): ImmutableList<ColumnSpec<T, C, E>> = EditableTableColumnsBuilder<T, C, E>().apply(build).build()

/** Type alias for readonly table columns (without table data) */
public typealias ReadonlyColumnSpec<T, C> = ColumnSpec<T, C, Unit>
