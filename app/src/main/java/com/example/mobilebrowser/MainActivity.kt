package com.example.mobilebrowser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.browser.GeckoDownloadDelegate
import com.example.mobilebrowser.browser.GeckoDownloadDelegate.DownloadRequest
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.screens.*
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.*
import com.example.mobilebrowser.worker.DynamicShortcutWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import android.view.View

// Enum to track which overlay screen is active
enum class OverlayScreen {
    None, Tabs, Settings, Bookmarks, Downloads, History, SearchEngine,
    TabManagement, ThemeSelection, HomepageSelection, BookmarkEdit
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: GeckoSessionManager
    private var geckoViewReference: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = GeckoSessionManager(this)
        // Schedule the dynamic shortcut worker
        DynamicShortcutWorker.schedule(this)
        setContent {
            // Obtain the SettingsViewModel via Hilt.
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            // Observe the current theme mode from DataStore.
            val themeMode by settingsViewModel.themeMode.collectAsState()
            // Get the system default dark theme value.
            val systemDarkTheme = isSystemInDarkTheme()
            // Determine darkTheme boolean based on user selection.
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDarkTheme // "SYSTEM" mode follows the system setting.
            }
            MobileBrowserTheme(darkTheme = darkTheme) {
                BrowserApp()
            }
        }
    }

    @Composable
    fun BrowserApp() {
        // Define UI state variables.
        var currentUrl by remember { mutableStateOf("") }
        var currentPageTitle by remember { mutableStateOf("New Tab") }
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var currentSession by remember { mutableStateOf<GeckoSession?>(null) }
        var isNavigating by remember { mutableStateOf(false) }

        // Track the last recorded history entry to avoid duplicate entries.
        var lastRecordedUrl by remember { mutableStateOf("") }
        var lastRecordedTitle by remember { mutableStateOf("") }

        // New state to track which screen is visible as an overlay
        var currentOverlay by remember { mutableStateOf(OverlayScreen.None) }
        // State to track bookmark edit ID
        var bookmarkEditId by remember { mutableStateOf<Long?>(null) }

        val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
        val tabViewModel: TabViewModel = hiltViewModel()
        val tabsState = tabViewModel.tabs.collectAsState()
        val displayTabCount by remember {
            derivedStateOf { tabsState.value.count { it.url.isNotBlank() } }
        }
        val historyViewModel: HistoryViewModel = hiltViewModel()
        val shortcutViewModel: ShortcutViewModel = hiltViewModel()
        val downloadViewModel: DownloadViewModel = hiltViewModel()
        val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
        val activeTab by tabViewModel.activeTab.collectAsState()
        val scope = rememberCoroutineScope()
        var isHomepageActive by remember { mutableStateOf(true) }

        // Helper: normalize URL by trimming and removing trailing "/"
        fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")

        // Helper: record history if the URL/title changed.
        fun recordHistory(url: String, title: String) {
            val normalizedUrl = normalizeUrl(url)
            if (title.isNotBlank() && title != "Loading..." &&
                (normalizedUrl != lastRecordedUrl || title != lastRecordedTitle)
            ) {
                lastRecordedUrl = normalizedUrl
                lastRecordedTitle = title
                historyViewModel.addHistoryEntry(normalizedUrl, title)

                // Also update shortcut visit count if this URL is a shortcut.
                scope.launch {
                    shortcutViewModel.recordVisit(normalizedUrl)
                }
            }
        }

        // Safely captures a thumbnail when GeckoView is ready
        fun safelyCaptureThumbnail(tabId: Long) {
            val view = geckoViewReference
            if (view == null) {
                Log.d("MainActivity", "Cannot capture thumbnail: GeckoView reference is null")
                return
            }

            scope.launch {
                try {
                    // Wait until we're confident the page has loaded and rendered
                    if (currentPageTitle.isBlank() || currentPageTitle == "Loading...") {
                        Log.d("MainActivity", "Delaying thumbnail capture until page is fully loaded")
                        delay(1000)  // Short delay
                        // If still loading, we'll skip this capture attempt
                        if (currentPageTitle.isBlank() || currentPageTitle == "Loading...") {
                            Log.d("MainActivity", "Skipping thumbnail capture - page still loading")
                            return@launch
                        }
                    }

                    // Add additional delay to ensure rendering is complete
                    delay(500)

                    Log.d("MainActivity", "Attempting to capture thumbnail for tab $tabId")
                    tabViewModel.updateTabThumbnail(tabId, view)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error while setting up thumbnail capture", e)
                }
            }
        }

        // State for the download confirmation dialog.
        var showDownloadConfirmationDialog by remember { mutableStateOf(false) }
        var currentDownloadRequest by remember { mutableStateOf<DownloadRequest?>(null) }

        val showDownloadConfirmation: (DownloadRequest) -> Unit = { downloadRequest ->
            currentDownloadRequest = downloadRequest
            showDownloadConfirmationDialog = true
        }

        // Create a shared GeckoDownloadDelegate using remember.
        val geckoDownloadDelegate = remember {
            GeckoDownloadDelegate(
                context = this@MainActivity,
                downloadViewModel = downloadViewModel,
                scope = scope,
                showDownloadConfirmation = showDownloadConfirmation
            )
        }

        // Update session when the active tab changes.
        LaunchedEffect(activeTab) {
            Log.d("MainActivity", "LaunchedEffect: activeTab = $activeTab")
            activeTab?.let { tab ->
                delay(1000) // wait a bit for the view to be ready
                if (geckoViewReference == null) {
                    Log.d("MainActivity", "Waiting for GeckoView reference to be created")
                } else {
                    Log.d("MainActivity", "GeckoView reference already exists, will use for thumbnails")
                }
            }

            activeTab?.let { tab ->
                Log.d("MainActivity", "Active tab URL = ${tab.url}, title = ${tab.title}")
                currentSession = sessionManager.getOrCreateSession(
                    tabId = tab.id,
                    url = tab.url,
                    onUrlChange = { newUrl ->
                        currentUrl = newUrl
                        bookmarkViewModel.updateCurrentUrl(newUrl)
                        tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                    },
                    onTitleChange = { newTitle ->
                        Log.d("MainActivity", "onTitleChange triggered with newTitle: $newTitle")
                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                            currentPageTitle = newTitle
                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                            recordHistory(currentUrl, newTitle)

                            // Don't try to capture thumbnails immediately on title change
                            // Instead, schedule a safe capture after the title indicates content is loaded
                            if (newTitle != "New Tab" && newTitle != "about:blank") {
                                scope.launch {
                                    delay(1000)  // Let the page finish rendering
                                    safelyCaptureThumbnail(tab.id)
                                }
                            }
                        }
                    },
                    onCanGoBack = { canGoBack = it },
                    onCanGoForward = { canGoForward = it },
                    downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                )
                // Update UI state with the tab's stored URL and title.
                currentUrl = tab.url
                currentPageTitle = tab.title

                // Schedule a safe thumbnail capture after the tab is loaded
                scope.launch {
                    // Delay to allow WebView to load and render
                    delay(1500)
                    safelyCaptureThumbnail(tab.id)
                }
            }
        }

        // Main Box that contains our entire UI
        Box(modifier = Modifier.fillMaxSize()) {
            // Browser content is always present but potentially hidden
            currentSession?.let { session ->
                BrowserContent(
                    geckoSession = session,
                    onNavigate = { url ->
                        if (url.isNotBlank()) {
                            isHomepageActive = false
                            currentUrl = url
                            bookmarkViewModel.updateCurrentUrl(url)
                            tabViewModel.updateActiveTabContent(url, currentPageTitle)
                            if (currentPageTitle.isNotBlank() && currentPageTitle != "Loading...") {
                                recordHistory(url, currentPageTitle)
                            }
                        } else {
                            // If the URL is blank (or normalized to blank), show the homepage overlay.
                            isHomepageActive = true
                            currentUrl = url
                        }
                    },
                    onBack = { session.goBack() },
                    isHomepageActive = isHomepageActive,
                    onForward = { session.goForward() },
                    onReload = { session.reload() },
                    onShowBookmarks = { currentOverlay = OverlayScreen.Bookmarks },
                    onShowHistory = { currentOverlay = OverlayScreen.History },
                    onShowTabs = { currentOverlay = OverlayScreen.Tabs },
                    onShowSettings = { currentOverlay = OverlayScreen.Settings },
                    onShowDownloads = { currentOverlay = OverlayScreen.Downloads },
                    activeTab = activeTab,
                    tabViewModel = tabViewModel,
                    onAddBookmark = { url, title ->
                        bookmarkViewModel.quickAddBookmark(url, title)
                    },
                    isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                    currentPageTitle = currentPageTitle,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    currentUrl = currentUrl,
//                    tabCount = tabViewModel.tabCount.collectAsState().value,
                    tabCount = displayTabCount,
                    onGeckoViewCreated = { view ->
                        Log.d("MainActivity", "Received GeckoView reference: ${view::class.java}")
                        geckoViewReference = view

                        // Use the existing scope from MainActivity
                        scope.launch {
                            activeTab?.let { tab ->
                                // Add delay to ensure the GeckoView is fully initialized
                                delay(1000)
                                Log.d("MainActivity", "Updating thumbnail for tab ${tab.id} after getting GeckoView reference")
                                safelyCaptureThumbnail(tab.id)
                            }
                        }
                    },
                    onNewTab = {
                        scope.launch {
                            // Create a new tab and session.
                            val newTabId = tabViewModel.createTab(url = "", title = "New Tab")
                            val newSession = sessionManager.getOrCreateSession(
                                tabId = newTabId,
                                url = "",
                                onUrlChange = { newUrl ->
                                    currentUrl = newUrl
                                    bookmarkViewModel.updateCurrentUrl(newUrl)
                                    tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                },
                                onTitleChange = { newTitle ->
                                    if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                        currentPageTitle = newTitle
                                        tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                        recordHistory(currentUrl, newTitle)

                                        // Schedule a safe thumbnail capture when title changes
                                        if (newTitle != "New Tab" && newTitle != "about:blank") {
                                            scope.launch {
                                                delay(1000)  // Let the page finish rendering
                                                safelyCaptureThumbnail(newTabId)
                                            }
                                        }
                                    }
                                },
                                onCanGoBack = { canGoBack = it },
                                onCanGoForward = { canGoForward = it },
                                downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                            )
                            currentSession = newSession
                            tabViewModel.switchToTab(newTabId)
                            currentUrl = ""
                            currentPageTitle = "New Tab"
                            isHomepageActive = true
                        }
                    },
                    onCloseAllTabs = {
                        sessionManager.removeAllSessions()
                        tabViewModel.closeAllTabs()
                        isHomepageActive = true
                        currentUrl = ""
                        currentPageTitle = "New Tab"
                    },
                    onCanGoBackChange = { canGoBack = it },
                    onCanGoForwardChange = { canGoForward = it },
                    showDownloadConfirmationDialog = showDownloadConfirmationDialog,
                    currentDownloadRequest = currentDownloadRequest,
                    onDismissDownloadConfirmationDialog = { showDownloadConfirmationDialog = false },
                    isOverlayActive = currentOverlay != OverlayScreen.None
                )
            }

            // Animated overlay container
            AnimatedVisibility(
                visible = currentOverlay != OverlayScreen.None,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
            ) {
                // Box with surface background to cover browser content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Show different overlays based on state
                    when (currentOverlay) {
                        OverlayScreen.Tabs -> {
                            TabScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onTabSelected = { tabId ->
                                    scope.launch {
                                        tabViewModel.switchToTab(tabId)
                                        tabViewModel.getTabById(tabId)?.let { tab ->
                                            currentUrl = tab.url
                                            currentPageTitle = tab.title
                                            bookmarkViewModel.updateCurrentUrl(tab.url)

                                            // Check if this is a new tab that should show the homepage
                                            if (tab.url.isBlank() || tab.url == "about:blank") {
                                                isHomepageActive = true
                                            } else {
                                                isHomepageActive = false
                                                currentSession?.loadUri(tab.url)
                                            }
                                        }
                                        currentOverlay = OverlayScreen.None
                                    }
                                }
                            )
                        }
                        OverlayScreen.Settings -> {
                            SettingsScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onSelectSearchEngine = { currentOverlay = OverlayScreen.SearchEngine },
                                onSelectTabManagement = { currentOverlay = OverlayScreen.TabManagement },
                                onSelectTheme = { currentOverlay = OverlayScreen.ThemeSelection },
                                onNavigateToHomepageSelection = { currentOverlay = OverlayScreen.HomepageSelection }
                            )
                        }
                        OverlayScreen.SearchEngine -> {
                            SearchEngineSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.TabManagement -> {
                            TabManagementSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.ThemeSelection -> {
                            ThemeSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.HomepageSelection -> {
                            HomepageSelectionScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.Settings }
                            )
                        }
                        OverlayScreen.Bookmarks -> {
                            BookmarkScreen(
                                onNavigateToEdit = { id ->
                                    bookmarkEditId = id
                                    currentOverlay = OverlayScreen.BookmarkEdit
                                },
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onNavigateToUrl = { url ->
                                    if (!isNavigating) {
                                        scope.launch {
                                            try {
                                                isNavigating = true
                                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                                activeTab?.let { tab ->
                                                    currentSession = sessionManager.getOrCreateSession(
                                                        tabId = tab.id,
                                                        url = tab.url,
                                                        onUrlChange = { newUrl ->
                                                            currentUrl = newUrl
                                                            bookmarkViewModel.updateCurrentUrl(newUrl)
                                                            tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                        },
                                                        onTitleChange = { newTitle ->
                                                            if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                                currentPageTitle = newTitle
                                                                tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                                recordHistory(currentUrl, newTitle)

                                                                // Schedule safe thumbnail capture
                                                                if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                                    scope.launch {
                                                                        delay(1000)
                                                                        safelyCaptureThumbnail(tab.id)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onCanGoBack = { canGoBack = it },
                                                        onCanGoForward = { canGoForward = it },
                                                        downloadDelegate = geckoDownloadDelegate
                                                    )
                                                }
                                                currentUrl = url
                                                currentSession?.loadUri(url)
                                                currentOverlay = OverlayScreen.None
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Error navigating to bookmark: ${e.message}")
                                            } finally {
                                                isNavigating = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        OverlayScreen.BookmarkEdit -> {
                            BookmarkEditScreen(
                                bookmarkId = bookmarkEditId,
                                onNavigateBack = {
                                    bookmarkEditId = null
                                    currentOverlay = OverlayScreen.Bookmarks
                                }
                            )
                        }
                        OverlayScreen.Downloads -> {
                            DownloadScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None }
                            )
                        }
                        OverlayScreen.History -> {
                            HistoryScreen(
                                onNavigateBack = { currentOverlay = OverlayScreen.None },
                                onNavigateToUrl = { url ->
                                    if (!isNavigating) {
                                        scope.launch {
                                            try {
                                                isNavigating = true
                                                tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                                activeTab?.let { tab ->
                                                    currentSession = sessionManager.getOrCreateSession(
                                                        tabId = tab.id,
                                                        url = url,
                                                        onUrlChange = { newUrl ->
                                                            currentUrl = newUrl
                                                            bookmarkViewModel.updateCurrentUrl(newUrl)
                                                            tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                                        },
                                                        onTitleChange = { newTitle ->
                                                            if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                                currentPageTitle = newTitle
                                                                tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                                recordHistory(currentUrl, newTitle)

                                                                // Schedule safe thumbnail capture
                                                                if (newTitle != "New Tab" && newTitle != "about:blank") {
                                                                    scope.launch {
                                                                        delay(1000)
                                                                        safelyCaptureThumbnail(tab.id)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onCanGoBack = { canGoBack = it },
                                                        onCanGoForward = { canGoForward = it },
                                                        downloadDelegate = geckoDownloadDelegate
                                                    )
                                                }
                                                currentUrl = url
                                                currentSession?.loadUri(url)
                                                currentOverlay = OverlayScreen.None
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Error navigating from history: ${e.message}")
                                            } finally {
                                                isNavigating = false
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        else -> { /* Not showing any overlay */ }
                    }
                }
            }
        }
    }
}