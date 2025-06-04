package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.util.FileUtils
import kotlinx.coroutines.delay
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
    // Observe whether the download is completed
    val isCompleted by viewModel.shouldShowCompletionDialog(downloadId)
        .collectAsState(initial = false)

    var visible by remember { mutableStateOf(isCompleted) }

    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            visible = true
            // Wait 5 seconds before auto dismissing
            delay(5000)
            visible = false
            onDismissRequest()
        }
    }

    // Place the banner in a full screen Box and align it to the bottom
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Completed",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "File downloaded successfully: $fileName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                                        Log.e("DownloadBanner", "No app found to open file", e)
                                    }
                                } else {
                                    Log.e("DownloadBanner", "File does not exist: ${file.absolutePath}")
                                }
                            }
                        }
                    }) {
                        Text("Open")
                    }
                    TextButton(onClick = {
                        visible = false
                        onDismissClicked()
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

