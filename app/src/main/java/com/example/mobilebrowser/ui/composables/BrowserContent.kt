package com.example.mobilebrowser.ui.composables

import Shortcut
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.ui.homepage.ShortcutEditDialog
import com.example.mobilebrowser.ui.homepage.ShortcutOptionsDialog
import com.example.mobilebrowser.ui.screens.HomeScreen
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
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
    activeTab: TabEntity?,
    tabViewModel: TabViewModel,
    onCloseAllTabs: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowSettings: () -> Unit,
    showDownloadConfirmationDialog: Boolean,
    currentDownloadRequest: GeckoDownloadDelegate.DownloadRequest?,
    onDismissDownloadConfirmationDialog: () -> Unit,
    isHomepageActive: Boolean,
    isOverlayActive: Boolean,
    modifier: Modifier = Modifier,
    onGeckoViewCreated: (android.view.View) -> Unit,
    shortcutViewModel: ShortcutViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
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
        SearchEngine("Wikipedia", "https://wikipedia.org/wiki/Special:Search?search=", R.drawable.wikipedia_icon),
        SearchEngine("eBay", "https://www.ebay.com/sch/i.html?_nkw=", R.drawable.ebay_icon)
    )
    val currentEngine =
        searchEngines.find { it.searchUrl == currentSearchEngineUrl } ?: searchEngines[0]

    var selectedShortcut: ShortcutEntity? by remember { mutableStateOf(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var shortcutToEdit: ShortcutEntity? by remember { mutableStateOf<ShortcutEntity?>(null) }
    val shortcuts by shortcutViewModel.shortcuts.collectAsState()

    // State for Download Completion Dialog and tracking current download.
    var showDownloadCompletionDialog by remember { mutableStateOf(false) }
    var currentDownloadId by remember { mutableStateOf<Long?>(null) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var geckoViewReference by remember { mutableStateOf<android.view.View?>(null) }

    // Back handler: Clear focus if editing.
    BackHandler(isEditing) {
        isEditing = false
        urlText = currentUrl
        focusManager.clearFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Render the HomeScreen as a background layer with a theme-based background.
        if (isHomepageActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                HomeScreen(
                    shortcuts = shortcuts,
                    onShortcutClick = { shortcut ->
                        onNavigate(shortcut.url)
                    },
                    onShortcutLongPressed = { shortcut ->
                        selectedShortcut = shortcut
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Main content (navigation bar and web view or spacer) drawn on top.
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Navigation bar with URL field and buttons.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                key("$tabCount") {
                    SearchUrlBar(
                        value = urlText,
                        currentUrl = currentUrl,
                        onValueChange = {
                            isEditing = true
                            urlText = it
                        },
                        onSearch = { query, engine ->
                            isEditing = false
                            val searchUrl = engine.searchUrl + query
                            onNavigate(searchUrl)
                            softwareKeyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        onNavigate = { url ->
                            isEditing = false
                            onNavigate(url)
                            softwareKeyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        isEditing = isEditing,
                        currentSearchEngine = currentEngine,
                        onStartEditing = { isEditing = true },
                        onEndEditing = {
                            isEditing = false
                            urlText = currentUrl
                        },
                        modifier = Modifier
                            .weight(1f, fill = !isEditing)
                            .focusRequester(focusRequester)
                    )
                }

                if (!isEditing) {
                    // Tab button that directly opens the tabs view.
                    IconButton(onClick = { onShowTabs() }) {
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Overflow menu.
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
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
                                        tint = if (canGoBack) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
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
                                        tint = if (canGoForward) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                                    )
                                }
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
                                    onShowSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Add a clickable overlay only when editing URL (to dismiss keyboard when tapped elsewhere)
            if (isEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            isEditing = false
                            urlText = currentUrl
                            focusManager.clearFocus()
                        }
                ) {}
            }
            else {
                // Keep layout structure by always adding a Box with weight
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // The key fix: Only add GeckoView to the composition when needed
                    if (!isHomepageActive) {
                        key(geckoSession, currentUrl) {
                            // Only add GeckoView when not on homepage
                            GeckoViewComponent(
                                geckoSession = geckoSession,
                                url = currentUrl,
                                onUrlChange = { newUrl ->
                                    val normalizedUrl = if (newUrl == "about:blank") "" else newUrl
                                    if (!isEditing && normalizedUrl != currentUrl) {
                                        onNavigate(normalizedUrl)
                                    }
                                },
                                onCanGoBackChange = onCanGoBackChange,
                                onCanGoForwardChange = onCanGoForwardChange,
                                onViewCreated = { view ->
                                    Log.d("BrowserContent", "GeckoView created, passing reference up")
                                    geckoViewReference = view
                                    // Pass the view up to the caller (MainActivity)
                                    onGeckoViewCreated(view)
                                },
                                onScrollStopped = { view ->
                                    // Use the active tab ID (if available) to update its thumbnail.
                                    activeTab?.id?.let { tabId ->
                                        // Here we call our existing updateTabThumbnail method,
                                        // which uses capturePixels() internally.
                                        tabViewModel.updateTabThumbnail(tabId, view)
                                    }
                                },
                                // When overlays are active, make GeckoView invisible
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isOverlayActive) {
                                            Modifier.alpha(0f) // Invisible when overlay active
                                        } else {
                                            Modifier.alpha(1f) // Fully visible
                                        }
                                    )
                            )
                        }
                    }
                    // No else branch needed - HomeScreen already rendered in background
                }
            }
        }

        selectedShortcut?.let { shortcut ->
            val shortcutEntity = ShortcutEntity(
                label = shortcut.label,
                url = shortcut.url,
                iconRes = shortcut.iconRes,
                isPinned = shortcut.isPinned
            )

            ShortcutOptionsDialog(
                shortcut = shortcut,
                onDismiss = { selectedShortcut = null },
                onOpenInNewTab = {
                    onNewTab()
                    onNavigate(shortcut.url)
                    selectedShortcut = null
                },
                onEdit = {
                    shortcutToEdit = shortcut
                    showEditDialog = true
                    selectedShortcut = null
                },
                onTogglePin = {
                    shortcutViewModel.togglePin(shortcut)
                    selectedShortcut = null
                },
                onDelete = {
                    shortcutViewModel.deleteShortcut(shortcut)
                    selectedShortcut = null
                }
            )
        }

        if (showEditDialog && shortcutToEdit != null) {
            ShortcutEditDialog(
                shortcut = shortcutToEdit!!,
                onDismiss = {
                    showEditDialog = false
                    shortcutToEdit = null
                },
                onSave = { label, url ->
                    shortcutViewModel.updateShortcut(
                        shortcutToEdit!!,
                        newLabel = label,
                        newUrl = url
                    )
                    showEditDialog = false
                    shortcutToEdit = null
                }
            )
        }
    }

    // Download Confirmation Dialog.
    if (showDownloadConfirmationDialog && currentDownloadRequest != null) {
        DownloadConfirmationDialog(
            fileName = currentDownloadRequest.fileName,
            fileSize = currentDownloadRequest.contentLength.toString(),
            onDownloadClicked = {
                Log.d("BrowserContent", "Download Confirmation: Download button clicked for ${currentDownloadRequest.fileName}")
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
                            Log.d("BrowserContent", "Set currentDownloadId=$currentDownloadId, showDownloadCompletionDialog=$showDownloadCompletionDialog")
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
                currentDownloadId = null
            },
            onDismissClicked = {
                Log.d("BrowserContent", "Download Completion: OK button clicked")
                showDownloadCompletionDialog = false
                currentDownloadId = null
            },
            onDismissRequest = {
                showDownloadCompletionDialog = false
                currentDownloadId = null
            }
        )
    }
}
