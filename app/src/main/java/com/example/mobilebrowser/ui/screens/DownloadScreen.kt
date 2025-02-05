package com.example.mobilebrowser.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    var showDialog by remember { mutableStateOf<DownloadDialog?>(null) }

    val scope = rememberCoroutineScope()
    // Snack bar state for undo functionality
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(downloads) { download ->
                        DownloadItem(
                            download = download,
                            dateFormat = dateFormat,
                            onRename = { showDialog = DownloadDialog.Rename(download) },
                            onDelete = { showDialog = DownloadDialog.Delete(download) },
                            onShare = {
                                val file = File(download.localPath)
                                if (file.exists()) {
                                    val intent = FileUtils.createShareIntent(
                                        context,
                                        file,
                                        download.mimeType
                                    )
                                    context.startActivity(Intent.createChooser(intent, "Share file"))
                                }
                            },
                            onOpen = {
                                val file = File(download.localPath)
                                if (file.exists()) {
                                    try {
                                        val intent = FileUtils.createOpenIntent(
                                            context,
                                            file,
                                            download.mimeType
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Show error dialog if no app can handle the file
                                        showDialog = DownloadDialog.NoAppFound
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Handle different types of dialogs
            when (val currentDialog = showDialog) {
                is DownloadDialog.Delete -> {
                    AlertDialog(
                        onDismissRequest = { showDialog = null },
                        title = { Text("Delete Download") },
                        text = { Text("Are you sure you want to delete ${currentDialog.download.fileName}?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteDownload(currentDialog.download)
                                    showDialog = null

                                    // Show snackbar with undo option
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Download deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                is DownloadDialog.Rename -> {
                    var newFileName by remember { mutableStateOf(currentDialog.download.fileName) }

                    AlertDialog(
                        onDismissRequest = { showDialog = null },
                        title = { Text("Rename Download") },
                        text = {
                            OutlinedTextField(
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                label = { Text("File Name") }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (viewModel.renameDownload(currentDialog.download, newFileName)) {
                                            showDialog = null
                                        }
                                    }
                                }
                            ) {
                                Text("Rename")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                is DownloadDialog.NoAppFound -> {
                    AlertDialog(
                        onDismissRequest = { showDialog = null },
                        title = { Text("No App Found") },
                        text = { Text("No app installed to open this type of file.") },
                        confirmButton = {
                            TextButton(onClick = { showDialog = null }) {
                                Text("OK")
                            }
                        }
                    )
                }

                null -> { /* No dialog to show */ }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadItem(
    download: DownloadEntity,
    dateFormat: SimpleDateFormat,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val formattedFileSize = remember(download.fileSize) {
        FileUtils.formatFileSize(download.fileSize)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(download.dateAdded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedFileSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            showMenu = false
                            onShare()
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

sealed class DownloadDialog {
    data class Delete(val download: DownloadEntity) : DownloadDialog()
    data class Rename(val download: DownloadEntity) : DownloadDialog()
    object NoAppFound : DownloadDialog()
}