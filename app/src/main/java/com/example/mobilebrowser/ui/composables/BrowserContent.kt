package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.R
import com.example.mobilebrowser.browser.GeckoDownloadDelegate
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession

@Composable
fun BrowserContent(
    geckoSession: GeckoSession,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowTabs: () -> Unit,
    onShowHistory: () -> Unit,
    onAddBookmark: (String, String) -> Unit,
    isCurrentUrlBookmarked: Boolean,
    currentPageTitle: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    currentUrl: String,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    tabCount: Int,
    onNewTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowSettings: () -> Unit,
    showDownloadConfirmationDialog: Boolean,
    currentDownloadRequest: GeckoDownloadDelegate.DownloadRequest?,
    onDismissDownloadConfirmationDialog: () -> Unit,
    modifier: Modifier = Modifier,
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val settingsViewModel: com.example.mobilebrowser.ui.viewmodels.SettingsViewModel =
        hiltViewModel()
    val currentSearchEngineUrl by settingsViewModel.searchEngine.collectAsState()
    val searchEngines = listOf(
        SearchEngine("Google", "https://www.google.com/search?q=", R.drawable.google_icon),
        SearchEngine("Bing", "https://www.bing.com/search?q=", R.drawable.bing_icon),
        SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=", R.drawable.duckduckgo_icon),
        SearchEngine("Qwant", "https://www.qwant.com/?q=", R.drawable.qwant_icon),
        SearchEngine(
            "Wikipedia",
            "https://wikipedia.org/wiki/Special:Search?search=",
            R.drawable.wikipedia_icon
        ),
        SearchEngine("eBay", "https://www.ebay.com/sch/i.html?_nkw=", R.drawable.ebay_icon)
    )
    val currentEngine =
        searchEngines.find { it.searchUrl == currentSearchEngineUrl } ?: searchEngines[0]

    // State for Download Completion Dialog and tracking current download
    var showDownloadCompletionDialog by remember { mutableStateOf(false) }
    var currentDownloadId by remember { mutableStateOf<Long?>(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current


    // Back handler for exiting edit mode.  This is where we reset urlText.
    BackHandler(isEditing) {
        isEditing = false
        urlText = currentUrl // Reset to the current URL
        focusManager.clearFocus()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Navigation bar with URL field and buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // No key needed here anymore!
            key("$tabCount") {
                SearchUrlBar(
                    value = urlText,
                    onValueChange = {
                        isEditing = true
                        urlText = it
                    },
                    onSearch = { query, engine ->
                        isEditing = false
                        urlText = currentUrl // Reset after search
                        val searchUrl = engine.searchUrl + query
                        onNavigate(searchUrl)
                        softwareKeyboardController?.hide()
                        focusManager.clearFocus() // Clear focus after search
                    },
                    onNavigate = { url ->
                        isEditing = false
                        urlText = currentUrl // Reset after navigation
                        onNavigate(url)
                        softwareKeyboardController?.hide()
                        focusManager.clearFocus() // Clear focus after navigation
                    },
                    isEditing = isEditing,
                    currentSearchEngine = currentEngine,
                    onStartEditing = { isEditing = true }, // Pass the callback
                    modifier = Modifier
                        .weight(1f, fill = !isEditing) // Expand when not editing
                        .focusRequester(focusRequester) // Attach the focus requester
                )
            }

            if (!isEditing) {
                // Tab button with counter and dropdown menu
                Box {
                    IconButton(onClick = { showTabMenu = true }) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clip(MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTabMenu,
                        onDismissRequest = { showTabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Tabs") },
                            onClick = {
                                showTabMenu = false
                                onShowTabs()
                            },
                            leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("New Tab") },
                            onClick = {
                                showTabMenu = false
                                onNewTab()
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Close All Tabs") },
                            onClick = {
                                showTabMenu = false
                                onCloseAllTabs()
                            },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                        )
                    }
                }

                // Bookmark star button
                if (currentUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (!isCurrentUrlBookmarked) {
                                onAddBookmark(currentUrl, currentPageTitle)
                            }
                        }
                    ) {
                        Icon(
                            if (isCurrentUrlBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isCurrentUrlBookmarked) "Bookmarked" else "Add bookmark",
                            tint = if (isCurrentUrlBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Overflow menu
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    showOverflowMenu = false
                                    onBack()
                                },
                                enabled = canGoBack
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (canGoBack) LocalContentColor.current else LocalContentColor.current.copy(
                                        alpha = 0.38f
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    showOverflowMenu = false
                                    onForward()
                                },
                                enabled = canGoForward
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Forward",
                                    tint = if (canGoForward) LocalContentColor.current else LocalContentColor.current.copy(
                                        alpha = 0.38f
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    showOverflowMenu = false
                                    onReload()
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Menu items
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            onClick = {
                                showOverflowMenu = false
                                onShowBookmarks()
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            onClick = {
                                showOverflowMenu = false
                                onShowDownloads()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                showOverflowMenu = false
                                onShowHistory()
                            },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showOverflowMenu = false
                                onShowSettings() // Invoke the callback to navigate to SettingsScreen.
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                        )
                    }
                }
            }
        }

        // Browser view
        key(geckoSession) {
            GeckoViewComponent(
                geckoSession = geckoSession,
                url = currentUrl,
                onUrlChange = { newUrl ->
                    if (!isEditing) {
                        onNavigate(newUrl)
                    }
                },
                onCanGoBackChange = onCanGoBackChange,
                onCanGoForwardChange = onCanGoForwardChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }

    // Download Confirmation Dialog
    if (showDownloadConfirmationDialog && currentDownloadRequest != null) {
        DownloadConfirmationDialog(
            fileName = currentDownloadRequest.fileName,
            fileSize = currentDownloadRequest.contentLength.toString(),
            onDownloadClicked = {
                Log.d(
                    "BrowserContent",
                    "Download Confirmation: Download button clicked for ${currentDownloadRequest.fileName}"
                )
                onDismissDownloadConfirmationDialog()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val id = downloadViewModel.startDownload(
                            currentDownloadRequest.fileName,
                            currentDownloadRequest.url,
                            currentDownloadRequest.contentType ?: "application/octet-stream",
                            currentDownloadRequest.contentLength
                        )
                        Log.d("BrowserContent", "Download ID received: $id")

                        withContext(Dispatchers.Main) {
                            currentDownloadId = id
                            showDownloadCompletionDialog = true
                            Log.d(
                                "BrowserContent",
                                "Set currentDownloadId=$currentDownloadId, showDownloadCompletionDialog=$showDownloadCompletionDialog"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("BrowserContent", "Error starting download", e)
                    }
                }
            },
            onCancelClicked = {
                Log.d("BrowserContent", "Download Confirmation: Cancel button clicked")
                onDismissDownloadConfirmationDialog()
            },
            onDismissRequest = onDismissDownloadConfirmationDialog
        )
    }
    if (showDownloadCompletionDialog && currentDownloadId != null) {
        Log.d("BrowserContent", "Showing completion dialog for download ID: $currentDownloadId")
        DownloadCompletionDialog(
            downloadId = currentDownloadId!!,
            fileName = currentDownloadRequest?.fileName ?: "unknown",
            viewModel = downloadViewModel,
            onOpenClicked = {
                Log.d("BrowserContent", "Download Completion: Open button clicked")
                showDownloadCompletionDialog = false
                currentDownloadId = null  // Reset the ID when closing
            },
            onDismissClicked = {
                Log.d("BrowserContent", "Download Completion: OK button clicked")
                showDownloadCompletionDialog = false
                currentDownloadId = null  // Reset the ID when closing
            },
            onDismissRequest = {
                showDownloadCompletionDialog = false
                currentDownloadId = null  // Reset the ID when closing
            }
        )
    }
}