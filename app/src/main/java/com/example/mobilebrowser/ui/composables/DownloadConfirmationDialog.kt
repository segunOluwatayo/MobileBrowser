package com.example.mobilebrowser.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DownloadConfirmationDialog(
    fileName: String,
    fileSize: String? = null,
    onDownloadClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Confirm Download") },
        text = {
            val fileSizeText = fileSize?.let { "File size: $it\n" } ?: ""
            Text("$fileSizeText File name: $fileName")
        },
        confirmButton = {
            TextButton(onClick = {
                onDownloadClicked()
                onDismissRequest() // Dismiss after action
            }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onCancelClicked()
                onDismissRequest() // Dismiss after action
            }) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDownloadConfirmationDialog() {
    val showDialog = remember { mutableStateOf(true) }
    if (showDialog.value) {
        DownloadConfirmationDialog(
            fileName = "example.pdf",
            fileSize = "2.5 MB",
            onDownloadClicked = {
                println("Download Clicked in Preview")
                showDialog.value = false
            },
            onCancelClicked = {
                println("Cancel Clicked in Preview")
                showDialog.value = false
            },
            onDismissRequest = {
                showDialog.value = false
            }
        )
    }
}