package com.example.mobilebrowser.ui.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun DownloadConfirmationDialog(
    filename: String,
    fileExists: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (fileExists) "File Already Exists" else "Download File") },
        text = {
            if (fileExists) {
                Text("\"$filename\" already exists. Do you want to download it again?")
            } else {
                Text("Do you want to download \"$filename\"?")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DownloadCompletionDialog(
    filename: String,
    isMusicFile: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Complete") },
        text = {
            Text(
                text = "\"$filename\" has been downloaded successfully.",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        confirmButton = {
            TextButton(
                onClick = onOpen
            ) {
                Text(if (isMusicFile) "Open Music Library" else "Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}