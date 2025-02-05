package com.example.mobilebrowser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.ui.composables.DownloadCompletionDialog
import com.example.mobilebrowser.ui.viewmodels.DownloadState
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val recentlyDeletedDownload by viewModel.recentlyDeletedDownload.collectAsState()
    var showRenameDialog by remember { mutableStateOf<DownloadEntity?>(null) }

    // Display the download completion dialog if needed
    if (downloadState is DownloadState.Completed) {
        DownloadCompletionDialog(
            state = downloadState as DownloadState.Completed,
            onDismiss = { viewModel.setIdle() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            // Removed parameter here to match the expected lambda type
            recentlyDeletedDownload?.let { download ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.undoDelete() }) {
                            Text("UNDO")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Deleted ${download.filename}")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                // Empty state
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
                // List of downloads
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = downloads,
                        key = { it.id }
                    ) { download ->
                        DownloadItem(
                            download = download,
                            onOpenClick = { viewModel.openDownloadedFile(download.id) },
                            onShareClick = {
                                // Handle share action (e.g., viewModel.shareDownload(download.id))
                            },
                            onRenameClick = { showRenameDialog = download },
                            onDeleteClick = { viewModel.deleteDownload(download.id) }
                        )
                    }
                }
            }
        }

        // Rename dialog
        showRenameDialog?.let { download ->
            RenameDialog(
                currentName = download.filename,
                onDismiss = { showRenameDialog = null },
                onRename = { newName ->
                    viewModel.renameDownload(download.id, newName)
                    showRenameDialog = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadItem(
    download: DownloadEntity,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenClick() }, // Open the file when clicked
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.filename,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(download.downloadDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Three-dot overflow menu
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
                            onShareClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Download") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
