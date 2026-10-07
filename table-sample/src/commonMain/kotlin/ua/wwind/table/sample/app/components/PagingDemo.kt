package ua.wwind.table.sample.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ua.wwind.paging.core.DataPortion
import ua.wwind.paging.core.Pager
import ua.wwind.table.paging.Table
import ua.wwind.table.sample.data.createDemoData
import ua.wwind.table.sample.model.Person
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.tableColumns

private const val LOAD_LATENCY_MS = 800L
private const val DEMO_ROWS = 1_000
private const val LOAD_SIZE = 40
private const val PRELOAD_SIZE = 80
private const val CACHE_SIZE = 200

/** Demo people served page by page, with switches that make every load fail or the list empty. */
internal class DemoPagingSource(
    private val people: List<Person>,
) {
    var failLoads: Boolean = false
    var empty: Boolean = false

    fun read(
        position: Int,
        size: Int,
    ): Flow<DataPortion<Person>> =
        flow {
            delay(LOAD_LATENCY_MS)
            check(!failLoads) { "Simulated load failure at $position" }
            val rows = if (empty) emptyList() else people
            val end = (position + size).coerceAtMost(rows.size)
            val values = (position until end).associateWith { rows[it] }.toPersistentMap()
            emit(DataPortion(totalSize = rows.size, values = values))
        }
}

private fun pagingDemoColumns() =
    tableColumns<Person, String, Unit> {
        column("id", valueOf = { it.id }) {
            header("#")
            width(64.dp, 64.dp)
            cell { person, _ -> Text(person.id.toString()) }
        }
        column("name", valueOf = { it.name }) {
            header("Name")
            width(180.dp, 180.dp)
            cell { person, _ -> Text(person.name) }
        }
        column("city", valueOf = { it.city }) {
            header("City")
            width(140.dp, 140.dp)
            cell { person, _ -> Text(person.city) }
        }
        column("salary", valueOf = { it.salary }) {
            header("Salary")
            width(120.dp, 120.dp)
            cell { person, _ -> Text(person.salary.toString()) }
        }
    }

/** A paged table over a slow source, with controls that reach every load state. */
@Composable
fun PagingDemo(modifier: Modifier = Modifier) {
    val source =
        remember {
            val base = createDemoData()
            DemoPagingSource(List(DEMO_ROWS) { index -> base[index % base.size].copy(id = index + 1) })
        }
    val pager =
        remember {
            Pager(
                loadSize = LOAD_SIZE,
                preloadSize = PRELOAD_SIZE,
                cacheSize = CACHE_SIZE,
                readData = source::read,
            )
        }
    val items by pager.flow.collectAsState(initial = null)
    var failLoads by remember { mutableStateOf(false) }
    var empty by remember { mutableStateOf(false) }
    val columns = remember { pagingDemoColumns() }
    val state = rememberTableState(columns = persistentListOf("id", "name", "city", "salary"))

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = failLoads,
                onClick = {
                    failLoads = !failLoads
                    source.failLoads = failLoads
                },
                label = { Text("Fail loads") },
            )
            FilterChip(
                selected = empty,
                onClick = {
                    empty = !empty
                    source.empty = empty
                    pager.refresh()
                },
                label = { Text("Empty dataset") },
            )
            Button(onClick = pager::refresh) { Text("Refresh") }
        }
        Table(
            items = items,
            state = state,
            columns = columns,
            modifier = Modifier.fillMaxSize().padding(12.dp),
        )
    }
}
