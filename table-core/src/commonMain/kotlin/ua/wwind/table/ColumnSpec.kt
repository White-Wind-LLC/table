package ua.wwind.table

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.data.TableFilterType

/**
 * Declarative specification of a table column.
 *
 * Contains header/cell content, sizing, alignment, sorting and filtering capabilities.
 *
 * @param key unique column key
 * @param header composable content for the header cell
 * @param cell composable content for each row cell
 * @param valueOf function that extracts the comparable value of this column from [T]; used for grouping and other logic
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
 */
public data class ColumnSpec<T : Any, C>(
    val key: C,
    val header: @Composable () -> Unit,
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
    /** Whether to render default header decorations (sort/filter icons) provided by the table. */
    val headerDecorations: Boolean = true,
    /** Whether a click on the whole header cell should toggle sort when sortable. */
    val headerClickToSort: Boolean = true,
)

/**
 * DSL builder for a list of [ColumnSpec].
 */
public class TableColumnsBuilder<T : Any, C> internal constructor() {
    private val specs = mutableListOf<ColumnSpec<T, C>>()

    /**
     * Declare a column with the given [key] and configure it via [block].
     */
    public fun column(
        key: C,
        valueOf: (T) -> Any?,
        block: ColumnBuilder<T, C>.() -> Unit,
    ) {
        val builder = ColumnBuilder<T, C>(key, valueOf)
        builder.block()
        specs += builder.build()
    }

    internal fun build(): List<ColumnSpec<T, C>> = specs.toList()
}

@Suppress("TooManyFunctions")
/**
 * Builder for a single [ColumnSpec].
 */
public class ColumnBuilder<T : Any, C> internal constructor(
    private val key: C,
    private val valueOf: ((T) -> Any?),
) {
    private var header: (@Composable () -> Unit)? = null
    private var title: (@Composable () -> String)? = null
    private var cell: (@Composable BoxScope.(T) -> Unit)? = null
    private var sortable: Boolean = false
    private var resizable: Boolean = true
    private var visible: Boolean = true
    private var width: Dp? = null
    private var minWidth: Dp = 10.dp
    private var autoWidth: Boolean = false
    private var autoMaxWidth: Dp? = null
    private var alignment: Alignment = Alignment.CenterStart
    private var minRowHeight: Dp? = null
    private var maxRowHeight: Dp? = null
    private var filter: TableFilterType<*>? = null
    private var headerDecorations: Boolean = true
    private var headerClickToSort: Boolean = true
    private var groupHeader: (@Composable BoxScope.(Any?) -> Unit)? = null

    /** Set simple text header. */
    public fun header(text: String) {
        // Ensure single-line headers truncate gracefully and can signal overflow for tooltips
        header = { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        // Default the title to the same text so it can be used for tooltips unless overridden
        if (title == null) {
            title = { text }
        }
    }

    /** Set custom composable header content. */
    public fun header(content: @Composable () -> Unit) {
        header = content
    }

    /** Set optional plain-text title (used for chips/tooltips). */
    public fun title(content: @Composable () -> String) {
        title = content
        if (header == null) {
            header = { Text(text = content(), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
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

    /** Enable automatic width based on measured content on first render; optionally cap with [max]. */
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

    internal fun build(): ColumnSpec<T, C> =
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
        )
}

/**
 * DSL entry to declare table columns.
 */
public fun <T : Any, C> tableColumns(build: TableColumnsBuilder<T, C>.() -> Unit): List<ColumnSpec<T, C>> =
    TableColumnsBuilder<T, C>().apply(build).build()
