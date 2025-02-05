package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.mobilebrowser.util.FileUtils

@Composable
fun DownloadConfirmationDialog(
    fileName: String,
    fileSize: String,
    onDownloadClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val formattedFileName = remember(fileName) {
        FileUtils.getFormattedFileName(fileName)
    }

    val formattedFileSize = remember(fileSize) {
        FileUtils.formatFileSize(fileSize.toLongOrNull() ?: 0)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Download File") },
        text = {
            Column {
                Text("Filename: $formattedFileName")
                Text("Size: $formattedFileSize")
            }
        },
        confirmButton = {
            TextButton(onClick = onDownloadClicked) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClicked) {
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