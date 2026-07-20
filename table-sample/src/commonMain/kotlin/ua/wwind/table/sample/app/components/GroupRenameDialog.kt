package ua.wwind.table.sample.app.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Renames a row group. The group id doubles as its name in this sample, so the dialog edits the id
 * itself and every row carrying it is rewritten.
 *
 * Because the id IS the identity, names must stay unique: two groups sharing one id are one logical
 * group that renames together yet only merges visually when adjacent. The dialog is the gate that
 * keeps them apart.
 *
 * @param groupId Id of the group being renamed; null keeps the dialog closed.
 * @param takenGroupIds Ids already in use by OTHER groups - [groupId] itself must be excluded by the
 *   caller, or the dialog would reject the name the group already has as taken.
 * @param onRename Callback with the new id, invoked only for a non-blank, unused change.
 * @param onDismissRequest Callback to close the dialog.
 */
@Composable
fun GroupRenameDialog(
    groupId: String?,
    takenGroupIds: Set<String>,
    onRename: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (groupId == null) return

    // Keyed on the group so reopening the dialog - or switching to another group without closing -
    // refills the field instead of showing the previous target's id.
    var newGroupId by remember(groupId) { mutableStateOf(groupId) }
    // Trimmed is what gets saved, so it is also what gets validated: a name is judged exactly as it
    // would be stored. Comparison is case-sensitive to match block identity, which is decided
    // with `==` over `blockOf` values - "alice" really is a free name next to "Alice".
    val trimmed = newGroupId.trim()
    val errorMessage =
        when {
            trimmed.isEmpty() -> "Name cannot be empty"
            trimmed in takenGroupIds -> "Name is already in use"
            else -> null
        }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Rename group") },
        text = {
            OutlinedTextField(
                value = newGroupId,
                onValueChange = { newGroupId = it },
                label = { Text("Group name") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = errorMessage?.let { message -> { Text(message) } },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRename(trimmed)
                    onDismissRequest()
                },
                enabled = errorMessage == null && trimmed != groupId,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
