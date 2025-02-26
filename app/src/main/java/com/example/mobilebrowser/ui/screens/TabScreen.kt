package com.example.mobilebrowser.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.ui.composables.TabListItem
import com.example.mobilebrowser.ui.util.rememberDragDropState
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
    var draggingTabId by remember { mutableStateOf<Long?>(null) }

    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(
        lazyListState = lazyListState,
        onMove = { fromIndex, toIndex ->
            viewModel.moveTab(fromIndex, toIndex)
        }
    )

    LaunchedEffect(Unit) {
        viewModel.debugThumbnails()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            "${selectedTabs.size} selected",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    } else {
                        Column {
                            Text(
                                "Tabs",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "$tabCount tabs open",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        // New Tab Button
                        FilledTonalIconButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    val newTabId = viewModel.createTab()
                                    onTabSelected(newTabId)
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Tab")
                        }

                        // More Options Menu
                        Box {
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
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (tabs.isEmpty()) {
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
                    FilledTonalButton(
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
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = tabs,
                    key = { it.id }
                ) { tab ->
                    TabListItem(
                        tab = tab,
                        isSelected = selectedTabs.contains(tab.id),
                        isSelectionMode = isSelectionMode,
                        isDragging = (tab.id == draggingTabId),
                        onStartDrag = { draggingTabId = tab.id },
                        onTabClick = {
                            if (isSelectionMode) {
                                viewModel.toggleTabSelection(tab.id)
                            } else {
                                onTabSelected(tab.id)
                                onNavigateBack()
                            }
                        },
                        onCloseTab = { viewModel.closeTab(tab) },
                        onNewTab = {
                            viewModel.viewModelScope.launch {
                                val newTabId = viewModel.createTab()
                                onTabSelected(newTabId)
                                onNavigateBack()
                            }
                        },
                        onBookmarkTab = {
                            bookmarkViewModel.quickAddBookmark(tab.url, tab.title)
                        }
                    )
                }
            }
        }
    }
}