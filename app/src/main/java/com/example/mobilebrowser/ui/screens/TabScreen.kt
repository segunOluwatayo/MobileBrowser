package com.example.mobilebrowser.ui.screens

import androidx.compose.animation.*
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
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
import com.example.mobilebrowser.data.entity.TabEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    onNavigateBack: () -> Unit,
    onTabSelected: (Long) -> Unit,
    viewModel: TabViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val isSelectionMode by viewModel.isSelectionModeActive.collectAsState()
    val selectedTabs by viewModel.selectedTabs.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectionMode) "${selectedTabs.size} selected" else "Tabs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Selection mode actions
                        IconButton(onClick = { viewModel.closeSelectedTabs() }) {
                            Icon(Icons.Default.Delete, "Close selected tabs")
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, "Exit selection mode")
                        }
                    } else {
                        // Normal mode actions
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Tab") },
                                onClick = {
                                    showMenu = false
                                    viewModel.viewModelScope.launch {
                                        val newTabId = viewModel.createTab()
                                        onTabSelected(newTabId)
                                        onNavigateBack()
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, "New tab")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Close All Tabs") },
                                onClick = {
                                    showMenu = false
                                    viewModel.closeAllTabs()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, "Close all tabs")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select Tabs") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleSelectionMode()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, "Select tabs")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tabs.isEmpty()) {
            // Show empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("No tabs open")
                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                val newTabId = viewModel.createTab()
                                onTabSelected(newTabId)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "New tab")
                        Spacer(Modifier.width(8.dp))
                        Text("New Tab")
                    }
                }
            }
        } else {
            // Show tab list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(
                    items = tabs,
                    key = { it.id }
                ) { tab ->
                    TabListItem(
                        tab = tab,
                        isSelected = selectedTabs.contains(tab.id),
                        isSelectionMode = isSelectionMode,
                        onTabClick = {
                            if (isSelectionMode) {
                                viewModel.toggleTabSelection(tab.id)
                            } else {
                                onTabSelected(tab.id)
                                onNavigateBack()
                            }
                        },
                        onCloseTab = { viewModel.closeTab(tab) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabListItem(
    tab: TabEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTabClick: () -> Unit,
    onCloseTab: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onTabClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onTabClick() }
                    )
                }
                Column(
                    modifier = Modifier.padding(start = if (isSelectionMode) 8.dp else 0.dp)
                ) {
                    Text(
                        text = tab.title.ifEmpty { "New Tab" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = tab.url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Close Tab") },
                        onClick = {
                            showMenu = false
                            onCloseTab()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Close, "Close tab")
                        }
                    )
                }
            }
        }
    }
}