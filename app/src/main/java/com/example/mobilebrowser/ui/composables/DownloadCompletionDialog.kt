package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DownloadCompletionDialog(
    downloadId: Long,
    fileName: String,
    viewModel: DownloadViewModel,
    onOpenClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isCompleted by viewModel.shouldShowCompletionDialog(downloadId)
        .collectAsState(initial = false)

    if (isCompleted) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Download Completed") },
            text = { Text("File downloaded successfully: $fileName") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.getDownloadById(downloadId)?.let { download ->
                            val file = File(download.localPath)
                            if (file.exists()) {
                                try {
                                    val intent = FileUtils.createOpenIntent(
                                        context,
                                        file,
                                        download.mimeType
                                    )
                                    context.startActivity(intent)
                                    onOpenClicked()
                                } catch (e: Exception) {
                                    // Handle no app found case if needed
                                    Log.e("DownloadCompletionDialog", "No app found to open file", e)
                                }
                            }
                        }
                    }
                }) {
                    Text("Open")
                    Log.d("DownloadCompletionDialog", "Context: ${context.javaClass.name}")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClicked) {
                    Text("OK")
                }
            }
        )
    }
}