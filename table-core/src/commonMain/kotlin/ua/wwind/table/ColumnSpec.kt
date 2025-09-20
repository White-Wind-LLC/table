package ua.wwind.table

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.wwind.table.filter.data.TableFilterType

public data class ColumnSpec<T : Any, C>(
    val key: C,
    val header: @Composable () -> Unit,
    val cell: @Composable BoxScope.(T) -> Unit,
    val title: (@Composable () -> String)? = null,
    val sortable: Boolean = false,
    val resizable: Boolean = true,
    val visible: Boolean = true,
    val width: Dp? = null,
    val minWidth: Dp = 100.dp,
    val alignment: Alignment.Horizontal = Alignment.Start,
    val filter: TableFilterType<*>? = null,
    /** Whether to render default header decorations (sort/filter icons) provided by the table. */
    val headerDecorations: Boolean = true,
    /** Whether a click on the whole header cell should toggle sort when sortable. */
    val headerClickToSort: Boolean = true,
)

public class TableColumnsBuilder<T : Any, C> internal constructor() {
    private val specs = mutableListOf<ColumnSpec<T, C>>()

    public fun column(
        key: C,
        block: ColumnBuilder<T, C>.() -> Unit,
    ) {
        val builder = ColumnBuilder<T, C>(key)
        builder.block()
        specs += builder.build()
    }

    internal fun build(): List<ColumnSpec<T, C>> = specs.toList()
}

@Suppress("TooManyFunctions")
public class ColumnBuilder<T : Any, C> internal constructor(
    private val key: C,
) {
    private var header: (@Composable () -> Unit)? = null
    private var title: (@Composable () -> String)? = null
    private var cell: (@Composable BoxScope.(T) -> Unit)? = null
    private var sortable: Boolean = false
    private var resizable: Boolean = true
    private var visible: Boolean = true
    private var width: Dp? = null
    private var minWidth: Dp = 100.dp
    private var alignment: Alignment.Horizontal = Alignment.Start
    private var filter: TableFilterType<*>? = null
    private var headerDecorations: Boolean = true
    private var headerClickToSort: Boolean = true

    public fun header(text: String) {
        header = { Text(text) }
    }

    public fun header(content: @Composable () -> Unit) {
        header = content
    }

    public fun title(content: @Composable () -> String) {
        title = content
    }

    public fun cell(content: @Composable BoxScope.(T) -> Unit) {
        cell = content
    }

    public fun sortable() {
        sortable = true
    }

    public fun resizable(value: Boolean) {
        resizable = value
    }

    public fun visible(value: Boolean) {
        visible = value
    }

    public fun width(
        min: Dp,
        pref: Dp? = null,
    ) {
        minWidth = min
        width = pref ?: width
    }

    public fun align(horizontal: Alignment.Horizontal) {
        alignment = horizontal
    }

    public fun filter(type: TableFilterType<*>) {
        filter = type
    }

    /** Enable or disable default sort/filter icons rendering for this header cell. */
    public fun headerDecorations(value: Boolean) {
        headerDecorations = value
    }

    /** Enable or disable click-to-sort on the whole header cell for this column. */
    public fun headerClickToSort(value: Boolean) {
        headerClickToSort = value
    }

    internal fun build(): ColumnSpec<T, C> =
        ColumnSpec(
            key = key,
            header = checkNotNull(header) { "Column header must be provided" },
            title = title,
            cell = checkNotNull(cell) { "Column cell must be provided" },
            sortable = sortable,
            resizable = resizable,
            visible = visible,
            width = width,
            minWidth = minWidth,
            alignment = alignment,
            filter = filter,
            headerDecorations = headerDecorations,
            headerClickToSort = headerClickToSort,
        )
}

public fun <T : Any, C> tableColumns(build: TableColumnsBuilder<T, C>.() -> Unit): List<ColumnSpec<T, C>> =
    TableColumnsBuilder<T, C>().apply(build).build()
