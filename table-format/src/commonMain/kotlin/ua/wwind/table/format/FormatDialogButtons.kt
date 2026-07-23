package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.TableFormatRule

@Composable
@Suppress("LongMethod")
internal fun <E : Enum<E>, FILTER> FormatDialogButtons(
    state: FormatDialogState<E, FILTER>,
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
) {
    val edit = state.editItem
    if (edit == null) {
        FloatingActionButton(
            onClick = {
                val id = rules.maxByOrNull { it.id }?.id?.inc() ?: 0L
                state.editItem =
                    EditFormatRule(
                        rules.lastIndex + 1,
                        getNewRule(id),
                        true,
                    )
            },
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add",
            )
        }
    } else {
        val (index, item, isNew) = edit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = spacedBy(16.dp, Alignment.End),
        ) {
            if (!isNew) {
                IconButton(
                    onClick = {
                        onRulesChange(
                            rules.toPersistentList().mutate { list ->
                                if (index in list.indices) list.removeAt(index)
                            },
                        )
                        state.editItem = null
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(
                    onClick = {
                        val id = rules.maxByOrNull { it.id }?.id?.inc() ?: 0L
                        val lastIndex = rules.lastIndex
                        val itemCopy = item.copy(id = id)
                        onRulesChange(
                            rules.toPersistentList().mutate { list ->
                                list.add(itemCopy)
                            },
                        )
                        state.editItem = null
                        state.itemCopyIndex = lastIndex.inc()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                    )
                }
            }
            IconButton(
                onClick = {
                    state.editItem = null
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    onRulesChange(
                        rules.toPersistentList().mutate { list ->
                            if (index in list.indices) {
                                list[index] = item
                            } else {
                                list.add(item)
                            }
                        },
                    )
                    state.editItem = null
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = "Save",
                )
            }
        }
    }
}
