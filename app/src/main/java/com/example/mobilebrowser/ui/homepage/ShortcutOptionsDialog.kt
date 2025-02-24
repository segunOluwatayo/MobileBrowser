package com.example.mobilebrowser.ui.homepage

import Shortcut
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.mobilebrowser.data.entity.ShortcutEntity

/**
 * ShortcutOptionsDialog displays a context menu with options for the given shortcut.
 *
 * @param shortcut The shortcut for which the options are shown.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onOpenInNewTab Called when "Open in New Tab" is selected.
 * @param onEdit Called when "Edit" is selected.
 * @param onTogglePin Called when "Pin/Unpin" is selected.
 * @param onDelete Called when "Delete from History" or "Remove from Homepage" is selected.
 */
@Composable
fun ShortcutOptionsDialog(
    shortcut: ShortcutEntity,
    onDismiss: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: (ShortcutEntity) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Shortcut Options") },
        text = {
            Column {
                // Option to open the shortcut in a new tab.
                TextButton(onClick = onOpenInNewTab) {
                    Text("Open in New Tab")
                }
                // Option to edit the shortcut details.
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                // Option to toggle pin status
                TextButton(onClick = { onTogglePin(shortcut) }) {
                    Text(if (shortcut.isPinned) "Unpin" else "Pin")
                }
                // Option to delete
                TextButton(onClick = onDelete) {
                    Text("Delete from History")
                }
            }
        },
        confirmButton = { /* no confirm needed */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

