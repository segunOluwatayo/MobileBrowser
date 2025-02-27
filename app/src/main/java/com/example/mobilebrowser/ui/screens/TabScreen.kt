package com.example.mobilebrowser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.ui.composables.TabListItem
import com.example.mobilebrowser.ui.composables.TabListItemNewTabCard
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    onNavigateBack: () -> Unit,
    onTabSelected: (Long) -> Unit,
    viewModel: TabViewModel = hiltViewModel(),
    bookmarkViewModel: BookmarkViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsState()
    val isSelectionMode by viewModel.isSelectionModeActive.collectAsState()
    val selectedTabs by viewModel.selectedTabs.collectAsState()
    val tabCount by viewModel.tabCount.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.debugThumbnails()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedTabs.size} selected")
                    } else {
                        Text("Tabs")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Selection mode actions
                        IconButton(onClick = { viewModel.closeSelectedTabs() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Close selected")
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        // Normal mode actions
                        if (tabs.isNotEmpty()) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Close All Tabs") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.closeAllTabs()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close All Tabs"
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select Tabs") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleSelectionMode()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.SelectAll,
                                            contentDescription = "Select Tabs"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tabs.isEmpty()) {
            // Empty state - show prompt to create a new tab
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
                    Icon(
                        imageVector = Icons.Default.Tab,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "No tabs open",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
            // Grid of tabs with Chrome-style cards
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Existing tabs
                items(
                    items = tabs,
                    key = { it.id }
                ) { tab ->
                    TabListItem(
                        tab = tab,
                        isSelected = selectedTabs.contains(tab.id),
                        isSelectionMode = isSelectionMode,
                        isDragging = false, // No dragging in grid mode
                        onTabClick = {
                            if (isSelectionMode) {
                                viewModel.toggleTabSelection(tab.id)
                            } else {
                                onTabSelected(tab.id)
                                onNavigateBack()
                            }
                        },
                        onCloseTab = { viewModel.closeTab(tab) },
                        onBookmarkTab = { bookmarkViewModel.quickAddBookmark(tab.url, tab.title) }
                    )
                }

                // New Tab card at the end
                item {
                    TabListItemNewTabCard(
                        onClick = {
                            scope.launch {
                                val newTabId = viewModel.createTab(url = "", title = "New Tab")
                                onTabSelected(newTabId)
                                onNavigateBack()
                            }
                        }
                    )
                }
            }
        }
    }
}