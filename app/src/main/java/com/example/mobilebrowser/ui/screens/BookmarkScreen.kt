package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.data.entity.BookmarkEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    onNavigateToEdit: (Long) -> Unit, // Callback for navigating to the edit bookmark screen.
    onNavigateBack: () -> Unit,      // Callback for navigating back to the previous screen.
    viewModel: BookmarkViewModel = hiltViewModel() // ViewModel instance injected via Hilt.
) {
    // Observing the list of bookmarks and the search query as state.
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Scaffolding for the UI, with a top bar and content area.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") }, // Title displayed in the top bar.
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back") // Back button.
                    }
                }
            )
        }
    ) { padding ->
        // Main content column.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar for filtering bookmarks.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search bookmarks") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // LazyColumn for displaying a scrollable list of bookmarks.
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dynamically renders each bookmark as a BookmarkItem.
                items(bookmarks) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onEditClick = { onNavigateToEdit(bookmark.id) },
                        onDeleteClick = { viewModel.deleteBookmark(bookmark) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: BookmarkEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // State to show or hide the delete confirmation dialog.
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Card layout for the bookmark item.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Row for displaying the bookmark details and action buttons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Display the bookmark's title.
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Display the bookmark's URL.
                    Text(
                        text = bookmark.url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // If tags exist, display them as chips.
                    bookmark.tags?.let { tags ->
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.split(",").forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag.trim()) }
                                )
                            }
                        }
                    }
                }

                // Edit and delete buttons.
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    // AlertDialog for confirming deletion of a bookmark.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Bookmark") },
            text = { Text("Are you sure you want to delete this bookmark?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
