package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.data.entity.BookmarkEntity
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkEditScreen(
    bookmarkId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: BookmarkViewModel = hiltViewModel()
) {
    // Mutable states for title, URL, and tags.
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    // Load existing bookmark data if editing an existing bookmark
    LaunchedEffect(bookmarkId) {
        if (bookmarkId != null) {
            viewModel.getBookmarkById(bookmarkId)?.let { bookmark ->
                title = bookmark.title
                url = bookmark.url
                tags = bookmark.tags ?: ""
            }
        }
    }

    // Scaffold layout for the screen, including a top app bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (bookmarkId == null) "Add Bookmark" else "Edit Bookmark") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Main content column with input fields and a save button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TextField for editing the title of the bookmark
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // TextField for editing the URL of the bookmark
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )

            // TextField for editing tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Button for saving the bookmark
            Button(
                onClick = {
                    // Create a BookmarkEntity using the current input values.
                    val bookmark = BookmarkEntity(
                        id = bookmarkId ?: 0,
                        title = title,
                        url = url,
                        tags = tags.takeIf { it.isNotBlank() },
                        favicon = null,
                        lastVisited = Date(),
                        dateAdded = if (bookmarkId == null) Date() else Date()
                    )

                    // Add or update the bookmark via the ViewModel.
                    if (bookmarkId == null) {
                        viewModel.addBookmark(bookmark)
                    } else {
                        viewModel.updateBookmark(bookmark)
                    }

                    // Navigate back after saving the bookmark.
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text(if (bookmarkId == null) "Add Bookmark" else "Save Changes")
            }
        }
    }
}
