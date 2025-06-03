package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val todayEntries by viewModel.todayHistory.collectAsState()
    val lastWeekEntries by viewModel.lastWeekHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<HistoryTimeRange?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "History",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "${todayEntries.size + lastWeekEntries.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Modern search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search history") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { }

            // History list with modern sections
            if (searchQuery.isBlank()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Today section
                    if (todayEntries.isNotEmpty()) {
                        item {
                            ModernHistorySectionHeader(title = "Today")
                        }
                        items(
                            items = todayEntries,
                            key = { it.id }
                        ) { history ->
                            ModernHistoryItem(
                                title = history.title,
                                url = history.url,
                                date = history.lastVisited,
                                favicon = history.favicon,
                                onItemClick = { onNavigateToUrl(history.url) },
                                onDeleteClick = { viewModel.deleteHistoryEntry(history) }
                            )
                        }
                    }

                    // Last 7 days section
                    if (lastWeekEntries.isNotEmpty()) {
                        item {
                            ModernHistorySectionHeader(title = "Last 7 days")
                        }
                        items(
                            items = lastWeekEntries,
                            key = { it.id }
                        ) { history ->
                            ModernHistoryItem(
                                title = history.title,
                                url = history.url,
                                date = history.lastVisited,
                                favicon = history.favicon,
                                onItemClick = { onNavigateToUrl(history.url) },
                                onDeleteClick = { viewModel.deleteHistoryEntry(history) }
                            )
                        }
                    }

                    // Empty state with modern design
                    if (todayEntries.isEmpty() && lastWeekEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(bottom = 16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "No browsing history yet",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Your browsing history will appear here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    FilledTonalButton(
                                        onClick = { viewModel.updateSearchQuery(" ") },
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {
                                        Text("Show all history")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Modern search results view
                val searchResults by viewModel.searchResults.collectAsState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(searchResults) { history ->
                        ModernHistoryItem(
                            title = history.title,
                            url = history.url,
                            date = history.lastVisited,
                            favicon = history.favicon,
                            onItemClick = { onNavigateToUrl(history.url) },
                            onDeleteClick = { viewModel.deleteHistoryEntry(history) }
                        )
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            EmptySearchResult()
                        }
                    }
                }
            }
        }

        // Modern delete dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        "Clear browsing data",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        ModernDeleteOption("Last hour", HistoryTimeRange.LAST_HOUR) {
                            showDeleteDialog = false
//                            showDeleteConfirmation = it
                            viewModel.deleteHistoryByTimeRange(HistoryTimeRange.LAST_HOUR)
                        }
                        ModernDeleteOption("Today", HistoryTimeRange.TODAY) {
                            showDeleteDialog = false
//                            showDeleteConfirmation = it
                            viewModel.deleteHistoryByTimeRange(HistoryTimeRange.TODAY)
                        }
                        ModernDeleteOption("Yesterday", HistoryTimeRange.YESTERDAY) {
                            showDeleteDialog = false
//                            showDeleteConfirmation = it

                        }
                        ModernDeleteOption("All time", HistoryTimeRange.ALL) {
                            showDeleteDialog = false
//                            showDeleteConfirmation = it
                            viewModel.deleteHistoryByTimeRange(HistoryTimeRange.ALL)
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
    }
}

@Composable
private fun ModernHistorySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernHistoryItem(
    title: String,
    url: String,
    date: Date,
    favicon: String?,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val domain = remember(url) {
        try {
            val uri = java.net.URI(url)
            uri.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    Surface(
        onClick = onItemClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern favicon display
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(favicon ?: "https://www.google.com/s2/favicons?domain=$domain&sz=64")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
//                    fallback = {
//                        Text(
//                            text = domain.take(1).uppercase(),
//                            style = MaterialTheme.typography.titleMedium,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title.takeIf { it.isNotBlank() && it != "Loading..." } ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernDeleteOption(
    text: String,
    timeRange: HistoryTimeRange,
    onClick: (HistoryTimeRange) -> Unit
) {
    Surface(
        onClick = { onClick(timeRange) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun EmptySearchResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No results found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}