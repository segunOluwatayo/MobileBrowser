package com.example.mobilebrowser.ui.homepage

import Shortcut
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
    shortcut: Shortcut,
    onDismiss: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
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
                // Option to toggle pin status (Pin if unpinned, Unpin if pinned).
                TextButton(onClick = onTogglePin) {
                    Text(if (shortcut.isPinned) "Unpin" else "Pin")
                }
                // Option to delete the shortcut from history or remove from homepage.
                TextButton(onClick = onDelete) {
                    Text("Delete from History")
                }
            }
        },
        confirmButton = { /* We don't need a confirm button here */ },
        dismissButton = {
            // Cancel button to close the dialog.
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
