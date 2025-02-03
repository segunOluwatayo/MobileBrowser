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
import com.example.mobilebrowser.ui.viewmodels.HistoryTimeRange
import com.example.mobilebrowser.ui.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyEntries by viewModel.historyEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<HistoryTimeRange?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search history") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // History list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = historyEntries,
                    key = { it.id }
                ) { history ->
                    HistoryItem(
                        title = history.title,
                        url = history.url,
                        date = history.lastVisited,
                        onItemClick = { onNavigateToUrl(history.url) },
                        onDeleteClick = { viewModel.deleteHistoryEntry(history) }
                    )
                }
            }

            if (historyEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank())
                            "No history entries"
                        else
                            "No matching history entries found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Delete dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Clear browsing data") },
                text = {
                    Column {
                        DeleteOption("Last hour", HistoryTimeRange.LAST_HOUR) {
                            showDeleteDialog = false
                            showDeleteConfirmation = it
                        }
                        DeleteOption("Today", HistoryTimeRange.TODAY) {
                            showDeleteDialog = false
                            showDeleteConfirmation = it
                        }
                        DeleteOption("Yesterday", HistoryTimeRange.YESTERDAY) {
                            showDeleteDialog = false
                            showDeleteConfirmation = it
                        }
                        DeleteOption("All time", HistoryTimeRange.ALL) {
                            showDeleteDialog = false
                            showDeleteConfirmation = it
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Confirmation dialog
        showDeleteConfirmation?.let { timeRange ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Confirm Delete") },
                text = {
                    Text(
                        "Are you sure you want to delete your browsing history for " +
                                when (timeRange) {
                                    HistoryTimeRange.LAST_HOUR -> "the last hour"
                                    HistoryTimeRange.TODAY -> "today"
                                    HistoryTimeRange.YESTERDAY -> "yesterday"
                                    HistoryTimeRange.ALL -> "all time"
                                } + "?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHistoryByTimeRange(timeRange)
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItem(
    title: String,
    url: String,
    date: Date,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Close, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun DeleteOption(
    text: String,
    timeRange: HistoryTimeRange,
    onClick: (HistoryTimeRange) -> Unit
) {
    TextButton(
        onClick = { onClick(timeRange) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, modifier = Modifier.fillMaxWidth())
    }
}