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

    // Animate the screen’s appearance
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally() + fadeIn(),
        exit = slideOutHorizontally() + fadeOut()
    ) {
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            // **New Tab Count Box**
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                                    .clip(MaterialTheme.shapes.small)
                                    .size(28.dp), // Adjust size
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }

                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (tabs.isEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Box(contentAlignment = Alignment.Center) {
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
                }
            } else {
                LazyColumn(
                    state = lazyListState,
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
                            isDragging = (tab.id == draggingTabId),
                            onTabClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleTabSelection(tab.id)
                                } else {
                                    onTabSelected(tab.id)
                                    onNavigateBack()
                                }
                            },
                            onCloseTab = { viewModel.closeTab(tab) },
                            onStartDrag = {
                                if (!isSelectionMode) {
                                    draggingTabId = tab.id
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
}
