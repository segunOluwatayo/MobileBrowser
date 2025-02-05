package com.example.mobilebrowser.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DownloadCompletionDialog(
    fileName: String,
    onOpenClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Download Completed") },
        text = { Text("File downloaded successfully: $fileName") },
        confirmButton = {
            TextButton(onClick = {
                onOpenClicked()
                onDismissRequest() // Dismiss after action
            }) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissClicked()
                onDismissRequest() // Dismiss after action
            }) {
                Text("OK") // Or "Dismiss", "Close" - choose the most appropriate
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadCompletionDialog() {
    val showDialog = remember { mutableStateOf(true) }
    if (showDialog.value) {
        DownloadCompletionDialog(
            fileName = "example.pdf",
            onOpenClicked = {
                println("Open Clicked in Preview")
                showDialog.value = false
            },
            onDismissClicked = {
                println("OK Clicked in Preview")
                showDialog.value = false
            },
            onDismissRequest = {
                showDialog.value = false
            }
        )
    }
}