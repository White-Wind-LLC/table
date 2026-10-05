// coreTest/component/header/ColumnMenuModelTest.kt
package ua.wwind.table.component.header

import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.ColumnMenuDefaults.Ids
import ua.wwind.table.component.ColumnMenuDefaults.Sections
import ua.wwind.table.component.ColumnMenuItemId
import ua.wwind.table.config.PinnedSide
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.state.ColumnWidthAction
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.UiString
import ua.wwind.table.tableColumns
import kotlin.test.Test

class ColumnMenuModelTest {
    private val specs =
        tableColumns<String, String, Unit> {
            column("full", valueOf = { it }) {
                header("Full")
                sortable()
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item) }
            }
            column("plain", valueOf = { it }) {
                header("Plain")
                resizable(false)
                cell { item, _ -> Text(item) }
            }
            column("last", valueOf = { it }) {
                header("Last")
                cell { item, _ -> Text(item) }
            }
        }

    private fun spec(key: String): ColumnSpec<*, String, *> = specs.first { it.key == key }

    private fun stateWith(settings: TableSettings = TableSettings()) =
        TableState(
            initialColumns = listOf("full", "plain", "last"),
            initialSort = null,
            initialOrder = listOf("full", "plain", "last"),
            initialWidths = emptyMap(),
            settings = settings,
            dimensions = TableDefaults.standardDimensions(),
        )

    private fun model(
        state: TableState<String>,
        key: String,
        context: ColumnMenuContext = ColumnMenuContext.Header,
    ) = columnMenuModel(spec(key), state, context, onOpenFilter = {})

    private fun List<ColumnMenuEntrySection>.ids(): List<ColumnMenuItemId> = flatMap { s -> s.entries.map { it.id } }

    private fun List<ColumnMenuEntrySection>.entry(id: ColumnMenuItemId): ColumnMenuEntry =
        flatMap { it.entries }.first { it.id == id }

    @Test
    fun `a capable column gets every section in order`() {
        val sections = model(stateWith(), "full")
        assertThat(sections.map { it.id })
            .containsExactly(Sections.Sort, Sections.Filter, Sections.Layout, Sections.Group, Sections.Visibility)
        assertThat(sections.ids()).containsExactly(
            Ids.SortAscending,
            Ids.SortDescending,
            Ids.OpenFilter,
            Ids.Pin,
            Ids.MoveLeft,
            Ids.MoveRight,
            Ids.AutoFit,
            Ids.ResetWidth,
            Ids.GroupBy,
            Ids.Hide,
        )
    }

    @Test
    fun `items a column can never do are left out`() {
        val ids = model(stateWith(), "plain").ids()
        assertThat(ids).doesNotContain(Ids.SortAscending)
        assertThat(ids).doesNotContain(Ids.OpenFilter)
        assertThat(ids).doesNotContain(Ids.AutoFit)
        assertThat(ids).doesNotContain(Ids.ResetWidth)
    }

    @Test
    fun `clear sort, clear filter, unpin, ungroup and show hidden appear with their state`() {
        val state = stateWith(TableSettings(pinnedColumnsCount = 1))
        state.setSort("full", SortOrder.DESCENDING)
        state.setFilter("full", TableFilterState(constraint = null, values = listOf("x")))
        state.groupBy("full")
        state.columns.hide("last")

        val sections = model(state, "full")
        assertThat(sections.entry(Ids.SortDescending).checked).isTrue()
        assertThat(sections.ids()).containsAll(Ids.ClearSort, Ids.ClearFilter, Ids.Unpin, Ids.Ungroup, Ids.ShowHidden)
        assertThat(sections.entry(Ids.ShowHidden).count).isEqualTo(1)
    }

    @Test
    fun `sort and group are disabled with a reason under the row reorder lock`() {
        val sections = model(stateWith(TableSettings(rowReorderEnabled = true)), "full")
        assertThat(sections.entry(Ids.SortAscending).enabled).isFalse()
        assertThat(sections.entry(Ids.SortAscending).disabledReason).isEqualTo(UiString.ColumnMenuReasonRowReorder)
        assertThat(sections.entry(Ids.GroupBy).disabledReason).isEqualTo(UiString.ColumnMenuReasonRowReorder)
        assertThat(sections.entry(Ids.OpenFilter).enabled).isTrue()
    }

    @Test
    fun `group by is disabled while row blocks are shown`() {
        val state = stateWith()
        state.rowBlocksNonEmpty = true
        assertThat(model(state, "full").entry(Ids.GroupBy).disabledReason).isEqualTo(UiString.ColumnMenuReasonRowBlocks)
    }

    @Test
    fun `move items explain the edge they hit`() {
        val state = stateWith(TableSettings(pinnedColumnsCount = 1))
        assertThat(model(state, "full").entry(Ids.MoveLeft).disabledReason).isEqualTo(UiString.ColumnMenuReasonFirst)
        assertThat(
            model(state, "full").entry(Ids.MoveRight).disabledReason,
        ).isEqualTo(UiString.ColumnMenuReasonPinnedEdge)
        assertThat(model(state, "last").entry(Ids.MoveRight).disabledReason).isEqualTo(UiString.ColumnMenuReasonLast)
        assertThat(model(state, "plain").entry(Ids.MoveRight).disabledReason).isNull()
    }

    @Test
    fun `width items explain why they are disabled`() {
        val state = stateWith()
        val sections = model(state, "full")
        assertThat(sections.entry(Ids.AutoFit).disabledReason).isEqualTo(UiString.ColumnMenuReasonNothingToFit)
        assertThat(sections.entry(Ids.ResetWidth).disabledReason).isEqualTo(UiString.ColumnMenuReasonDefaultWidth)

        state.columns.resize("full", ColumnWidthAction.Set(200.dp))
        state.columns.updateMaxContentWidth("full", 120.dp, source = "Row[0]")
        val after = model(state, "full")
        assertThat(after.entry(Ids.AutoFit).enabled).isTrue()
        assertThat(after.entry(Ids.ResetWidth).enabled).isTrue()
    }

    @Test
    fun `pin explains why the last unpinned column cannot be pinned`() {
        val state = stateWith(TableSettings(pinnedColumnsCount = 2))
        assertThat(model(state, "last").entry(Ids.Pin).disabledReason).isEqualTo(UiString.ColumnMenuReasonLastUnpinned)
    }

    @Test
    fun `pin label follows the pinned side`() {
        val state = stateWith(TableSettings(pinnedColumnsSide = PinnedSide.Right))
        assertThat(model(state, "full").entry(Ids.Pin).label).isEqualTo(UiString.ColumnMenuPinRight)
    }

    @Test
    fun `hide explains why the last visible column stays`() {
        val state = stateWith()
        state.columns.hide("plain")
        state.columns.hide("last")
        assertThat(model(state, "full").entry(Ids.Hide).disabledReason).isEqualTo(UiString.ColumnMenuReasonLastVisible)
    }

    @Test
    fun `the group header gets only sort and group`() {
        val state = stateWith()
        state.groupBy("full")
        val sections = model(state, "full", ColumnMenuContext.GroupHeader)
        assertThat(sections.map { it.id }).containsExactly(Sections.Sort, Sections.Group)
    }

    @Test
    fun `menu actions drive the state`() {
        val state = stateWith()
        model(state, "full").entry(Ids.SortDescending).onClick()
        assertThat(state.sort).isEqualTo(SortState("full", SortOrder.DESCENDING))
        model(state, "full").entry(Ids.MoveRight).onClick()
        assertThat(state.columns.order.toList()).containsExactly("plain", "full", "last")
        model(state, "full").entry(Ids.Hide).onClick()
        assertThat(state.columns.hidden.toList()).containsExactly("full")
    }
}
