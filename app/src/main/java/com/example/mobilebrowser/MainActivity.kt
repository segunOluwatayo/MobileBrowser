package com.example.mobilebrowser

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilebrowser.browser.GeckoDownloadDelegate
import com.example.mobilebrowser.browser.GeckoDownloadDelegate.DownloadRequest
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.screens.BookmarkEditScreen
import com.example.mobilebrowser.ui.screens.BookmarkScreen
import com.example.mobilebrowser.ui.screens.DownloadScreen
import com.example.mobilebrowser.ui.screens.HistoryScreen
import com.example.mobilebrowser.ui.screens.SearchEngineSelectionScreen
import com.example.mobilebrowser.ui.screens.SettingsScreen
import com.example.mobilebrowser.ui.screens.TabManagementSelectionScreen
import com.example.mobilebrowser.ui.screens.TabScreen
import com.example.mobilebrowser.ui.screens.ThemeSelectionScreen
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.ui.viewmodels.HistoryViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: GeckoSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = GeckoSessionManager(this)

        setContent {
            MobileBrowserTheme {
                // Define UI state variables.
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var currentPageTitle by remember { mutableStateOf("New Tab") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }
                var currentSession by remember { mutableStateOf<GeckoSession?>(null) }
                var isNavigating by remember { mutableStateOf(false) }

                // Track the last recorded history entry to avoid duplicate entries.
                var lastRecordedUrl by remember { mutableStateOf("") }
                var lastRecordedTitle by remember { mutableStateOf("") }

                val navController = rememberNavController()
                val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
                val tabViewModel: TabViewModel = hiltViewModel()
                val historyViewModel: HistoryViewModel = hiltViewModel()
                val downloadViewModel: com.example.mobilebrowser.ui.viewmodels.DownloadViewModel = hiltViewModel()
                val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
                val activeTab by tabViewModel.activeTab.collectAsState()
                val scope = rememberCoroutineScope()

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
                                //  Log.d("MainActivity", "onTitleChange in LaunchedEffect: $newTitle") // Add logging here
                                if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                    currentPageTitle = newTitle
                                    tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                    recordHistory(currentUrl, newTitle)
                                }
                            },
                            onCanGoBack = { canGoBack = it },
                            onCanGoForward = { canGoForward = it },
                            downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                        )
                        // Update UI state with the tab's stored URL and title.
                        currentUrl = tab.url
                        currentPageTitle = tab.title
                    }
                }

                // Define the navigation graph.
                NavHost(navController = navController, startDestination = "browser") {
                    composable("browser") {
                        val tabCount by tabViewModel.tabCount.collectAsState()
                        currentSession?.let { session ->
                            BrowserContent(
                                geckoSession = session,
                                onNavigate = { url ->
                                    currentUrl = url
                                    bookmarkViewModel.updateCurrentUrl(url)
                                    tabViewModel.updateActiveTabContent(url, currentPageTitle)
                                    if (currentPageTitle.isNotBlank() && currentPageTitle != "Loading...") {
                                        recordHistory(url, currentPageTitle)
                                    }
                                },
                                onBack = { session.goBack() },
                                onForward = { session.goForward() },
                                onReload = { session.reload() },
                                onShowBookmarks = { navController.navigate("bookmarks") },
                                onShowHistory = { navController.navigate("history") },
                                onShowTabs = { navController.navigate("tabs") },
                                onShowSettings = { navController.navigate("settings") },
                                onShowDownloads = { navController.navigate("downloads") },
                                onAddBookmark = { url, title ->
                                    bookmarkViewModel.quickAddBookmark(url, title)
                                },
                                isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                                currentPageTitle = currentPageTitle,
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                currentUrl = currentUrl,
                                tabCount = tabCount,
                                onNewTab = {
                                    scope.launch {
                                        // Create a new tab and session.
                                        val newTabId = tabViewModel.createTab()
                                        val newSession = sessionManager.getOrCreateSession(
                                            tabId = newTabId,
                                            url = "https://www.mozilla.org",
                                            onUrlChange = { newUrl ->
                                                currentUrl = newUrl
                                                bookmarkViewModel.updateCurrentUrl(newUrl)
                                                tabViewModel.updateActiveTabContent(newUrl, currentPageTitle)
                                            },
                                            onTitleChange = { newTitle ->
                                                // Log.d("MainActivity", "onTitleChange in onNewTab: $newTitle") // Add logging here
                                                if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                    currentPageTitle = newTitle
                                                    tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                    recordHistory(currentUrl, newTitle)
                                                }
                                            },
                                            onCanGoBack = { canGoBack = it },
                                            onCanGoForward = { canGoForward = it },
                                            downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                                        )
                                        currentSession = newSession
                                        tabViewModel.switchToTab(newTabId)
                                        currentUrl = "https://www.mozilla.org"
                                        currentPageTitle = "New Tab"
                                    }
                                },
                                onCloseAllTabs = {
                                    sessionManager.removeAllSessions()
                                    tabViewModel.closeAllTabs()
                                },
                                onCanGoBackChange = { canGoBack = it },
                                onCanGoForwardChange = { canGoForward = it },
                                showDownloadConfirmationDialog = showDownloadConfirmationDialog,
                                currentDownloadRequest = currentDownloadRequest,
                                onDismissDownloadConfirmationDialog = { showDownloadConfirmationDialog = false }
                            )
                        }
                    }
                    composable("bookmarks") {
                        BookmarkScreen(
                            onNavigateToEdit = { bookmarkId ->
                                navController.navigate("bookmark/edit/$bookmarkId")
                            },
                            onNavigateBack = { navController.popBackStack() },
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
                                                        //Log.d("MainActivity", "onTitleChange in bookmarks: $newTitle")
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it },
                                                    downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                                                )
                                            }
                                            currentUrl = url
                                            currentSession?.loadUri(url)
                                            navController.popBackStack()
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
                    composable("bookmark/edit/{bookmarkId}") { backStackEntry ->
                        val bookmarkId = backStackEntry.arguments?.getString("bookmarkId")?.toLongOrNull()
                        BookmarkEditScreen(
                            bookmarkId = bookmarkId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("downloads") {
                        DownloadScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            onNavigateBack = { navController.popBackStack() },
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
                                                        //   Log.d("MainActivity", "onTitleChange in history: $newTitle")
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it },
                                                    downloadDelegate = geckoDownloadDelegate  // Pass the delegate
                                                )
                                            }
                                            currentUrl = url
                                            currentSession?.loadUri(url)
                                            navController.popBackStack()
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
                    composable("tabs") {
                        TabScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onTabSelected = { tabId ->
                                scope.launch {
                                    tabViewModel.switchToTab(tabId)
                                    tabViewModel.getTabById(tabId)?.let { tab ->
                                        currentUrl = tab.url
                                        currentPageTitle = tab.title
                                        bookmarkViewModel.updateCurrentUrl(tab.url)
                                        currentSession?.loadUri(tab.url)
                                    }
                                }
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSelectSearchEngine = { navController.navigate("search_engine") },
                            onSelectTabManagement = { navController.navigate("tab_management") }
                        )
                    }
                    composable("search_engine") {
                        SearchEngineSelectionScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("tab_management") {
                        TabManagementSelectionScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("theme_selection") {
                        ThemeSelectionScreen(onNavigateBack = { navController.popBackStack() })
                    }

                }

            }
        }
    }
}