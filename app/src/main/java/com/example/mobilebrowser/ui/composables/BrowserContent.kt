package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.R
import com.example.mobilebrowser.browser.GeckoDownloadDelegate.DownloadRequest
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.ui.homepage.ShortcutEditDialog
import com.example.mobilebrowser.ui.homepage.ShortcutOptionsDialog
import com.example.mobilebrowser.ui.screens.HomeScreen
import com.example.mobilebrowser.ui.viewmodels.*
import kotlinx.coroutines.*
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
    currentDownloadRequest: DownloadRequest?,
    onDismissDownloadConfirmationDialog: () -> Unit,
    isHomepageActive: Boolean,
    isOverlayActive: Boolean,
    modifier: Modifier = Modifier,
    onGeckoViewCreated: (android.view.View) -> Unit,
    shortcutViewModel: ShortcutViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Define default search engines.
    val defaultSearchEngines = listOf(
        SearchEngine("Google", "https://www.google.com/search?q=", R.drawable.google_icon),
        SearchEngine("Bing", "https://www.bing.com/search?q=", R.drawable.bing_icon),
        SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=", R.drawable.duckduckgo_icon),
        SearchEngine("Qwant", "https://www.qwant.com/?q=", R.drawable.qwant_icon),
        SearchEngine("Wikipedia", "https://wikipedia.org/wiki/Special:Search?search=", R.drawable.wikipedia_icon),
        SearchEngine("eBay", "https://www.ebay.com/sch/i.html?_nkw=", R.drawable.ebay_icon)
    )

    // Retrieve custom search engines from SettingsViewModel.
    val customEngines by settingsViewModel.customSearchEngines.collectAsState()

    // Merge default and custom search engines; for custom engines we use a generic icon.
    val mergedSearchEngines = (defaultSearchEngines + customEngines.map { custom ->
        SearchEngine(custom.name, custom.searchUrl, R.drawable.generic_searchengine)
    }).sortedBy { it.name }

    // Retrieve the current search engine URL.
    val currentEngineUrl by settingsViewModel.searchEngine.collectAsState()
    // Determine current search engine.
    val currentEngine = mergedSearchEngines.find { it.searchUrl == currentEngineUrl }
        ?: mergedSearchEngines[0]

    var selectedShortcut: ShortcutEntity? by remember { mutableStateOf(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var shortcutToEdit: ShortcutEntity? by remember { mutableStateOf<ShortcutEntity?>(null) }
    val shortcuts by shortcutViewModel.shortcuts.collectAsState()

    val homepageEnabled by settingsViewModel.homepageEnabled.collectAsState()
    val recentTabEnabled by settingsViewModel.recentTabEnabled.collectAsState()
    val bookmarksEnabled by settingsViewModel.bookmarksEnabled.collectAsState()
    val historyEnabled by settingsViewModel.historyEnabled.collectAsState()
    val recentHistory by historyViewModel.recentHistory.collectAsState(initial = emptyList())

    var showDownloadCompletionDialog by remember { mutableStateOf(false) }
    var currentDownloadId by remember { mutableStateOf<Long?>(null) }
    val focusRequester = remember { FocusRequester() }
    var geckoViewReference by remember { mutableStateOf<android.view.View?>(null) }
    val addressBarLocation by settingsViewModel.addressBarLocation.collectAsState(initial = "TOP")
    val isAddressBarAtTop = (addressBarLocation == "TOP")

    // If user presses back while editing, end editing mode.
    BackHandler(isEditing) {
        isEditing = false
        urlText = currentUrl
        focusManager.clearFocus()
    }

    // Whenever URL changes or homepage is toggled, update displayed text if not editing.
    LaunchedEffect(currentUrl, isHomepageActive) {
        if (!isEditing) {
            urlText = if (isHomepageActive) "" else currentUrl
        }
    }

    // On first load, if homepage is active, show empty URL (unless user is already editing).
    LaunchedEffect(Unit) {
        if (isHomepageActive && !isEditing) {
            urlText = ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // If homepage is active, show the home screen in the background.
        if (isHomepageActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                HomeScreen(
                    shortcuts = shortcuts,
                    onShortcutClick = { shortcut -> onNavigate(shortcut.url) },
                    onShortcutLongPressed = { shortcut -> selectedShortcut = shortcut },
                    onShowAllTabs = { onShowTabs() },
                    onRecentTabClick = { activeTab -> onNavigate(activeTab.url) },
                    onRestoreDefaultShortcuts = { shortcutViewModel.restoreDefaultShortcuts() },
                    recentTab = activeTab,
                    onShowBookmarks = { onShowBookmarks() },
                    recentHistory = recentHistory,
                    onRecentHistoryClick = { historyEntry -> onNavigate(historyEntry.url) },
                    onShowAllHistory = { onShowHistory() },
                    showShortcuts = homepageEnabled,
                    showRecentTab = recentTabEnabled,
                    showBookmarks = bookmarksEnabled,
                    showHistory = historyEnabled,
                    isAddressBarAtTop = isAddressBarAtTop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // The main Column that includes the address bar(s) and GeckoView
        Column(modifier = Modifier.fillMaxSize()) {
            if (isAddressBarAtTop) {
                // Top address bar
                AddressBarSection(
                    urlText = urlText,
                    currentUrl = currentUrl,
                    isEditing = isEditing,
                    onUrlTextChange = {
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
                    currentSearchEngine = currentEngine,
                    availableSearchEngines = mergedSearchEngines,
                    onStartEditing = { isEditing = true },
                    onEndEditing = {
                        isEditing = false
                        urlText = currentUrl
                    },
                    tabCount = tabCount,
                    onShowTabs = onShowTabs,
                    showOverflowMenu = showOverflowMenu,
                    onShowOverflowMenu = { showOverflowMenu = true },
                    onDismissOverflowMenu = { showOverflowMenu = false },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onBack = onBack,
                    onForward = onForward,
                    onReload = onReload,
                    onAddBookmark = {
                        if (!isCurrentUrlBookmarked) {
                            onAddBookmark(currentUrl, currentPageTitle)
                        }
                    },
                    isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                    onShowBookmarks = onShowBookmarks,
                    onShowHistory = onShowHistory,
                    onShowDownloads = onShowDownloads,
                    onShowSettings = onShowSettings,
                    focusRequester = focusRequester
                )

                // Divider after top bar
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }

            // Main browsing area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!isHomepageActive) {
                    key(geckoSession, currentUrl) {
                        GeckoViewComponent(
                            geckoSession = geckoSession,
                            url = currentUrl,
                            onUrlChange = { newUrl ->
                                val normalizedUrl = if (newUrl == "about:blank") "" else newUrl
                                // Only update if not editing
                                if (!isEditing && normalizedUrl != currentUrl) {
                                    onNavigate(normalizedUrl)
                                }
                            },
                            onCanGoBackChange = onCanGoBackChange,
                            onCanGoForwardChange = onCanGoForwardChange,
                            onViewCreated = { view ->
                                Log.d("BrowserContent", "GeckoView created, passing reference up")
                                geckoViewReference = view
                                onGeckoViewCreated(view)
                            },
                            onScrollStopped = { view ->
                                activeTab?.id?.let { tabId ->
                                    tabViewModel.viewModelScope.launch {
                                        delay(300)
                                        if (activeTab?.id == tabId) {
                                            tabViewModel.updateTabThumbnail(tabId, view)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                // Show or hide the WebView based on overlay
                                .then(if (isOverlayActive) Modifier.alpha(0f) else Modifier.alpha(1f))
                        )
                    }
                }

                // <-- Here's the transparent overlay when editing -->
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    // When the user taps outside, end editing
                                    focusManager.clearFocus()
                                    isEditing = false
                                    urlText = currentUrl
                                }
                            }
                    )
                }
            }

            // If the address bar is at the bottom, show it after the main content
            if (!isAddressBarAtTop) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                AddressBarSection(
                    urlText = urlText,
                    currentUrl = currentUrl,
                    isEditing = isEditing,
                    onUrlTextChange = {
                        isEditing = true
                        urlText = it
                    },
                    onSearch = { query, engine ->
                        isEditing = false
                        val searchUrl = engine.searchUrl.replace("%s", query)
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
                    currentSearchEngine = currentEngine,
                    availableSearchEngines = mergedSearchEngines,
                    onStartEditing = { isEditing = true },
                    onEndEditing = {
                        isEditing = false
                        urlText = currentUrl
                    },
                    tabCount = tabCount,
                    onShowTabs = onShowTabs,
                    showOverflowMenu = showOverflowMenu,
                    onShowOverflowMenu = { showOverflowMenu = true },
                    onDismissOverflowMenu = { showOverflowMenu = false },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onBack = onBack,
                    onForward = onForward,
                    onReload = onReload,
                    onAddBookmark = {
                        if (!isCurrentUrlBookmarked) {
                            onAddBookmark(currentUrl, currentPageTitle)
                        }
                    },
                    isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                    onShowBookmarks = onShowBookmarks,
                    onShowHistory = onShowHistory,
                    onShowDownloads = onShowDownloads,
                    onShowSettings = onShowSettings,
                    focusRequester = focusRequester
                )
            }
        }

        // Handle shortcut dialogs
        selectedShortcut?.let { shortcut ->
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
                    shortcutViewModel.updateShortcut(shortcutToEdit!!, newLabel = label, newUrl = url)
                    showEditDialog = false
                    shortcutToEdit = null
                }
            )
        }
    }

    // Handle download confirmation
    if (showDownloadConfirmationDialog && currentDownloadRequest != null) {
        DownloadConfirmationDialog(
            fileName = currentDownloadRequest.fileName,
            fileSize = currentDownloadRequest.contentLength.toString(),
            onDownloadClicked = {
                Log.d("BrowserContent", "Download Confirmation: Download button clicked")
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

    // Handle post-download completion dialog
    if (showDownloadCompletionDialog && currentDownloadId != null) {
        DownloadCompletionDialog(
            downloadId = currentDownloadId!!,
            fileName = currentDownloadRequest?.fileName ?: "unknown",
            viewModel = downloadViewModel,
            onOpenClicked = {
                showDownloadCompletionDialog = false
                currentDownloadId = null
            },
            onDismissClicked = {
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
