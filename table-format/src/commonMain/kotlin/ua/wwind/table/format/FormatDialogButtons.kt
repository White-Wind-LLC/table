package ua.wwind.table.format

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.painterResource
import ua.wwind.table.format.component.FormatDialogState
import ua.wwind.table.format.data.EditFormatRule
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.icon.TableIcons
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

/**
 * The FAB that adds a rule while the list shows; while a rule is edited, a "More actions" menu (Duplicate,
 * Delete) on the start side and Cancel / Save text buttons on the end side.
 */
@Composable
@Suppress("LongMethod")
internal fun <E : Enum<E>, FILTER> FormatDialogButtons(
    state: FormatDialogState<E, FILTER>,
    rules: ImmutableList<TableFormatRule<E, FILTER>>,
    onRulesChange: (ImmutableList<TableFormatRule<E, FILTER>>) -> Unit,
    getNewRule: (id: Long) -> TableFormatRule<E, FILTER>,
    strings: StringProvider,
) {
    val edit = state.editItem
    val nextId = (rules.maxByOrNull { it.id }?.id?.inc() ?: 0L)
    if (edit == null) {
        FloatingActionButton(
            onClick = {
                state.editItem = EditFormatRule(rules.lastIndex + 1, getNewRule(nextId), true)
            },
            shape = CircleShape,
        ) {
            Icon(
                painter = painterResource(TableIcons.Add),
                contentDescription = strings.get(UiString.FormatAddRule),
            )
        }
    } else {
        val index = edit.index
        val item = edit.item
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = spacedBy(8.dp),
        ) {
            if (!edit.isNew) {
                var confirmDelete by remember(index) { mutableStateOf(false) }
                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        text = { Text(strings.get(UiString.FormatDeleteRuleTitle)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    confirmDelete = false
                                    onRulesChange(
                                        rules.toPersistentList().mutate { list ->
                                            if (index in list.indices) list.removeAt(index)
                                        },
                                    )
                                    state.editItem = null
                                },
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                            ) {
                                Text(strings.get(UiString.FormatDeleteRuleConfirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) {
                                Text(strings.get(UiString.FormatDeleteRuleCancel))
                            }
                        },
                    )
                }
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            painter = painterResource(TableIcons.MoreVert),
                            contentDescription = strings.get(UiString.FormatRuleMoreActions),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        // The copy opens as a new, unsaved rule; the original keeps its saved state.
                        DropdownMenuItem(
                            text = { Text(strings.get(UiString.FormatRuleDuplicate)) },
                            leadingIcon = {
                                Icon(painter = painterResource(TableIcons.ContentCopy), contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                state.editItem =
                                    EditFormatRule(
                                        rules.lastIndex + 1,
                                        item.copy(id = nextId),
                                        isNew = true,
                                        isCopy = true,
                                    )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(strings.get(UiString.FormatRuleDelete)) },
                            leadingIcon = {
                                Icon(painter = painterResource(TableIcons.Delete), contentDescription = null)
                            },
                            colors =
                                MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error,
                                ),
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { state.editItem = null }) {
                Text(strings.get(UiString.FormatRuleCancel))
            }
            TextButton(
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
                    if (edit.isCopy) state.itemCopyIndex = index
                },
            ) {
                Text(strings.get(UiString.FormatRuleSave))
            }
        }
    }
}
