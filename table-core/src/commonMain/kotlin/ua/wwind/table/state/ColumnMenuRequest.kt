package ua.wwind.table.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpOffset
import ua.wwind.table.component.header.ColumnMenuContext

/**
 * An open column menu: which [column], in which [context], where. A null [offset] anchors the menu
 * under the header cell (keyboard and ⋮ button); a pointer sets it to the press position.
 *
 * [anchor] is the identity of the header box that opened the menu, so one request opens one menu
 * even when several boxes share a column (group header rows). Null means "the header box for this
 * column", which is what keyboard requests use.
 */
@Immutable
internal data class ColumnMenuRequest<C>(
    val column: C,
    val context: ColumnMenuContext,
    val offset: DpOffset?,
    val fromKeyboard: Boolean,
    val anchor: Any? = null,
)
