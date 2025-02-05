package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel

@Composable
fun DownloadCompletionDialog(
    downloadId: Long,
    fileName: String,
    viewModel: DownloadViewModel,
    onOpenClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val isCompleted by viewModel.shouldShowCompletionDialog(downloadId)
        .collectAsState(initial = false)

    Log.d("DownloadCompletionDialog", "Dialog state: downloadId=$downloadId, isCompleted=$isCompleted")

    if (isCompleted) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Download Completed") },
            text = { Text("File downloaded successfully: $fileName") },
            confirmButton = {
                TextButton(onClick = {
                    onOpenClicked()
                }) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismissClicked()
                }) {
                    Text("OK")
                }
            }
        )
    }
}