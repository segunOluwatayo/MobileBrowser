package com.example.mobilebrowser

import android.app.DownloadManager
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobilebrowser.browser.GeckoSessionManager
import com.example.mobilebrowser.data.repository.DownloadRepository
import com.example.mobilebrowser.receiver.DownloadCompleteReceiver
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.composables.DownloadCompletionDialog
import com.example.mobilebrowser.ui.composables.DownloadConfirmationDialog
import com.example.mobilebrowser.ui.screens.*
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import com.example.mobilebrowser.ui.viewmodels.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    private val downloadViewModel: DownloadViewModel by viewModels()

    @Inject
    lateinit var sessionManager: GeckoSessionManager

    @Inject
    lateinit var downloadRepository: DownloadRepository
    private lateinit var downloadCompleteReceiver: DownloadCompleteReceiver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileBrowserTheme {
                // UI state for URL, title, navigation flags, etc.
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var currentPageTitle by remember { mutableStateOf("New Tab") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }
                var currentSession by remember { mutableStateOf<GeckoSession?>(null) }
                var isNavigating by remember { mutableStateOf(false) }

                // Track the last recorded history entry to avoid duplicates
                var lastRecordedUrl by remember { mutableStateOf("") }
                var lastRecordedTitle by remember { mutableStateOf("") }

                val navController = rememberNavController()

                // Other ViewModels that can still use hiltViewModel()
                val bookmarkViewModel: BookmarkViewModel = hiltViewModel()
                val tabViewModel: TabViewModel = hiltViewModel()
                val historyViewModel: HistoryViewModel = hiltViewModel()
                val mainViewModel: MainViewModel = hiltViewModel()

                // Bookmark & tab state
                val isCurrentUrlBookmarked by bookmarkViewModel.isCurrentUrlBookmarked.collectAsState()
                val activeTab by tabViewModel.activeTab.collectAsState()
                val scope = rememberCoroutineScope()

                // Download logic
                val pendingDownload by mainViewModel.pendingDownload.collectAsState()

                // 3) Observe DownloadViewModel from the Activity scope
                val downloadState by downloadViewModel.downloadState.collectAsState()

                // Helper to avoid repeated history entries
                fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")
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

                // Observe changes in the active tab
                LaunchedEffect(activeTab) {
                    // Listen for download requests in the session manager
                    sessionManager.onDownloadRequested = { filename, mimeType, sourceUrl, contentDisposition ->
                        mainViewModel.setPendingDownload(filename, mimeType, sourceUrl, contentDisposition)
                    }
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
                                }
                            },
                            onCanGoBack = { canGoBack = it },
                            onCanGoForward = { canGoForward = it }
                        )
                        // Update UI from the active tab
                        currentUrl = tab.url
                        currentPageTitle = tab.title
                    }
                }

                // Show a confirmation dialog if a new download is requested
                pendingDownload?.let { download ->
                    DownloadConfirmationDialog(
                        download = download,
                        onConfirm = { mainViewModel.confirmDownload(download) },
                        onDismiss = { mainViewModel.clearPendingDownload() }
                    )
                }

                // 4) Show the completion dialog if the DownloadViewModel state is Completed
                when (downloadState) {
                    is DownloadState.Completed -> {
                        DownloadCompletionDialog(
                            state = downloadState as DownloadState.Completed,
                            onDismiss = { downloadViewModel.setIdle() }
                        )
                    }
                    else -> { /* No other states need a dialog here */ }
                }

                // The NavHost for your various screens
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
                                onAddBookmark = { url, title ->
                                    bookmarkViewModel.quickAddBookmark(url, title)
                                },
                                isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                                currentPageTitle = currentPageTitle,
                                canGoBack = canGoBack,
                                canGoForward = canGoForward,
                                onShowDownloads = { navController.navigate("downloads") },
                                currentUrl = currentUrl,
                                tabCount = tabCount,
                                onNewTab = {
                                    scope.launch {
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
                                                if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                    currentPageTitle = newTitle
                                                    tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                    recordHistory(currentUrl, newTitle)
                                                }
                                            },
                                            onCanGoBack = { canGoBack = it },
                                            onCanGoForward = { canGoForward = it }
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
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it }
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
                                                        if (newTitle.isNotBlank() && newTitle != "Loading...") {
                                                            currentPageTitle = newTitle
                                                            tabViewModel.updateActiveTabContent(currentUrl, newTitle)
                                                            recordHistory(currentUrl, newTitle)
                                                        }
                                                    },
                                                    onCanGoBack = { canGoBack = it },
                                                    onCanGoForward = { canGoForward = it }
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

                    composable("downloads") {
                        DownloadScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = hiltViewModel()
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
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(downloadCompleteReceiver)
        super.onDestroy()
    }
}
